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
package org.xwiki.contrib.replication.internal.message.log;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationMessage;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * @version $Id$
 */
@Component(roles = ReplicationMessageLogStore.class)
@Singleton
public class ReplicationMessageLogStore
{
    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private XWikiStoreInterface hibernateStore;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * @param messageId the identifier of the message
     * @return true if the message was found
     * @throws XWikiException when failing to search for the message
     */
    public boolean exist(String messageId) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        return store.executeRead(xcontext, session -> exist(messageId, session));
    }

    private boolean exist(String messageId, Session session)
    {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<HibernateReplicationMessage> from = query.from(HibernateReplicationMessage.class);
        query.select(builder.count(from)).where(builder.equal(from.get("id"), messageId));

        return session.createQuery(query).uniqueResult() > 0;
    }

    /**
     * @param message the message to save
     * @throws XWikiException when failing to save the message
     */
    public void save(ReplicationMessage message) throws XWikiException
    {
        executeWrite(session -> saveHibernateReplicationMessage(message, session));
    }

    /**
     * @param messageId the identifier of the message to delete
     * @throws XWikiException when failing to save the message
     */
    public void delete(String messageId) throws XWikiException
    {
        executeWrite(session -> deleteHibernateReplicationMessage(messageId, session));
    }

    private Object saveHibernateReplicationMessage(ReplicationMessage message, Session session)
    {
        session.save(new HibernateReplicationMessage(message));

        return null;
    }

    private Object deleteHibernateReplicationMessage(String messageId, Session session)
    {
        session.delete(new HibernateReplicationMessage(messageId));

        return null;
    }

    private void executeWrite(HibernateCallback<Object> callback) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        store.executeWrite(xcontext, callback);
    }
}
