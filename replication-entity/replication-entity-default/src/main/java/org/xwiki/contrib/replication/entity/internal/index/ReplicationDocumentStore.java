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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

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
    private static final String PROP_OWNER = "owner";

    private static final String PROP_CONFLICT = "conflict";

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private XWikiStoreInterface hibernateStore;

    @Inject
    @Named("uid")
    private EntityReferenceSerializer<String> idSerializer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * @param document the reference of the document
     * @param owner the owner instance of the document
     * @throws ReplicationException when failing to create the document entry
     */
    public void create(DocumentReference document, String owner) throws ReplicationException
    {
        executeWrite(session -> saveHibernateReplicationDocument(toDocumentId(document), owner, session));
    }

    /**
     * @param document the reference of the document
     * @throws ReplicationException when failing to delete the document entry
     */
    public void deleteDocument(DocumentReference document) throws ReplicationException
    {
        executeWrite(session -> deleteHibernateReplicationDocument(toDocumentId(document), session));
    }

    /**
     * @param document the identifier of the document
     * @param owner the owner instance of the document
     * @throws ReplicationException when failing to update the owner
     */
    public void setOwner(DocumentReference document, String owner) throws ReplicationException
    {
        executeWrite(session -> saveHibernateReplicationDocument(toDocumentId(document), owner, session));
    }

    /**
     * @param document the identifier of the document
     * @param conflict true if the document has a replication conflict
     * @throws ReplicationException when failing to update the conflict marker
     */
    public void setConflict(DocumentReference document, boolean conflict) throws ReplicationException
    {
        executeWrite(session -> updateConflict(toDocumentId(document), conflict, session));
    }

    private Void saveHibernateReplicationDocument(long docId, String owner, Session session)
    {
        session.saveOrUpdate(new HibernateReplicationDocument(docId, owner));

        return null;
    }

    private Void updateConflict(long docId, boolean conflict, Session session)
    {
        Query query =
            session.createQuery("UPDATE HibernateReplicationDocument SET conflict=:conflict WHERE docId=:docId");
        query.setParameter(PROP_CONFLICT, conflict);
        query.setParameter("docId", docId);
        query.executeUpdate();

        return null;
    }

    private Void deleteHibernateReplicationDocument(long docId, Session session)
    {
        session.delete(new HibernateReplicationDocument(docId));

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
     * @param documentReference the reference of the document
     * @return the owner instance of the document
     * @throws ReplicationException when failing to access the owner
     */
    public String getOwner(DocumentReference documentReference) throws ReplicationException
    {
        return get(documentReference, PROP_OWNER);
    }

    /**
     * @param documentReference the reference of the document
     * @return true if the document has a replication conflict
     * @throws ReplicationException when failing to access the owner
     */
    public boolean getConflict(DocumentReference documentReference) throws ReplicationException
    {
        Boolean conflict = get(documentReference, PROP_CONFLICT);

        return Boolean.TRUE.equals(conflict);
    }

    private void executeWrite(HibernateCallback<Void> callback) throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        try {
            store.executeWrite(xcontext, callback);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to execute the write", e);
        }
    }

    private <T> T get(DocumentReference reference, String property) throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        try {
            return store.executeRead(xcontext, s -> (T) s.createQuery("select " + property
                + " from HibernateReplicationDocument where docId = '" + toDocumentId(reference) + "'").uniqueResult());
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to execute the read", e);
        }
    }
}
