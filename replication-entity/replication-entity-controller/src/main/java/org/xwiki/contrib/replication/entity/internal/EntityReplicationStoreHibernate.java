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
package org.xwiki.contrib.replication.entity.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Store entity replication configuration in the database.
 * 
 * @version $Id$
 */
@Component(roles = EntityReplicationStoreHibernate.class)
@Singleton
public class EntityReplicationStoreHibernate
{
    private static final String PROP_ENTITY = "entity";

    private static final String PROP_INSTANCE = "instance";

    private static final String PROP_LEVEL = "level";

    private static final String PROP_DIRECTION = "direction";

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private XWikiStoreInterface hibernateStore;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private WikiDescriptorManager wikis;

    @Inject
    private Logger logger;

    /**
     * @param wiki the wiki where to write
     * @param entityId the id of the entity
     * @param instances the instance to send the entity to
     * @throws XWikiException when failing to store entity replication configuration
     */
    public void storeHibernateEntityReplication(String wiki, long entityId,
        Collection<DocumentReplicationControllerInstance> instances) throws XWikiException
    {
        executeWrite(wiki, session -> storeHibernateEntityReplication(entityId, instances, session));
    }

    private Object storeHibernateEntityReplication(long entityId,
        Collection<DocumentReplicationControllerInstance> instances, Session session)
    {
        // Delete existing instances
        deleteEntity(entityId, session);

        // Add new instances
        if (instances != null) {
            if (instances.isEmpty()) {
                insert(new HibernateEntityReplicationInstance(entityId,
                    new DocumentReplicationControllerInstance(null, null, null)), session);
            } else {
                for (DocumentReplicationControllerInstance instance : instances) {
                    insert(new HibernateEntityReplicationInstance(entityId, instance), session);
                }
            }
        }

        return null;
    }

    private Void insert(HibernateEntityReplicationInstance instance, Session session)
    {
        // Not using the Hibernate session entity API to avoid classloader problems
        // Using native query since it's impossible to insert values with HQL
        NativeQuery<?> query = session.createNativeQuery(
            "INSERT INTO replication_entity_instances (XWR_ENTITY, XWR_INSTANCE, XWR_LEVEL, XWR_DIRECTION)"
                + " VALUES (:entity, :instance, :level, :direction)");
        query.setParameter(PROP_ENTITY, instance.getEntity());
        query.setParameter(PROP_INSTANCE, instance.getInstance());
        query.setParameter(PROP_LEVEL, instance.getLevel() != null ? instance.getLevel().name() : null);
        query.setParameter(PROP_DIRECTION, instance.getDirection().name());

        query.executeUpdate();

        return null;
    }

    private Object deleteEntity(long entityId, Session session)
    {
        // Delete existing instances
        Query<?> query = session
            .createQuery("DELETE FROM HibernateEntityReplicationInstance AS instance where instance.entity = :entity");
        query.setParameter(PROP_ENTITY, entityId);
        query.executeUpdate();

        return null;
    }

    private Object deleteInstance(String uri, Session session)
    {
        // Delete existing instances
        Query<?> query = session.createQuery(
            "DELETE FROM HibernateEntityReplicationInstance AS instance where instance.instance = :instance");
        query.setParameter(PROP_INSTANCE, uri);
        query.executeUpdate();

        return null;
    }

    /**
     * @param wiki the wiki where to search
     * @param entityId the id of the entity
     * @return the instances directly configured at this entity level
     * @throws XWikiException when failing to get the instances
     */
    public Collection<DocumentReplicationControllerInstance> getHibernateEntityReplication(String wiki, long entityId)
        throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        String currentWiki = xcontext.getWikiId();
        try {
            xcontext.setWikiId(wiki);

            return getHibernateEntityReplication(entityId, xcontext);
        } finally {
            xcontext.setWikiId(currentWiki);
        }
    }

    private Collection<DocumentReplicationControllerInstance> getHibernateEntityReplication(long entityId,
        XWikiContext xcontext) throws XWikiException
    {
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        return store.executeRead(xcontext, session -> getHibernateEntityReplication(entityId, session));
    }

    private List<DocumentReplicationControllerInstance> getHibernateEntityReplication(long entityId, Session session)
        throws XWikiException
    {
        Query<Object[]> query = session.createQuery(
            "SELECT instance.entity, instance.instance, instance.level, instance.direction"
                + " FROM HibernateEntityReplicationInstance AS instance WHERE instance.entity = :entity",
            Object[].class);
        query.setParameter(PROP_ENTITY, entityId);

        List<HibernateEntityReplicationInstance> hibernateInstances = query.list().stream().map(i -> {
            // Avoid classloader reloading related issue by asking for the values instead of the a
            // HibernateEntityReplicationInstance instance directly
            long entity = ((Number) i[0]).longValue();
            String instance = (String) i[1];
            DocumentReplicationLevel level = EntityReplicationUtils.toDocumentReplicationLevel(i[2]);
            DocumentReplicationDirection direction = EntityReplicationUtils.toDocumentReplicationDirection(i[3]);

            return new HibernateEntityReplicationInstance(entity, instance, level, direction);
        }).collect(Collectors.toList());

        // There is no entry
        if (hibernateInstances.isEmpty()) {
            // This level inherit its parent configuration
            return null;
        }

        List<DocumentReplicationControllerInstance> instances = new ArrayList<>(hibernateInstances.size());
        for (HibernateEntityReplicationInstance hibernateInstance : hibernateInstances) {
            if (hibernateInstance.getInstance().isEmpty()) {
                // Replication use the same level for all instances
                instances.add(new DocumentReplicationControllerInstance(null, hibernateInstance.getLevel(),
                    hibernateInstance.getDirection()));
            } else {
                ReplicationInstance instance;
                try {
                    instance = this.instanceManager.getInstanceByURI(hibernateInstance.getInstance());
                } catch (ReplicationException e) {
                    throw new XWikiException("Failed to access instances", e);
                }

                if (instance != null && (instance.getStatus() == Status.REGISTERED || instance.getStatus() == null)) {
                    instances.add(new DocumentReplicationControllerInstance(instance, hibernateInstance.getLevel(),
                        hibernateInstance.getDirection()));
                }
            }
        }

        return instances;
    }

    /**
     * @param uri the uri of the instance to remove
     */
    public void deleteInstance(String uri)
    {
        // Remove from the database
        try {
            for (String wiki : this.wikis.getAllIds()) {
                try {
                    executeWrite(wiki, session -> deleteInstance(uri, session));
                } catch (XWikiException e) {
                    this.logger.error("Failed to delete replication configuration for instance [{}] and wiki [{}]", uri,
                        wiki, e);
                }
            }
        } catch (WikiManagerException e) {
            this.logger.error("Failed to get the wikis", e);
        }
    }

    private void executeWrite(String wiki, HibernateCallback<Object> callback) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        String currentWiki = xcontext.getWikiId();
        try {
            xcontext.setWikiId(wiki);

            store.executeWrite(xcontext, callback);
        } finally {
            xcontext.setWikiId(currentWiki);
        }
    }
}
