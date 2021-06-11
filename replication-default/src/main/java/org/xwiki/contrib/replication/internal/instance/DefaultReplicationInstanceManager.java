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
package org.xwiki.contrib.replication.internal.instance;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.internal.ReplicationClient;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultReplicationInstanceManager implements ReplicationInstanceManager
{
    @Inject
    private ReplicationClient client;

    @Inject
    private ReplicationInstanceStore store;

    @Inject
    private ReplicationInstanceCache cache;

    @Override
    public Collection<ReplicationInstance> getInstances()
    {
        return Collections.unmodifiableCollection(this.cache.getInstances().values());
    }

    @Override
    public ReplicationInstance getInstance(String id)
    {
        return this.cache.getInstances().get(id);
    }

    @Override
    public boolean removeInstance(ReplicationInstance instance) throws ReplicationException
    {
        if (!this.cache.getInstances().containsKey(instance.getId())) {
            return false;
        }

        // Remove the instance from the store
        this.store.deleteInstance(instance);

        // TODO: Notify the target instance that it's not linked anymore

        return true;
    }

    @Override
    public void requestInstance(String uri) throws ReplicationException
    {
        // TODO: send a request to the target instance

        // Add instance to the store
        this.store.saveRequestedInstance(uri);
    }

    @Override
    public Collection<String> getRequestedInstances()
    {
        return Collections.unmodifiableCollection(this.cache.getRequestedInstances());
    }

    @Override
    public boolean cancelRequestedInstance(String uri) throws ReplicationException
    {
        if (!this.cache.getRequestedInstances().contains(uri)) {
            return false;
        }

        // Remove instance from the store
        this.store.deleteRequestedInstance(uri);

        // TODO: notify the instance about the cancelled request

        return true;
    }

    @Override
    public boolean confirmRequestedInstance(ReplicationInstance instance) throws ReplicationException
    {
        if (!this.cache.getRequestedInstances().contains(instance.getURI())) {
            return false;
        }

        // Add instance to accepted instances
        this.store.saveInstance(instance);

        // Remove instance from requested list
        this.store.deleteRequestedInstance(instance.getURI());

        return true;
    }

    @Override
    public ReplicationInstance getRequestingInstance(String id)
    {
        return this.cache.getRequestingInstances().get(id);
    }

    @Override
    public Collection<ReplicationInstance> getRequestingInstances()
    {
        return Collections.unmodifiableCollection(this.cache.getRequestingInstances().values());
    }

    @Override
    public void addRequestingInstance(ReplicationInstance instance) throws ReplicationException
    {
        // Add the instance to the store
        this.store.saveRequestingInstance(instance);
    }

    @Override
    public boolean acceptRequestingInstance(ReplicationInstance instance) throws ReplicationException
    {
        if (!this.cache.getRequestingInstances().containsKey(instance.getId())) {
            return false;
        }

        // TODO: notify the instance of the acceptance

        // Save the instance to the store
        this.store.saveInstance(instance);

        // Remove it from the requesting list
        this.store.deleteRequestingInstance(instance);

        return true;
    }

    @Override
    public boolean declineRequestingInstance(ReplicationInstance instance) throws ReplicationException
    {
        if (!this.cache.getRequestingInstances().containsKey(instance.getId())) {
            return false;
        }

        // TODO: Notify the instance about the refusal

        return removeRequestingInstance(instance);
    }

    @Override
    public boolean removeRequestingInstance(ReplicationInstance instance) throws ReplicationException
    {
        if (!this.cache.getRequestingInstances().containsKey(instance.getId())) {
            return false;
        }

        // Remove it from the requesting list
        this.store.deleteRequestingInstance(instance);

        return true;
    }

    @Override
    public void reload() throws ReplicationException
    {
        this.cache.getInstances().clear();
        this.store.loadInstances().forEach(i -> this.cache.getInstances().put(i.getId(), i));

        this.cache.getRequestingInstances().clear();
        this.store.loadRequestingInstances().forEach(i -> this.cache.getRequestingInstances().put(i.getId(), i));

        this.cache.getRequestedInstances().clear();
        this.cache.getRequestedInstances().addAll(this.store.loadRequestedInstances());
    }
}
