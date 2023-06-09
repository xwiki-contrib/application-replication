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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
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
        Collection<DocumentReplicationControllerInstance> instances) throws XWikiException
    {
        long entityId = this.cache.toEntityId(reference);

        this.hibernate.storeHibernateEntityReplication(reference.getRoot().getName(), entityId, instances);

        // Clear the cache for this entity
        this.cache.remove(entityId);
    }

    /**
     * @param configurations the stored configuration
     * @param relay true if the relay configuration should be returned, false for the replication configuration
     * @return the resolved configuration
     * @throws ReplicationException when failing to resolve the configuration
     */
    public Map<String, DocumentReplicationControllerInstance> resolveControllerInstancesMap(
        Collection<DocumentReplicationControllerInstance> configurations, boolean relay) throws ReplicationException
    {
        if (configurations != null) {
            Map<String, DocumentReplicationControllerInstance> resolvedConfigurations =
                new HashMap<>(this.instanceManager.getRegisteredInstances().size() + 1);

            DocumentReplicationControllerInstance allConfiguration = null;

            // Add direct configurations and extract the wildcard configuration
            for (DocumentReplicationControllerInstance configuration : configurations) {
                if (configuration.getInstance() != null) {
                    resolvedConfigurations.put(configuration.getInstance().getURI(), configuration);
                } else {
                    allConfiguration = configuration;
                }
            }

            // Resolve the whildcard configuration
            if (allConfiguration != null) {
                DocumentReplicationDirection defaultDirection = allConfiguration.getDirection();

                // Relaying messages is always allowed in case of wildcard configuration
                if (relay && defaultDirection == DocumentReplicationDirection.RECEIVE_ONLY) {
                    defaultDirection = DocumentReplicationDirection.BOTH;
                }

                for (ReplicationInstance instance : this.instanceManager.getInstances()) {
                    // Filter out explicitly configured instances
                    if (!resolvedConfigurations.containsKey(instance.getURI())) {
                        resolvedConfigurations.put(instance.getURI(), new DocumentReplicationControllerInstance(
                            instance, allConfiguration.getLevel(), defaultDirection));
                    }
                }
            }

            return resolvedConfigurations;
        }

        // Not configured at this level
        return null;
    }

    /**
     * @param instances the stored configuration
     * @param relay true if the relay configuration should be returned, false for the replication configuration
     * @return the resolved configuration
     * @throws ReplicationException when failing to resolve the configuration
     */
    public Collection<DocumentReplicationControllerInstance> resolveControllerInstances(
        Collection<DocumentReplicationControllerInstance> instances, boolean relay) throws ReplicationException
    {
        Map<String, DocumentReplicationControllerInstance> resolvedConfigurations =
            resolveControllerInstancesMap(instances, relay);

        return resolvedConfigurations != null ? resolvedConfigurations.values() : null;
    }

    /**
     * @param reference the reference of the entity
     * @param relay true if the relay configuration should be returned, false for the replication configuration
     * @return the instances to send the entity to
     * @throws XWikiException when failing to get the instances
     * @throws ReplicationException when failing to access instances
     */
    public Collection<DocumentReplicationControllerInstance> resolveHibernateEntityReplication(
        EntityReference reference, boolean relay) throws XWikiException, ReplicationException
    {
        if (reference == null) {
            // Don't replicate by default
            return Collections.emptyList();
        }

        long entityId = this.cache.toEntityId(reference);
        String cacheKey = this.cache.toResolveCacheKey(entityId, relay);

        // Try the cache
        EntityReplicationCacheEntry cacheEntry = this.cache.getResolveCache().get(cacheKey);

        if (cacheEntry != null) {
            return cacheEntry.getConfiguration();
        }

        // Load from the database
        Collection<DocumentReplicationControllerInstance> configurations =
            resolveControllerInstances(getHibernateEntityReplication(reference.getRoot().getName(), entityId), relay);

        // Fallback on parent if nothing is explicitly set for this reference
        if (configurations == null) {
            configurations = resolveHibernateEntityReplication(reference.getParent(), relay);
        }

        // Update the cache
        this.cache.getResolveCache().set(cacheKey, new EntityReplicationCacheEntry(configurations));

        return configurations;
    }

    /**
     * @param reference the reference of the entity
     * @param instance the configured instance
     * @param relay true if the relay configuration should be returned, false for the replication configuration
     * @return the configuration of the instance
     * @throws XWikiException when failing to get the instances
     * @throws ReplicationException when failing to access instances
     */
    public DocumentReplicationControllerInstance resolveHibernateEntityReplication(EntityReference reference,
        ReplicationInstance instance, boolean relay) throws XWikiException, ReplicationException
    {
        Collection<DocumentReplicationControllerInstance> resolvedConfigurations =
            resolveHibernateEntityReplication(reference, relay);

        for (DocumentReplicationControllerInstance resolvedConfiguration : resolvedConfigurations) {
            if (resolvedConfiguration.getInstance() == instance) {
                return resolvedConfiguration;
            }
        }

        return new DocumentReplicationControllerInstance(instance, DocumentReplicationLevel.ALL, null);
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

        return getHibernateEntityReplication(reference.getRoot().getName(), entityId);
    }

    private Collection<DocumentReplicationControllerInstance> getHibernateEntityReplication(String wiki, long entityId)
        throws XWikiException
    {
        if (!this.initializer.isInitialized()) {
            // The configuration mapping is not registered yet
            return Collections.emptyList();
        }

        String cacheKey = this.cache.toDataCacheKey(entityId);

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
