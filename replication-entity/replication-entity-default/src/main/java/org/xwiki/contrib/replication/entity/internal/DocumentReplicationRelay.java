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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.RelayReplicationSender;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.DocumentReplicationMessageReader;
import org.xwiki.contrib.replication.entity.internal.reference.DocumentReferenceReplicationMessage;

/**
 * @version $Id$
 */
@Component(roles = DocumentReplicationRelay.class)
@Singleton
public class DocumentReplicationRelay
{
    @Inject
    private ReplicationSender sender;

    @Inject
    private RelayReplicationSender relay;

    @Inject
    private Provider<DocumentReferenceReplicationMessage> documentReferenceMessageProvider;

    @Inject
    private DocumentReplicationController controller;

    @Inject
    private DocumentReplicationMessageReader documentMessageTool;

    private List<ReplicationInstance> getInstances(DocumentReplicationLevel level,
        Collection<DocumentReplicationControllerInstance> configurations)
    {
        return configurations.stream().filter(c -> c.getLevel() == level)
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }

    private List<ReplicationInstance> getRelayInstances(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> instances = this.controller.getRelayConfiguration(message);

        return instances.stream().filter(i -> i.getLevel().ordinal() >= minimumLevel.ordinal())
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }

    /**
     * @param message the message to relay
     * @param minimumLevel the minimum level required to relay the message
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to queue the replication message
     */
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel) throws ReplicationException
    {
        // Find the instances allowed to receive the message
        List<ReplicationInstance> targets = getRelayInstances(message, minimumLevel);

        // Relay the message
        return this.relay.relay(message, targets);
    }

    /**
     * @param message the message to relay
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to queue the replication message
     */
    public CompletableFuture<ReplicationSenderMessage> relayDocumentUpdate(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> allInstances = this.controller.getRelayConfiguration(message);

        // Send the message as is for instances allowed to receive complete updates
        CompletableFuture<ReplicationSenderMessage> future =
            this.relay.relay(message, getInstances(DocumentReplicationLevel.ALL, allInstances));

        // Strip the message for instances allowed to receive only references
        if (this.documentMessageTool.isComplete(message)) {
            List<ReplicationInstance> referenceInstances =
                this.relay.getRelayedInstances(message, getInstances(DocumentReplicationLevel.REFERENCE, allInstances));

            if (!referenceInstances.isEmpty()) {
                DocumentReferenceReplicationMessage sendMessage = this.documentReferenceMessageProvider.get();
                sendMessage.initialize(message);

                future = this.sender.send(sendMessage, referenceInstances);
            }
        }

        return future;
    }
}
