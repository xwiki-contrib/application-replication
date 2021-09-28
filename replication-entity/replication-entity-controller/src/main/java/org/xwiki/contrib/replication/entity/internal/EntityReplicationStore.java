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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.internal.message.EntityReplicationControllerSender;
import org.xwiki.model.reference.EntityReference;
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
@Component(roles = EntityReplicationStore.class)
@Singleton
public class EntityReplicationStore
{
    private static final String ENTITY = "entity";

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private XWikiStoreInterface hibernateStore;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReplicationCache cache;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private WikiDescriptorManager wikis;

    @Inject
    private EntityReplicationControllerSender sender;

    @Inject
    private ReplicationContext replicationContext;

    @Inject
    private Logger logger;

    /**
     * @param reference the reference of the entity
     * @param instances the instance to send the entity to
     * @throws XWikiException when failing to store entity replication configuration
     */
    public void storeHibernateEntityReplication(EntityReference reference,
        List<DocumentReplicationControllerInstance> instances) throws XWikiException
    {
        long entityId = this.cache.toEntityId(reference);

        executeWrite(reference.getRoot().getName(),
            session -> storeHibernateEntityReplication(entityId, instances, session));

        // Clear the cache for this entity
        this.cache.remove(entityId);

        // Synchronize configuration with other instances
        // TODO: move it to a listener
        if (!this.replicationContext.isReplicationMessage()) {
            try {
                this.sender.send(reference, instances);
            } catch (Exception e) {
                // TODO: put this in a retry queue
                this.logger.error("Failed to notify other instances about the replication configuration change", e);
            }
        }
    }

    private Object storeHibernateEntityReplication(long entityId, List<DocumentReplicationControllerInstance> instances,
        Session session)
    {
        // Delete existing instances
        deleteEntity(entityId, session);

        // Add new instances
        if (instances != null) {
            if (instances.isEmpty()) {
                session.save(new HibernateEntityReplicationInstance(entityId,
                    new DocumentReplicationControllerInstance(null, null)));
            } else {
                for (DocumentReplicationControllerInstance instance : instances) {
                    session.save(new HibernateEntityReplicationInstance(entityId, instance));
                }
            }
        }

        return null;
    }

    private Object deleteEntity(long entityId, Session session)
    {
        // Delete existing instances
        Query<?> query = session
            .createQuery("DELETE FROM HibernateEntityReplicationInstance AS instance where instance.entity = :entity");
        query.setParameter(ENTITY, entityId);
        query.executeUpdate();

        return null;
    }

    private Object deleteInstance(String uri, Session session)
    {
        // Delete existing instances
        Query<?> query = session.createQuery(
            "DELETE FROM HibernateEntityReplicationInstance AS instance where instance.instance = :instance");
        query.setParameter("instance", uri);
        query.executeUpdate();

        return null;
    }

    /**
     * @param instances the stored instances
     * @return the resolved instances
     */
    public List<DocumentReplicationControllerInstance> resolveControllerInstances(
        List<DocumentReplicationControllerInstance> instances)
    {
        if (instances != null) {
            List<DocumentReplicationControllerInstance> resolvedInstances = new ArrayList<>(instances.size());

            for (DocumentReplicationControllerInstance instance : instances) {
                if (instance.getInstance() == null) {
                    resolvedInstances.addAll(this.instanceManager.getInstances().stream()
                        .map(i -> new DocumentReplicationControllerInstance(i, instance.getLevel()))
                        .collect(Collectors.toList()));
                } else {
                    resolvedInstances.add(instance);
                }
            }

            return resolvedInstances;
        }

        return null;
    }

