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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.internal.ReplicationClient;
import org.xwiki.contrib.replication.internal.message.ReplicationInstanceMessageSender;

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
    private Provider<ReplicationInstanceMessageSender> senderProvider;

    @Inject
    private Logger logger;

    private Map<String, ReplicationInstance> instancesByURI;

    private Map<String, ReplicationInstance> instancesByName;

    @Override
    public ReplicationInstance getCurrentInstance() throws ReplicationException
    {
        return store.getCurrentInstance();
    }

    private Map<String, ReplicationInstance> getInternalInstancesByURI() throws ReplicationException
    {
        if (this.instancesByURI == null) {
            reload();
        }

        return this.instancesByURI;
    }

    private Map<String, ReplicationInstance> getInternalInstancesByName() throws ReplicationException
    {
        if (this.instancesByName == null) {
            reload();
        }

        return this.instancesByName;
    }

    @Override
    public Collection<ReplicationInstance> getInstances() throws ReplicationException
    {
        return getInternalInstancesByURI().values();
    }

    @Override
    public ReplicationInstance getInstanceByURI(String uri) throws ReplicationException
    {
        if (uri == null) {
            return null;
        }

        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstancesByURI().get(cleanURI);

        if (instance == null && getCurrentInstance().getURI().equals(cleanURI)) {
            instance = getCurrentInstance();
        }

        return instance;
    }

    @Override
    public ReplicationInstance getInstanceByName(String name) throws ReplicationException
    {
        if (name == null) {
            return null;
        }

        ReplicationInstance instance = getInternalInstancesByName().get(name);

        if (instance == null && getCurrentInstance().getName().equals(name)) {
            instance = getCurrentInstance();
        }

        return instance;
    }

    @Override
    public ReplicationInstance getInstanceByProperty(String key, Object value) throws ReplicationException
    {
        if (key == null) {
            return null;
        }

        for (ReplicationInstance instance : getInstances()) {
            if (Objects.equals(instance.getProperties().get(key), value)) {
                return instance;
            }
        }

        return null;
    }

    @Override
    public boolean addInstance(ReplicationInstance instance) throws ReplicationException
    {
        if (getInternalInstancesByURI().containsKey(instance.getURI())) {
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

        ReplicationInstance instance = getInternalInstancesByURI().get(cleanURI);

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
        return getInternalInstancesByURI().values().stream().filter(i -> i.getStatus() == Status.REGISTERED)
            .collect(Collectors.toList());
    }

    @Override
    public void requestInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        // Send a request to the target instance
        try {
            this.client.register(cleanURI);
        } catch (Exception e) {
            throw new ReplicationException("Failed to register the instance on [" + cleanURI + "]", e);
        }

        // Create the new instance
        ReplicationInstance instance = new DefaultReplicationInstance(null, cleanURI, Status.REQUESTED, null, null);

        // Add instance to the store
        this.store.addInstance(instance);
    }

    @Override
    public boolean removeRegisteredInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstancesByURI().get(cleanURI);

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
        return getInternalInstancesByURI().values().stream().filter(i -> i.getStatus() == Status.REQUESTED)
            .collect(Collectors.toList());
    }

    @Override
    public boolean cancelRequestedInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstancesByURI().get(cleanURI);

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
        ReplicationInstance existingInstance = getInternalInstancesByURI().get(newInstance.getURI());

        if (existingInstance == null || existingInstance.getStatus() != Status.REQUESTED) {
            return false;
        }

        // Update
        this.store.updateInstance(newInstance);

        // Send details about the current instance to the new linked one
        // TODO: send custom properties as part of the ACCESS request along with the name ?
        this.senderProvider.get().updateCurrentInstance(newInstance);

        return true;
    }

    @Override
    public Collection<ReplicationInstance> getRequestingInstances() throws ReplicationException
    {
        return getInternalInstancesByURI().values().stream().filter(i -> i.getStatus() == Status.REQUESTING)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<ReplicationInstance> getRelayedInstances() throws ReplicationException
    {
        return getInternalInstancesByURI().values().stream().filter(i -> i.getStatus() == Status.RELAYED)
            .collect(Collectors.toList());
    }

    @Override
    public boolean acceptRequestingInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstancesByURI().get(cleanURI);

        if (instance == null || instance.getStatus() != Status.REQUESTING) {
            return false;
        }

        // Notify the instance of the acceptance
        Status status;
        try {
            status = this.client.accept(instance);
        } catch (Exception e) {
            throw new ReplicationException("Failed to send a request to instance [" + cleanURI + "]", e);
        }

        if (status == Status.REGISTERED) {
            // Update the instance status
            this.store.updateStatus(instance, status);

            // Send details about the current instance to the new linked one
            // TODO: send custom properties as part of the ACCESS request along with the name ?
            this.senderProvider.get().updateCurrentInstance(instance);

            // The instances are now linked
            return true;
        } else if (status == Status.REQUESTED) {
            // Convert the REQUESTING instance into a REQUESTED one
            this.store.updateInstance(new DefaultReplicationInstance(instance.getURI(), instance.getName(), status,
                null, instance.getProperties()));
        }

        return false;
    }

    @Override
    public boolean declineRequestingInstance(String uri) throws ReplicationException
    {
        String cleanURI = DefaultReplicationInstance.cleanURI(uri);

        ReplicationInstance instance = getInternalInstancesByURI().get(cleanURI);

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
        ReplicationInstance instance = getInternalInstancesByURI().get(uri);

        if (instance == null || instance.getStatus() != Status.REQUESTING) {
            return null;
        }

        // Remove instance from the store/cache
        removeInstanceInternal(instance);

        return instance;
    }

    @Override
    public void saveCurrentInstance(String name, String uri) throws ReplicationException
    {
        this.store.saveCurrentInstance(name, uri);
    }

    @Override
    public void reload() throws ReplicationException
    {
        Map<String, ReplicationInstance> newInstancesByURI = new HashMap<>();
        this.store.loadInstances().forEach(i -> newInstancesByURI.put(i.getURI(), i));

        Map<String, ReplicationInstance> newInstancesByName = new HashMap<>();
        this.store.loadInstances().forEach(i -> newInstancesByName.put(i.getName(), i));

        this.instancesByURI = Collections.unmodifiableMap(newInstancesByURI);
        this.instancesByName = Collections.unmodifiableMap(newInstancesByName);
    }

    @Override
    public void resetSendKey(String uri) throws ReplicationException
    {
        try {
            this.client.resetSendKey(getInstanceByURI(uri));
        } catch (Exception e) {
            throw new ReplicationException("Failed to reset the key for instance with URI [" + uri + "]", e);
        }
    }
}
