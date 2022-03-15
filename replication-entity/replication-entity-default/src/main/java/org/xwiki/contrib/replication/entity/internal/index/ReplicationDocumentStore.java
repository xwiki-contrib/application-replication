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
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private XWikiStoreInterface hibernateStore;

    @Inject
    @Named("uid")
    private EntityReferenceSerializer<String> idSerializer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * @param entity the identifier of the document
     * @param owner the owner instance of the document
     * @throws ReplicationException when failing to create the document entry
     */
    public void create(EntityReference entity, String owner) throws ReplicationException
    {
        executeWrite(session -> saveHibernateReplicationDocument(toEntityId(entity), owner, session));
    }

    /**
     * @param entity the identifier of the document
     * @throws ReplicationException when failing to delete the document entry
     */
    public void deleteDocument(EntityReference entity) throws ReplicationException
    {
        executeWrite(session -> deleteHibernateReplicationDocument(toEntityId(entity), session));
    }

    /**
     * @param entity the identifier of the document
     * @param owner the owner instance of the document
     * @throws ReplicationException when failing to update the owner
     */
    public void setOwner(EntityReference entity, String owner) throws ReplicationException
    {
        executeWrite(session -> saveHibernateReplicationDocument(toEntityId(entity), owner, session));
    }

    private Void saveHibernateReplicationDocument(long docId, String owner, Session session)
    {
        session.save(new HibernateReplicationDocument(docId, owner));

        return null;
    }

    private Void deleteHibernateReplicationDocument(long docId, Session session)
    {
        session.delete(new HibernateReplicationDocument(docId));

        return null;
    }

    /**
     * @param reference the reference of the entity
     * @return the id of the entity
     */
    private long toEntityId(EntityReference reference)
    {
        return Util.getHash(reference.getType().name() + ':' + this.idSerializer.serialize(reference));
    }

    /**
     * @param documentReference the reference of the document
     * @return the owner instance of the document
     * @throws ReplicationException when failing to access the owner
     */
    public String getOwner(DocumentReference documentReference) throws ReplicationException
    {
        return get(documentReference, "owner");
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
                + " from HibernateReplicationDocument where docId = '" + toEntityId(reference) + "'").uniqueResult());
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to execute the read", e);
        }
    }
}
