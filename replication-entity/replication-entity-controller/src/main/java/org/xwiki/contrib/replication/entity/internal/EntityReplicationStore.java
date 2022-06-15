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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiException;

/**
 * Store entity replication configuration in the database.
 * 
 * @version $Id$
 */
@Component(roles = EntityReplicationStore.class)
@Singleton
public class EntityReplicationStore
{
    @Inject
    private EntityReplicationStoreHibernate hibernate;

    @Inject
    private EntityReplicationCache cache;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private EntityReplicationInitializer initializer;


    /**
     * @param reference the reference of the entity
     * @param instances the instance to send the entity to
     * @throws XWikiException when failing to store entity replication configuration
     */
    public void storeHibernateEntityReplication(EntityReference reference,
        List<DocumentReplicationControllerInstance> instances) throws XWikiException
    {
        long entityId = this.cache.toEntityId(reference);

        this.hibernate.storeHibernateEntityReplication(reference.getRoot().getName(), entityId, instances);

        // Clear the cache for this entity
        this.cache.remove(entityId);
    }

    /**
     * @param instances the stored configuration
     * @return the resolved configuration
     * @throws ReplicationException when failing to resolve the configuration
     */
    public Map<String, DocumentReplicationControllerInstance> resolveControllerInstancesMap(
        Collection<DocumentReplicationControllerInstance> instances) throws ReplicationException
    {
        if (instances != null) {
            Map<String, DocumentReplicationControllerInstance> resolvedInstances =
                new HashMap<>(this.instanceManager.getRegisteredInstances().size() + 1);

            DocumentReplicationControllerInstance allConfiguration = null;

            // Add direct configuration
            for (DocumentReplicationControllerInstance instance : instances) {
                if (instance.getInstance() != null) {
                    resolvedInstances.put(instance.getInstance().getURI(), instance);
                } else {
                    allConfiguration = instance;
                }
            }

            // Add whildcard configuration
            if (allConfiguration != null) {
                for (ReplicationInstance replicationInstance : this.instanceManager.getInstances()) {
                    if (!resolvedInstances.containsKey(replicationInstance.getURI())) {
                        resolvedInstances.put(replicationInstance.getURI(), new DocumentReplicationControllerInstance(
                            replicationInstance, allConfiguration.getLevel(), allConfiguration.isReadonly()));
                    }
                }
            }

            return resolvedInstances;
        }

        // Not configured at this level
        return null;
    }

    /**
     * @param instances the stored configuration
     * @return the resolved configuration
     * @throws ReplicationException when failing to resolve the configuration
     */
    public Collection<DocumentReplicationControllerInstance> resolveControllerInstances(
        Collection<DocumentReplicationControllerInstance> instances) throws ReplicationException
    {
        Map<String, DocumentReplicationControllerInstance> resolvedInstances = resolveControllerInstancesMap(instances);

        return resolvedInstances != null ? resolvedInstances.values() : null;
    }

    /**
     * @param reference the reference of the entity
     * @return the instances to send the entity to
     * @throws XWikiException when failing to get the instances
     * @throws ReplicationException when failing to access instances
     */
    public Collection<DocumentReplicationControllerInstance> resolveHibernateEntityReplication(
        EntityReference reference) throws XWikiException, ReplicationException
    {
        if (reference == null) {
            // Don't replicate by default
            return Collections.emptyList();
        }

        long entityId = this.cache.toEntityId(reference);
        String cacheKey = this.cache.toCacheKey(entityId);

        // Try the cache
        EntityReplicationCacheEntry cacheEntry = this.cache.getResolveCache().get(cacheKey);

        if (cacheEntry != null) {
            return cacheEntry.getConfiguration();
        }

        // Load from the database
        Collection<DocumentReplicationControllerInstance> instances = resolveControllerInstances(
            getHibernateEntityReplication(reference.getRoot().getName(), entityId, cacheKey));

        // Fallback on parent if nothing is explicitly set for this reference
        if (instances == null) {
            instances = resolveHibernateEntityReplication(reference.getParent());
        }

        // Update the cache
        this.cache.getResolveCache().set(cacheKey, new EntityReplicationCacheEntry(instances));

        return instances;
    }

    /**
     * @param reference the reference of the entity
     * @param instance the configured instance
     * @return the configuration of the instance
     * @throws XWikiException when failing to get the instances
     * @throws ReplicationException when failing to access instances
     */
    public DocumentReplicationControllerInstance resolveHibernateEntityReplication(EntityReference reference,
        ReplicationInstance instance) throws XWikiException, ReplicationException
    {
        Collection<DocumentReplicationControllerInstance> configuredInstances =
            resolveHibernateEntityReplication(reference);

        for (DocumentReplicationControllerInstance configuredInstance : configuredInstances) {
            if (configuredInstance.getInstance() == instance) {
                return configuredInstance;
            }
        }

        return new DocumentReplicationControllerInstance(instance, DocumentReplicationLevel.ALL, false);
    }

    /**
     * @param reference the reference of the entity
     * @return the instances directly configured at this entity level
     * @throws XWikiException when failing to get the instances
     */
    public Collection<DocumentReplicationControllerInstance> getHibernateEntityReplication(EntityReference reference)
        throws XWikiException
    {
        if (reference == null) {
            return null;
        }

        long entityId = this.cache.toEntityId(reference);
        String cacheKey = this.cache.toCacheKey(entityId);

        return getHibernateEntityReplication(reference.getRoot().getName(), entityId, cacheKey);
    }

    private Collection<DocumentReplicationControllerInstance> getHibernateEntityReplication(String wiki, long entityId,
        String cacheKey) throws XWikiException
    {
        if (!this.initializer.isInitialized()) {
            // The configuration mapping is not registered yet
            return Collections.emptyList();
        }

        EntityReplicationCacheEntry cacheEntry = this.cache.getDataCache().get(cacheKey);

        if (cacheEntry != null) {
            return cacheEntry.getConfiguration();
        }

        Collection<DocumentReplicationControllerInstance> instances =
            this.hibernate.getHibernateEntityReplication(wiki, entityId);

        this.cache.getDataCache().set(cacheKey, new EntityReplicationCacheEntry(instances));

        return instances;
    }

    /**
     * @param uri the uri of the instance to remove
     */
    public void deleteInstance(String uri)
    {
        // Remove from the database
        this.hibernate.deleteInstance(uri);

        // Remove from the cache
        this.cache.removeAll();
    }
}
