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

/**
 * @version $Id$
 * @since 1.8.0
 */
@Component(roles = ReplicationDocumentStoreCache.class)
@Singleton
public class ReplicationDocumentStoreCache implements Initializable, Disposable
{
    @Inject
    private CacheManager cacheManager;

    private Cache<ReplicationDocumentStoreCacheEntry> cache;

    /**
     * The metadata associated with a document.
     * 
     * @version $Id$
     */
    public class ReplicationDocumentStoreCacheEntry
    {
        private String owner;

        private Boolean conflict;

        private Boolean readonly;

        /**
         * @return the owner
         */
        public String getOwner()
        {
            return this.owner;
        }

        /**
         * @param owner the owner
         */
        public void setOwner(String owner)
        {
            this.owner = owner;
        }

        /**
         * @return the conflict
         */
        public Boolean getConflict()
        {
            return this.conflict;
        }

        /**
         * @param conflict the conflict
         */
        public void setConflict(boolean conflict)
        {
            this.conflict = conflict;
        }

        /**
         * @return the readonly
         * @since 1.12.0
         */
        public Boolean getReadonly()
        {
            return this.readonly;
        }

        /**
         * @param readonly the readonly
         * @since 1.12.0
         */
        public void setReadonly(boolean readonly)
        {
            this.readonly = readonly;
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.cache =
                this.cacheManager.createNewLocalCache(new LRUCacheConfiguration("replication.entity.document"));
        } catch (CacheException e) {
            throw new InitializationException("Failed to create a cache", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.cache.dispose();
    }

    /**
     * @param docId the identifier of the document
     * @param create true of an entry should be create when none already exist
     * @return the entry associated with the document identifier
     */
    public ReplicationDocumentStoreCacheEntry getEntry(String docId, boolean create)
    {
        ReplicationDocumentStoreCacheEntry entry = this.cache.get(docId);

        if (create && entry == null) {
            entry = new ReplicationDocumentStoreCacheEntry();
            this.cache.set(docId, entry);
        }

        return entry;
    }

    /**
     * @param docId the document identifier
     * @param owner the owner of the document
     */
    public void setOwner(String docId, String owner)
    {
        ReplicationDocumentStoreCacheEntry entry = getEntry(docId, true);

        entry.owner = owner;
    }

    /**
     * @param docId the document identifier
     * @param conflict true if the document is in conflict
     */
    public void setConflict(String docId, boolean conflict)
    {
        ReplicationDocumentStoreCacheEntry entry = getEntry(docId, true);

        entry.conflict = conflict;
    }

    /**
     * @param docId the document identifier
     * @param readonly true if the document is readonly
     */
    public void setReadonly(String docId, boolean readonly)
    {
        ReplicationDocumentStoreCacheEntry entry = getEntry(docId, true);

        entry.readonly = readonly;
    }

    /**
     * @param docId the document identifier
     */
    public void remove(String docId)
    {
        this.cache.remove(docId);
    }
}
