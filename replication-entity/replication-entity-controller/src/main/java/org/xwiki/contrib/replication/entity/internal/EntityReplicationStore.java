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
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Store entity replication configuration in hibernate.
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
    private Provider<XWikiStoreInterface> hibernateStoreProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReplicationCache cache;

    @Inject
    private ReplicationInstanceManager instanceManager;

    private void storeHibernateEntityReplication(EntityReference reference,
        List<DocumentReplicationControllerInstance> instances) throws XWikiException
    {
        long entityId = this.cache.toEntityId(reference);

        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore hibernateStore = (XWikiHibernateStore) this.hibernateStoreProvider.get();

        boolean transation = hibernateStore.beginTransaction(xcontext);

        try {
            Session session = hibernateStore.getSession(xcontext);

            // Delete existing instances
            Query<HibernateEntityReplicationInstance> query = session
                .createQuery("DELETE FROM HibernateEntityReplication AS instance where instance.entity = :entity");
            query.setParameter(ENTITY, entityId);

            // Add new instances
            if (instances != null) {
                for (DocumentReplicationControllerInstance instance : instances) {
                    hibernateStore.getSession(xcontext)
                        .save(new HibernateEntityReplicationInstance(entityId, instance));
                }
            }

            // Clear the cache for this entity
            this.cache.remove(entityId);
        } finally {
            if (transation) {
                hibernateStore.endTransaction(xcontext, true);
            }
        }
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
            return null;
        }

        long entityId = this.cache.toEntityId(reference);
        String cacheKey = this.cache.toCacheKey(entityId);

        List<DocumentReplicationControllerInstance> instances = this.cache.getResolveCache().get(cacheKey);

        if (instances != null) {
            return instances;
        }

        instances = getHibernateEntityReplication(entityId, cacheKey);
        if (instances == null) {
            instances = resolveHibernateEntityReplication(reference.getParent());
        }

        this.cache.getResolveCache().set(cacheKey, instances);

        return instances;
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

        return getHibernateEntityReplication(entityId, cacheKey);
    }

    private List<DocumentReplicationControllerInstance> getHibernateEntityReplication(long entityId, String cacheKey)
        throws XWikiException
    {
        List<DocumentReplicationControllerInstance> instances = this.cache.getDataCache().get(cacheKey);

        if (instances != null) {
            return instances;
        }

        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore hibernateStore = (XWikiHibernateStore) this.hibernateStoreProvider.get();

        boolean transation = hibernateStore.beginTransaction(xcontext);

        try {
            Session session = hibernateStore.getSession(xcontext);

            Query<HibernateEntityReplicationInstance> query = session.createQuery(
                "SELECT instance FROM HibernateEntityReplication AS instance where instance.entity = :entity");
            query.setParameter(ENTITY, entityId);

            List<HibernateEntityReplicationInstance> hibernateInstances = query.list();

            // There is a single entry
            if (hibernateInstances.size() == 1) {
                HibernateEntityReplicationInstance hibernateInstance = hibernateInstances.get(0);
                if (hibernateInstance.getInstance() == null) {
                    if (hibernateInstance.getLevel() == null) {
                        // Replication is disabled for all instances
                        return Collections.emptyList();
                    } else {
                        // Replication use the same level for all instances
                        return this.instanceManager.getInstances().stream()
                            .map(i -> new DocumentReplicationControllerInstance(i, hibernateInstance.getLevel()))
                            .collect(Collectors.toList());
                    }
                }
            }

            // There is several entries
            instances = new ArrayList<>(hibernateInstances.size());
            for (HibernateEntityReplicationInstance hibernateInstance : hibernateInstances) {
                ReplicationInstance instance = this.instanceManager.getInstance(hibernateInstance.getInstance());

                if (instance.getStatus() == Status.REGISTERED) {
                    instances.add(new DocumentReplicationControllerInstance(instance, hibernateInstance.getLevel()));
                }
            }

            this.cache.getDataCache().set(cacheKey, instances);

            return instances;
        } finally {
            if (transation) {
                hibernateStore.endTransaction(xcontext, true);
            }
        }
    }
}
