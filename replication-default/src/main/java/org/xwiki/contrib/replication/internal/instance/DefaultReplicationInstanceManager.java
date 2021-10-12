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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private Logger logger;

    private Map<String, ReplicationInstance> instances;

    @Override
    public ReplicationInstance getCurrentInstance() throws ReplicationException
    {
        return this.client.getCurrentInstance();
    }

    private Map<String, ReplicationInstance> getInternalInstances() throws ReplicationException
    {
        if (this.instances == null) {
            reload();
        }

        return this.instances;
    }

    @Override
    public Collection<ReplicationInstance> getInstances() throws ReplicationException
    {
        return Collections.unmodifiableCollection(getInternalInstances().values());
    }

    @Override
    public ReplicationInstance getInstance(String uri) throws ReplicationException
    {
        if (uri == null) {
            return null;
        }

        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstances().get(cleanURI);

        try {
            if (instance == null && getCurrentInstance().getURI().equals(cleanURI)) {
                instance = getCurrentInstance();
            }
        } catch (ReplicationException e) {
            this.logger.error("Failed to get the current instance", e);
        }

        return instance;
    }

    @Override
    public boolean addInstance(ReplicationInstance instance) throws ReplicationException
    {
        if (getInternalInstances().containsKey(instance.getURI())) {
            return false;
        }

        // Add the instance to the store
        this.store.addInstance(instance);

        return true;
    }

    @Override
    public ReplicationInstance removeInstance(String uri) throws ReplicationException
    {
        if (uri == null) {
            return null;
        }

        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstances().get(cleanURI);

        if (instance == null) {
            return null;
        }

        removeInstanceInternal(instance);

        return instance;
    }

    private ReplicationInstance removeInstanceInternal(ReplicationInstance instance) throws ReplicationException
    {
        // Remove the instance from the store
        this.store.deleteInstance(instance.getURI());

        return instance;
    }

    @Override
    public Collection<ReplicationInstance> getRegisteredInstances() throws ReplicationException
    {
        return getInternalInstances().values().stream().filter(i -> i.getStatus() == Status.REGISTERED)
            .collect(Collectors.toList());
    }

    @Override
    public void requestInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        // Send a request to the target instance
        Status status;
        try {
            status = this.client.register(cleanURI);
        } catch (Exception e) {
            throw new ReplicationException("Failed to register the instance on [" + cleanURI + "]", e);
        }

        // Create the new instance
        ReplicationInstance instance = new DefaultReplicationInstance(null, cleanURI, status);

        // Add instance to the store
        this.store.addInstance(instance);
    }

    @Override
    public boolean removeRegisteredInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstances().get(cleanURI);

        if (instance == null || instance.getStatus() != Status.REGISTERED) {
            return false;
        }

        // Remove instance from the store/cache
        removeInstanceInternal(instance);

        // Notify the instance about the removed instance
        try {
            this.client.unregister(instance);
        } catch (Exception e) {
            // TODO: put it in a retry queue
            this.logger.warn("Failed to notify the instance it's not registered anymore [{}]: {}", cleanURI,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return true;
    }

    @Override
    public Collection<ReplicationInstance> getRequestedInstances() throws ReplicationException
    {
        return getInternalInstances().values().stream().filter(i -> i.getStatus() == Status.REQUESTED)
            .collect(Collectors.toList());
    }

    @Override
    public boolean cancelRequestedInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstances().get(cleanURI);

        if (instance == null || instance.getStatus() != Status.REQUESTED) {
            return false;
        }

        // Remove instance from the store/cache
        removeInstanceInternal(instance);

        // Notify the instance about the cancelled request
        try {
            this.client.unregister(instance);
        } catch (Exception e) {
            // TODO: put it in a retry queue
            this.logger.warn("Failed to notify the instance it's not requested anymore [{}]: {}", cleanURI,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return true;
    }

    @Override
    public boolean confirmRequestedInstance(ReplicationInstance newInstance) throws ReplicationException
    {
        ReplicationInstance existingInstance = getInternalInstances().get(newInstance.getURI());

        if (existingInstance == null || existingInstance.getStatus() != Status.REQUESTED) {
            return false;
        }

        // Update
        this.store.updateInstance(newInstance);

        return true;
    }

    @Override
    public Collection<ReplicationInstance> getRequestingInstances() throws ReplicationException
    {
        return getInternalInstances().values().stream().filter(i -> i.getStatus() == Status.REQUESTING)
            .collect(Collectors.toList());
    }

    @Override
    public boolean acceptRequestingInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstances().get(cleanURI);

        if (instance == null || instance.getStatus() != Status.REQUESTING) {
            return false;
        }

        // Notify the instance of the acceptance
        Status status;
        try {
            status = this.client.register(cleanURI);
        } catch (Exception e) {
            throw new ReplicationException("Failed to send a request to instance [" + cleanURI + "]", e);
        }

        if (status != Status.REQUESTING) {
            // Update the instance status
            this.store.updateStatus(instance, status);
        }

        return true;
    }

    @Override
    public boolean declineRequestingInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstances().get(cleanURI);

        if (instance == null || instance.getStatus() != Status.REQUESTING) {
            return false;
        }

        // Notify the instance about the refusal
        try {
            this.client.unregister(instance);
        } catch (Exception e) {
            throw new ReplicationException("Failed to decline the request of instance [" + cleanURI + "]", e);
        }

        // Remove the instance locally
        removeInstanceInternal(instance);

        return true;
    }

    @Override
    public ReplicationInstance removeRequestingInstance(String uri) throws ReplicationException
    {
        ReplicationInstance instance = getInternalInstances().get(uri);

        if (instance == null || instance.getStatus() != Status.REQUESTING) {
            return null;
        }

        // Remove instance from the store/cache
        removeInstanceInternal(instance);

        return instance;
    }

    @Override
    public void reload() throws ReplicationException
    {
        Map<String, ReplicationInstance> newInstances = new ConcurrentHashMap<>();

        this.store.loadInstances().forEach(i -> newInstances.put(i.getURI(), i));

        this.instances = newInstances;
    }
}
