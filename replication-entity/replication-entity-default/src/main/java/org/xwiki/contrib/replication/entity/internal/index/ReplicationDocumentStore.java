/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.replication.entity.internal.index;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStoreCache.ReplicationDocumentStoreCacheEntry;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.util.Util;

/**
 * @version $Id$
 */
@Component(roles = ReplicationDocumentStore.class)
@Singleton
public class ReplicationDocumentStore
{
    private static final String PROP_DOCID = "docId";

    private static final String PROP_OWNER = "owner";

    private static final String PROP_CONFLICT = "conflict";

    private static final String PROP_READONLY = "readonly";

    private static final String PROP_NATIVE_DOCID = "XWD_ID";

    private static final String PROP_NATIVE_OWNER = "XWRD_OWNER";

    private static final String PROP_NATIVE_CONFLICT = "XWRD_CONFLICT";

    private static final String PROP_NATIVE_READONLY = "XWRD_READONLY";

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private XWikiStoreInterface hibernateStore;

    @Inject
    @Named("uid")
    private EntityReferenceSerializer<String> idSerializer;

    @Inject
    private ReplicationDocumentStoreCache cache;

    @Inject
    private ObservationManager observation;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * @param document the reference of the document
     * @throws ReplicationException when failing to delete the document entry
     */
    public void remove(DocumentReference document) throws ReplicationException
    {
        long id = toDocumentId(document);

        executeWrite(session -> deleteHibernateReplicationDocument(id, session), document.getWikiReference());

        this.observation.notify(new DocumentIndexDeletedEvent(document), null);
    }

    /**
     * @param document the identifier of the document
     * @param owner the owner instance of the document
     * @throws ReplicationException when failing to update the owner
     */
    public void setOwner(DocumentReference document, String owner) throws ReplicationException
    {
        long id = toDocumentId(document);

        executeWrite(session -> saveHibernateReplicationDocument(id, owner, session), document.getWikiReference());

        this.observation.notify(new DocumentOwnerUpdatedEvent(document, owner), null);
    }

    /**
     * @param document the identifier of the document
     * @param conflict true if the document has a replication conflict
     * @throws ReplicationException when failing to update the conflict marker
     */
    public void setConflict(DocumentReference document, boolean conflict) throws ReplicationException
    {
        long id = toDocumentId(document);

        executeWrite(session -> update(id, PROP_CONFLICT, conflict, session), document.getWikiReference());

        this.observation.notify(new DocumentConflictUpdatedEvent(document, conflict), null);
    }

    /**
     * @param document the identifier of the document
     * @param readonly true if the document should be made readonly
     * @throws ReplicationException when failing to update the conflict marker
     * @since 1.12.0
     */
    public void setReadonly(DocumentReference document, boolean readonly) throws ReplicationException
    {
        long id = toDocumentId(document);

        executeWrite(session -> update(id, PROP_READONLY, readonly, session), document.getWikiReference());

        this.observation.notify(new DocumentReadonlyUpdatedEvent(document, readonly), null);
    }

    private Void saveHibernateReplicationDocument(long docId, String owner, Session session) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (get(docId, PROP_DOCID, Long.class, xcontext) != null) {
            update(docId, PROP_OWNER, owner, session);
        } else {
            insert(docId, owner, false, false, session);
        }

