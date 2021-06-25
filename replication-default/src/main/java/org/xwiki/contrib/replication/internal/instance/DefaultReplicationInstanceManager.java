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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
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

    @Inject
    private Logger logger;

    @Override
    public Collection<ReplicationInstance> getInstances()
    {
        return Collections.unmodifiableCollection(this.cache.getInstances().values());
    }

    @Override
    public ReplicationInstance getInstance(String uri)
    {
        ReplicationInstance instance = this.cache.getInstances().get(uri);

        try {
            if (instance == null && this.client.getCurrentInstance().getURI().equals(uri)) {
                instance = this.client.getCurrentInstance();
            }
        } catch (ReplicationException e) {
            this.logger.error("Failed to get the current instance", e);
        }

        return instance;
    }

    @Override
    public boolean addInstance(ReplicationInstance instance) throws ReplicationException
    {
        if (this.cache.getInstances().containsKey(instance.getURI())) {
            return false;
        }

        // Add the instance to the store
        this.store.addInstance(instance);

        return true;
    }

    @Override
    public boolean removeInstance(String uri) throws ReplicationException
    {
        ReplicationInstance instance = this.cache.getInstances().get(uri);

        if (instance == null) {
            return false;
        }

        // Remove the instance from the store
        this.store.deleteInstance(uri);

        // Notify the target instance that it's not linked anymore
        try {
            this.client.unregister(instance);
        } catch (Exception e) {
            // TODO: put it in a retry queue
            this.logger.warn("Failed to notify the instance it's been removed [{}]: {}", uri,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return true;
    }

    @Override
    public void requestInstance(String uri) throws ReplicationException
    {
        // Send a request to the target instance
        ReplicationInstance instance;
        try {
            instance = this.client.register(uri);
        } catch (Exception e) {
            throw new ReplicationException("Failed to register the instance on [" + uri + "]", e);
        }

        // Add instance to the store
        this.store.addInstance(instance);
    }

    @Override
    public Collection<ReplicationInstance> getRequestedInstances()
    {
        return this.cache.getInstances().values().stream().filter(i -> i.getStatus() == Status.REQUESTED)
            .collect(Collectors.toList());
    }

    @Override
    public boolean cancelRequestedInstance(String uri) throws ReplicationException
    {
        ReplicationInstance instance = this.cache.getInstances().get(uri);

        if (instance == null || instance.getStatus() != Status.REQUESTED) {
            return false;
        }

        // Remove instance from the store
        this.store.deleteInstance(instance.getURI());

        // Notify the instance about the cancelled request
        try {
            this.client.unregister(instance);
        } catch (Exception e) {
            // TODO: put it in a retry queue
            this.logger.warn("Failed to notify the instance it's not requested anymore [{}]: {}", uri,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return true;
    }

    @Override
    public boolean confirmRequestedInstance(ReplicationInstance newInstance) throws ReplicationException
    {
        ReplicationInstance existingInstance = this.cache.getInstances().get(newInstance.getURI());

        if (existingInstance == null || existingInstance.getStatus() != Status.REQUESTED) {
            return false;
        }

        // Update
        this.store.updateInstance(newInstance);

        return true;
    }

    @Override
    public Collection<ReplicationInstance> getRequestingInstances()
    {
        return this.cache.getInstances().values().stream().filter(i -> i.getStatus() == Status.REQUESTING)
            .collect(Collectors.toList());
    }

    @Override
    public boolean acceptRequestingInstance(String uri) throws ReplicationException
    {
        ReplicationInstance instance = this.cache.getInstances().get(uri);

        if (instance == null || instance.getStatus() != Status.REQUESTING) {
            return false;
        }

        // Notify the instance of the acceptance
        try {
            instance = this.client.register(instance.getURI());
        } catch (Exception e) {
            throw new ReplicationException("Failed to send a request to instance [" + uri + "]", e);
        }

        if (instance != null) {
            // Update the instance metadata and status
            this.store.updateInstance(instance);

            return true;
        }

        return false;
    }

    @Override
    public boolean declineRequestingInstance(String uri) throws ReplicationException
    {
        ReplicationInstance instance = this.cache.getInstances().get(uri);

        if (instance == null || instance.getStatus() != Status.REQUESTING) {
            return false;
        }

        // Notify the instance about the refusal
        try {
            this.client.unregister(instance);
        } catch (Exception e) {
            throw new ReplicationException("Failed to decline the request of instance [" + uri + "]", e);
        }

        // Remove the instance locally
        return removeRequestingInstance(uri);
    }

    @Override
    public boolean removeRequestingInstance(String uri) throws ReplicationException
    {
        ReplicationInstance instance = this.cache.getInstances().get(uri);

        if (instance == null || instance.getStatus() != Status.REQUESTING) {
            return false;
        }

        // Remove it from the requesting list
        this.store.deleteInstance(uri);

        return true;
    }

    @Override
    public void reload() throws ReplicationException
    {
        this.cache.getInstances().clear();
        this.store.loadInstances().forEach(i -> this.cache.getInstances().put(i.getURI(), i));
    }
}
