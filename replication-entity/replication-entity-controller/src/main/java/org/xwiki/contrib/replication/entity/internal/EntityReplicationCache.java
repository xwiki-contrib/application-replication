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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.util.Util;

/**
 * Store entity replication configuration in Hibernate.
 * 
 * @version $Id$
 */
@Component(roles = EntityReplicationCache.class)
@Singleton
public class EntityReplicationCache implements Initializable, Disposable
{
    @Inject
    @Named("uid")
    private EntityReferenceSerializer<String> idSerializer;

    @Inject
    private CacheManager cacheManager;

    private Cache<List<DocumentReplicationControllerInstance>> dataCache;

    private Cache<List<DocumentReplicationControllerInstance>> resolveCache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.dataCache =
                this.cacheManager.createNewLocalCache(new LRUCacheConfiguration("replication.entity.controller.data"));
            this.resolveCache = this.cacheManager
                .createNewLocalCache(new LRUCacheConfiguration("replication.entity.controller.resolve"));
        } catch (CacheException e) {
            throw new InitializationException("Failed to create a cache", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.dataCache.dispose();
        this.resolveCache.dispose();
    }

    /**
     * @return the data cache
     */
    public Cache<List<DocumentReplicationControllerInstance>> getDataCache()
    {
        return this.dataCache;
    }

    /**
     * @return the resolve cache
     */
    public Cache<List<DocumentReplicationControllerInstance>> getResolveCache()
    {
        return this.resolveCache;
    }

    /**
     * @param reference the reference of the entity
     * @return the id of the entity
     */
    public long toEntityId(EntityReference reference)
    {
        return Util.getHash(this.idSerializer.serialize(reference));
    }

    /**
     * @param entityId the id of the entity
     * @return the value used as cache key
     */
    public String toCacheKey(long entityId)
    {
        return String.valueOf(entityId);
    }

    /**
     * @param entityId th id of the entity to remove from the cache
     */
    public void remove(long entityId)
    {
        // Clear the data cache for this entity
        this.dataCache.remove(toCacheKey(entityId));

        // Clear the whole resolve cache since the change might impact children
        this.resolveCache.removeAll();
    }

    /**
     * @param reference the reference of the deleted document
     */
    public void onDelete(DocumentReference reference)
    {
        this.dataCache.remove(toCacheKey(toEntityId(reference)));

        this.resolveCache.removeAll();
    }

    /**
     * Empty the entire cache.
     */
    public void removeAll()
    {
        this.dataCache.removeAll();
        this.resolveCache.removeAll();
    }
}