        return null;
    }

    private Void update(long docId, String propertyName, Object propertyValue, Session session)
    {
        // Not using the Hibernate session entity API to avoid classloader problems
        Query<?> query = session.createQuery(
            "UPDATE HibernateReplicationDocument SET " + propertyName + " = :propertyUpdate WHERE docId = :docId");
        query.setParameter(PROP_DOCID, docId);
        query.setParameter("propertyUpdate", propertyValue);

        query.executeUpdate();

        return null;
    }

    private <T> T get(long docId, String propertyName, Class<T> resultType, Session session)
    {
        // Not using the Hibernate session entity API to avoid classloader problems
        Query<T> query = session.createQuery(
            "SELECT " + propertyName + " FROM HibernateReplicationDocument where docId = :docId", resultType);
        query.setParameter(PROP_DOCID, docId);

        return query.uniqueResult();
    }

    private Void insert(long docId, String owner, boolean conflict, boolean readonly, Session session)
    {
        // Not using the Hibernate session entity API to avoid classloader problems
        // Using native query since it's impossible to insert values with HQL
        NativeQuery<?> query = session.createNativeQuery(
            String.format("INSERT INTO replication_document (%s, %s, %s, %s) VALUES (:%s, :%s, :%s, :%s)",
                PROP_NATIVE_DOCID, PROP_NATIVE_OWNER, PROP_NATIVE_CONFLICT, PROP_NATIVE_READONLY, PROP_DOCID,
                PROP_OWNER, PROP_CONFLICT, PROP_READONLY));
        query.setParameter(PROP_DOCID, docId);
        query.setParameter(PROP_OWNER, owner);
        query.setParameter(PROP_CONFLICT, conflict);
        query.setParameter(PROP_READONLY, readonly);

        query.executeUpdate();

        return null;
    }

    private Void deleteHibernateReplicationDocument(long docId, Session session)
    {
        // Not using the Hibernate session entity API to avoid classloader problems
        Query<?> query = session.createQuery("DELETE HibernateReplicationDocument WHERE docId=:docId");
        query.setParameter(PROP_DOCID, docId);
        query.executeUpdate();

        return null;
    }

    /**
     * @param document the reference of the document
     * @return the id of the document
     */
    private long toDocumentId(DocumentReference document)
    {
        return Util.getHash(document.getType().name() + ':' + this.idSerializer.serialize(document.withoutLocale()));
    }

    /**
     * @param document the reference of the document
     * @return the cache key used to store replication metadata about the document
     */
    public String getCacheKey(DocumentReference document)
    {
        return String.valueOf(toDocumentId(document));
    }

    /**
     * @param documentReference the reference of the document
     * @return the owner instance of the document
     * @throws ReplicationException when failing to access the owner
     */
    public String getOwner(DocumentReference documentReference) throws ReplicationException
    {
        long docId = toDocumentId(documentReference);

        ReplicationDocumentStoreCacheEntry cacheEntry = this.cache.getEntry(String.valueOf(docId), true);

        if (cacheEntry.getOwner() != null) {
            return cacheEntry.getOwner();
        }

        String owner = get(documentReference, docId, PROP_OWNER, String.class);

        cacheEntry.setOwner(owner);

        return owner;
    }

    /**
     * @param documents the references of the documents
     * @return the owner instances of the provided documents
     * @throws ReplicationException when failing to get the owners
     */
    public List<String> getOwners(List<DocumentReference> documents) throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        try {
            return store.executeRead(xcontext, s -> s
                .createQuery("SELECT owner FROM HibernateReplicationDocument WHERE docId IN :documents", String.class)
                .setParameter("documents", documents.stream().map(this::toDocumentId).collect(Collectors.toList()))
                .list());
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to get document owners", e);
        }
    }

    /**
     * @param documentReference the reference of the document
     * @return true if the document has a replication conflict
     * @throws ReplicationException when failing to access the owner
     */
    public boolean getConflict(DocumentReference documentReference) throws ReplicationException
    {
        long docId = toDocumentId(documentReference);

        ReplicationDocumentStoreCacheEntry cacheEntry = this.cache.getEntry(String.valueOf(docId), true);

        if (cacheEntry.getConflict() != null) {
            return cacheEntry.getConflict();
        }

        boolean conflict = Boolean.TRUE.equals(get(documentReference, docId, PROP_CONFLICT, Boolean.class));

        cacheEntry.setConflict(conflict);

        return conflict;
    }

    /**
     * @param documentReference the reference of the document
     * @return true if the document is readonly
     * @throws ReplicationException when failing to access the owner
     */
    public boolean isReadonly(DocumentReference documentReference) throws ReplicationException
    {
        long docId = toDocumentId(documentReference);

        ReplicationDocumentStoreCacheEntry cacheEntry = this.cache.getEntry(String.valueOf(docId), true);

        if (cacheEntry.getReadonly() != null) {
            return cacheEntry.getReadonly();
        }

        boolean readonly = Boolean.TRUE.equals(get(documentReference, docId, PROP_READONLY, Boolean.class));

        cacheEntry.setReadonly(readonly);

        return readonly;
    }

    private void executeWrite(HibernateCallback<Void> callback, WikiReference wiki) throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        WikiReference currentWiki = xcontext.getWikiReference();
        try {
            xcontext.setWikiReference(wiki);

            store.executeWrite(xcontext, callback);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to execute the write", e);
        } finally {
            xcontext.setWikiReference(currentWiki);
        }
    }

    private <T> T get(long docId, String property, Class<T> resultType, XWikiContext xcontext) throws XWikiException
    {
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        // Not using the Hibernate session entity API to avoid classloader problems
        return store.executeRead(xcontext, s -> get(docId, property, resultType, s));
    }

    private <T> T get(DocumentReference reference, long docId, String property, Class<T> resultType)
        throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        WikiReference currentWiki = xcontext.getWikiReference();
        try {
            xcontext.setWikiReference(reference.getWikiReference());

            return get(docId, property, resultType, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to execute the select", e);
        } finally {
            xcontext.setWikiReference(currentWiki);
        }
    }
}
