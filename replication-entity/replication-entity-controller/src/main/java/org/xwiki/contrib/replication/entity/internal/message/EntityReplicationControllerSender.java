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
package org.xwiki.contrib.replication.entity.internal.message;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.RelayReplicationSender;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.model.reference.EntityReference;

/**
 * Synchronize replication configuration between instances.
 * 
 * @version $Id$
 */
@Component(roles = EntityReplicationControllerSender.class)
@Singleton
public class EntityReplicationControllerSender
{
    @Inject
    private Provider<EntityReplicationControllerMessage> messageProvider;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private ReplicationSender sender;

    @Inject
    private RelayReplicationSender relay;

    /**
     * @param reference the reference of the configured entity
     * @param configuration the replication configuration of the entity
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to send replication configuration to other instances
     */
    public CompletableFuture<ReplicationSenderMessage> send(EntityReference reference,
        Collection<DocumentReplicationControllerInstance> configuration) throws ReplicationException
    {
        return send(reference, configuration, null);
    }

    /**
     * @param reference the reference of the configured entity
     * @param configuration the replication configuration of the entity
     * @param instances the instances to send the message to
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to send replication configuration to other instances
     * @since 1.1
     */
    public CompletableFuture<ReplicationSenderMessage> send(EntityReference reference,
        Collection<DocumentReplicationControllerInstance> configuration, Collection<ReplicationInstance> instances)
        throws ReplicationException
    {
        Collection<ReplicationInstance> targets = instances;
        if (targets == null) {
            // Send the configuration to everyone, each instance will decide what to do with it (including when they are
            // not or not anymore supposed to be part of the replication)
            // TODO: cut this in two different messaging for adding new instance and removing instances ?
            targets = this.instanceManager.getRegisteredInstances();
        }

        CompletableFuture<ReplicationSenderMessage> future;
        if (!targets.isEmpty()) {
            EntityReplicationControllerMessage message = this.messageProvider.get();

            message.initialize(reference, configuration);

            future = this.sender.send(message, targets);
        } else {
            // Nothing to send
            future = new CompletableFuture<>();
            future.complete(null);
        }

        return future;
    }

    /**
     * @param message the message to relay
     * @param configurations the configuration to send with the relayed message
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to send replication configuration to other instances
     */
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException
    {
        // Send the configuration to everyone, each instance will decide what to do with it (including when they are not
        // or not anymore supposed to be part of the replication)
        // TODO: cut this in two different messaging for adding new instance and removing instances ?
        Collection<ReplicationInstance> instances = this.instanceManager.getRegisteredInstances();

        CompletableFuture<ReplicationSenderMessage> future;
        if (!instances.isEmpty()) {
            // Change the configuration in the message
            Map<String, Collection<String>> metadata = new HashMap<>(message.getCustomMetadata());
            EntityReplicationControllerMessage.setConfiguration(metadata, configurations);

            // Send the new message
            future = this.relay.relay(message, metadata, instances);
        } else {
            // Nothing to send
            future = new CompletableFuture<>();
            future.complete(null);
        }

        return future;
    }
}
