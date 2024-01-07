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
package org.xwiki.contrib.replication.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.replication.RelayReplicationSender;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public abstract class AbstractDocumentReplicationController implements DocumentReplicationController
{
    @Inject
    protected ReplicationSender sender;

    @Inject
    protected EntityReplicationBuilders builders;

    @Inject
    protected ReplicationInstanceManager instanceManager;

    @Inject
    @Named("context")
    protected Provider<ComponentManager> componentManagerProvider;

    @Inject
    protected ComponentDescriptor<DocumentReplicationController> descriptor;

    @Inject
    protected DocumentReplicationMessageReader documentMessageReader;

    @Inject
    protected RelayReplicationSender relay;

    protected List<DocumentReplicationControllerInstance> getReplicationConfiguration(XWikiDocument document,
        Collection<String> receivers) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> configurations = getReplicationConfiguration(document);

        return getReplicationConfiguration(configurations, receivers);
    }

    protected List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference,
        Collection<String> receivers) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> configurations = getReplicationConfiguration(entityReference);

        return getReplicationConfiguration(configurations, receivers);
    }

    protected List<DocumentReplicationControllerInstance> getReplicationConfiguration(
        List<DocumentReplicationControllerInstance> configurations, Collection<String> receivers)
        throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> filteredConfigurations = configurations;

        // Optimizing a bit the replication configuration based on the receivers
        if (receivers != null) {
            // According to receivers the message should not be sent to any instance
            if (receivers.isEmpty()) {
                return Collections.emptyList();
            }

            // Check if all receivers are direct links
            for (String receiver : receivers) {
                ReplicationInstance instance = this.instanceManager.getRegisteredInstanceByURI(receiver);
                if (instance == null) {
                    // One of the receiver is unknown so we don't filter the replication configuration
                    return configurations;
                }
            }

            // Filter the replication configuration to keep only indicated receivers
            filteredConfigurations = new ArrayList<>(configurations.size());
            for (DocumentReplicationControllerInstance configuration : configurations) {
                if (receivers.contains(configuration.getInstance().getURI())) {
                    filteredConfigurations.add(configuration);
                }
            }
        }

        return filteredConfigurations;
    }

    @Override
    public DocumentReplicationControllerInstance getReceiveConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> configurations =
            getReplicationConfiguration(this.documentMessageReader.getDocumentReference(message));

        for (DocumentReplicationControllerInstance configuration : configurations) {
            if (configuration.getInstance() == message.getInstance()) {
                return configuration;
            }
        }

        return null;
    }

    /**
     * @param document the document associated with the metadata
     * @return the metadata associated with the document
     * @throws ReplicationException when failing to gather the metadata associated with the document
     */
    protected Map<String, Collection<String>> getMetadata(XWikiDocument document) throws ReplicationException
    {
        // No custom metadata by default
        return null;
    }

    /**
     * @param entity the entity associated with the metadata
     * @return the metadata associated with the entity
     * @throws ReplicationException when failing to gather the metadata associated with the entity
     */
    protected Map<String, Collection<String>> getMetadata(EntityReference entity) throws ReplicationException
    {
        // No custom metadata by default
        return null;
    }

    @Override
    public void onDocumentCreated(XWikiDocument document) throws ReplicationException
    {
        send(this.builders.documentCreateMessageBuilder(document));
    }

    @Override
    public void onDocumentUpdated(XWikiDocument document) throws ReplicationException
    {
        send(this.builders.documentPartialUpdateMessageBuilder(document));
    }

    @Override
    public void onDocumentDeleted(XWikiDocument document) throws ReplicationException
    {
        send(this.builders.documentDeleteMessageBuilder(document));
    }

    @Override
    public void onDocumentHistoryDelete(XWikiDocument document, String from, String to) throws ReplicationException
    {
        send(this.builders.documentHistoryMessageBuilder(document, from, to));
    }

    @Override
    public void send(EntityReplicationSenderMessageBuilder messageBuilder) throws ReplicationException
    {
        send(messageBuilder, null);
    }

    @Override
    public void send(EntityReplicationSenderMessageBuilder messageBuilder,
        Collection<DocumentReplicationControllerInstance> customConfigurations) throws ReplicationException
    {
        XWikiDocument document = messageBuilder instanceof DocumentReplicationSenderMessageBuilder
            ? ((DocumentReplicationSenderMessageBuilder) messageBuilder).getDocument() : null;
        EntityReference entityReference =
            document != null ? document.getDocumentReferenceWithLocale() : messageBuilder.getEntityReference();

        if (messageBuilder.getMinimumLevel() == null) {
            // If there is no minimum level we send it to all instances
            this.sender.send(messageBuilder.build(null, null, getMetadata(document)));
        } else {
            // Get the replication configurations
            Collection<DocumentReplicationControllerInstance> configurations = customConfigurations;
            if (configurations == null) {
                if (document != null) {
                    configurations = getReplicationConfiguration(document, messageBuilder.getReceivers());
                } else {
                    configurations = getReplicationConfiguration(entityReference, messageBuilder.getReceivers());
                }
            }

            // No replication configuration
            if (configurations == null || configurations.isEmpty()) {
                // No instance to send the message to
                return;
            }

            Map<String, Collection<String>> extraMetadata = getMetadata(document);

            if (messageBuilder.getMinimumLevel() == DocumentReplicationLevel.REFERENCE) {
                // If the minimum requirement is REFERENCE then, we don't really care about read only
                // The message to send to instances allowed to receive full document
                send(messageBuilder, DocumentReplicationLevel.ALL, null, extraMetadata, configurations);
            } else {
                // The message to send to instances allowed to receive full document and send it back
                send(messageBuilder, DocumentReplicationLevel.ALL, true, extraMetadata, configurations);

                // The message to send to instances allowed to receive full document but not to send it back
                send(messageBuilder, DocumentReplicationLevel.ALL, false, extraMetadata, configurations);
            }

            // The message to send to instances allowed to receive only the reference
            send(messageBuilder, DocumentReplicationLevel.REFERENCE, null, extraMetadata, configurations);
        }
    }

    private void send(EntityReplicationSenderMessageBuilder messageBuilder, DocumentReplicationLevel level,
        Boolean readonly, Map<String, Collection<String>> extraMetadata,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException
    {
        if (level.ordinal() < messageBuilder.getMinimumLevel().ordinal()) {
            // We don't want to send any message for this level of replication
            return;
        }

        List<ReplicationInstance> instances = getInstances(level, readonly, configurations);

        if (instances.isEmpty()) {
            // No instance to send the message to
            return;
        }

        this.sender.send(messageBuilder.build(level, readonly, extraMetadata), instances);
    }

    private List<ReplicationInstance> getInstances(DocumentReplicationLevel level, Boolean readonly,
        Collection<DocumentReplicationControllerInstance> configurations)
    {
        return configurations.stream()
            .filter(c -> c.getLevel() == level && c.getDirection() != DocumentReplicationDirection.RECEIVE_ONLY
                && (readonly == null || (readonly ? c.getDirection() == DocumentReplicationDirection.SEND_ONLY
                    : c.getDirection() == DocumentReplicationDirection.BOTH)))
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }

    @Override
    public void sendDocument(DocumentReference documentReference) throws ReplicationException
    {
        send(this.builders.documentMessageBuilder(documentReference));
    }

    @Override
    public void sendDocument(DocumentReference documentReference,
        Collection<DocumentReplicationControllerInstance> customConfigurations) throws ReplicationException
    {
        send(this.builders.documentMessageBuilder(documentReference), customConfigurations);
    }

    @Override
    public boolean receiveREFERENCEDocument(XWikiDocument document, ReplicationReceiverMessage message)
    {
        return false;
    }

    @Override
    public ReplicationReceiverMessage filter(ReplicationReceiverMessage message) throws ReplicationException
    {
        ComponentManager componentManager = this.componentManagerProvider.get();

        String filterHint = getFilterHint(message, componentManager);
        if (filterHint != null) {
            try {
                DocumentReplicationReceiverMessageFilter filter =
                    componentManager.getInstance(DocumentReplicationReceiverMessageFilter.class, filterHint);

                return filter.filter(message);
            } catch (ComponentLookupException e) {
                throw new ReplicationException(
                    "Failed to lookup the DocumentReplicationReceiverMessageFilter component", e);
            }
        }

        return message;
    }

    private String getFilterHint(ReplicationReceiverMessage message, ComponentManager componentManager)
    {
        // Try a filter specific to this controller and message type
        String hint = this.descriptor.getRoleHint() + '/' + message.getType();
        if (componentManager.hasComponent(DocumentReplicationReceiverMessageFilter.class, hint)) {
            return hint;
        }

        // Try a filter for any controller but specific to this message type
        if (componentManager.hasComponent(DocumentReplicationReceiverMessageFilter.class, message.getType())) {
            return message.getType();
        }

        return null;
    }

    private List<ReplicationInstance> getInstances(DocumentReplicationLevel level,
        Collection<DocumentReplicationControllerInstance> configurations)
    {
        return configurations.stream().filter(c -> c.getLevel() == level)
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }

    private List<ReplicationInstance> getRelayInstances(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> instances = getRelayConfiguration(message);

        return instances.stream().filter(i -> i.getLevel().ordinal() >= minimumLevel.ordinal())
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }

    private List<ReplicationInstance> getRelayInstances(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel, DocumentReplicationLevel maximumLevel) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> instances = getRelayConfiguration(message);

        return instances.stream().filter(
            i -> i.getLevel().ordinal() >= minimumLevel.ordinal() && i.getLevel().ordinal() <= maximumLevel.ordinal())
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel) throws ReplicationException
    {
        // Find the instances allowed to receive the message
        List<ReplicationInstance> targets = getRelayInstances(message, minimumLevel);

        // Relay the message
        return this.relay.relay(message, targets);
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel, DocumentReplicationLevel maximumLevel) throws ReplicationException
    {
        // Find the instances allowed to receive the message
        List<ReplicationInstance> targets = getRelayInstances(message, minimumLevel, maximumLevel);

        // Relay the message
        return this.relay.relay(message, targets);
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relayDocumentUpdate(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> allInstances = getRelayConfiguration(message);

        // Get instances allowed to receive updates
        List<ReplicationInstance> fullInstances = getInstances(DocumentReplicationLevel.ALL, allInstances);

        // Send the message as is for instances allowed to receive complete updates
        CompletableFuture<ReplicationSenderMessage> future = this.relay.relay(message, fullInstances);

        // Get instances only allowed to receive references
        List<ReplicationInstance> referenceInstances =
            this.relay.getRelayedInstances(message, getInstances(DocumentReplicationLevel.REFERENCE, allInstances));

        if (!referenceInstances.isEmpty()) {
            // Convert the message to a reference message and send it to instances not allowed to receive updates
            DocumentReplicationSenderMessageBuilder builder =
                this.builders.documentReferenceMessageBuilder(message);
            ReplicationSenderMessage sendMessage = builder.build(DocumentReplicationLevel.REFERENCE, true, null);

            future = this.sender.send(sendMessage, referenceInstances);
        }

        return future;
    }
}