    /**
     * @param reference the reference of the entity
     * @return the instances to send the entity to
     * @throws XWikiException when failing to get the instances
     */
    public List<DocumentReplicationControllerInstance> resolveHibernateEntityReplication(EntityReference reference)
        throws XWikiException
    {
        if (reference == null) {
            // Don't replicate by default
            return Collections.emptyList();
        }

        long entityId = this.cache.toEntityId(reference);
        String cacheKey = this.cache.toCacheKey(entityId);

        // Try the cache
        List<DocumentReplicationControllerInstance> instances = this.cache.getResolveCache().get(cacheKey);

        // If not in the cache, load from the database
        if (instances == null) {
            instances = resolveControllerInstances(
                getHibernateEntityReplication(reference.getRoot().getName(), entityId, cacheKey));
        }

        // Fallback on parent if nothing is explicitly set for this reference
        if (instances == null) {
            instances = resolveHibernateEntityReplication(reference.getParent());
        }

        // Update the cache
        this.cache.getResolveCache().set(cacheKey, instances);

        return instances;
    }

    /**
     * @param reference the reference of the entity
     * @param instance the configured instance
     * @return the configuration of the instance
     * @throws XWikiException when failing to get the instances
     */
    public DocumentReplicationControllerInstance resolveHibernateEntityReplication(EntityReference reference,
        ReplicationInstance instance) throws XWikiException
    {
        List<DocumentReplicationControllerInstance> configuredInstances = resolveHibernateEntityReplication(reference);

        for (DocumentReplicationControllerInstance configuredInstance : configuredInstances) {
            if (configuredInstance.getInstance().getStatus() == null) {
                return configuredInstance;
            }
        }

        return new DocumentReplicationControllerInstance(instance, DocumentReplicationLevel.ALL);
    }

    /**
     * @param reference the reference of the entity
     * @return the instances directly configured at this entity level
     * @throws XWikiException when failing to get the instances
     */
    public List<DocumentReplicationControllerInstance> getHibernateEntityReplication(EntityReference reference)
        throws XWikiException
    {
        long entityId = this.cache.toEntityId(reference);
        String cacheKey = this.cache.toCacheKey(entityId);

        return getHibernateEntityReplication(reference.getRoot().getName(), entityId, cacheKey);
    }

    private List<DocumentReplicationControllerInstance> getHibernateEntityReplication(String wiki, long entityId,
        String cacheKey) throws XWikiException
    {
        List<DocumentReplicationControllerInstance> instances = this.cache.getDataCache().get(cacheKey);

        if (instances != null) {
            return instances;
        }

        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore store = (XWikiHibernateStore) this.hibernateStore;

        String currentWiki = xcontext.getWikiId();
        try {
            xcontext.setWikiId(wiki);

            instances = store.executeRead(xcontext, session -> getHibernateEntityReplication(entityId, session));
        } finally {
            xcontext.setWikiId(currentWiki);
        }

        this.cache.getDataCache().set(cacheKey, instances);

        return instances;
    }

    private List<DocumentReplicationControllerInstance> getHibernateEntityReplication(long entityId, Session session)
    {
        Query<HibernateEntityReplicationInstance> query = session.createQuery(
            "SELECT instance FROM HibernateEntityReplicationInstance AS instance where instance.entity = :entity",
            HibernateEntityReplicationInstance.class);
        query.setParameter(ENTITY, entityId);

        List<HibernateEntityReplicationInstance> hibernateInstances = query.list();

        // There is no entry
        if (hibernateInstances.isEmpty()) {
            // This level inherit its parent configuration
            return null;
        }

        List<DocumentReplicationControllerInstance> instances = new ArrayList<>(hibernateInstances.size());
        for (HibernateEntityReplicationInstance hibernateInstance : hibernateInstances) {
            if (hibernateInstance.getInstance().isEmpty()) {
                // Replication use the same level for all instances
                instances.add(new DocumentReplicationControllerInstance(null, hibernateInstance.getLevel()));
            } else {
                ReplicationInstance instance = this.instanceManager.getInstance(hibernateInstance.getInstance());

                if (instance != null && (instance.getStatus() == Status.REGISTERED || instance.getStatus() == null)) {
                    instances.add(new DocumentReplicationControllerInstance(instance, hibernateInstance.getLevel()));
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

        // Remove from the cache
        this.cache.removeAll();
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
