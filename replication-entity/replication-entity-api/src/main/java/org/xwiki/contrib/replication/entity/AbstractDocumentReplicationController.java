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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public abstract class AbstractDocumentReplicationController implements DocumentReplicationController
{
    @Inject
    protected DocumentReplicationSender sender;

    @Inject
    protected ReplicationInstanceManager instanceManager;

    @Inject
    @Named("context")
    protected Provider<ComponentManager> componentManagerProvider;

    @Inject
    protected ComponentDescriptor<DocumentReplicationController> descriptor;

    @Inject
    protected DocumentReplicationMessageReader messageReader;

    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference,
        Collection<String> receivers) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> configurations = getReplicationConfiguration(entityReference);

        // Optimizing a bit the replication configuration based on the receivers
        if (receivers != null) {
            // According to receivers the message should not be sent to any instance
            if (receivers.isEmpty()) {
                return Collections.emptyList();
            }

            // Check if all receivers are direct links
            for (String receiver : receivers) {
                ReplicationInstance instance = this.instanceManager.getInstanceByURI(receiver);
                if (instance == null) {
                    // One of the receiver is unknown so we don't filter the replication configuration
                    return configurations;
                }
            }

            // Filter the replication configuration to keep only indicated receivers
            List<DocumentReplicationControllerInstance> filteredConfigurations = new ArrayList<>(configurations.size());
            for (DocumentReplicationControllerInstance configuration : configurations) {
                if (receivers.contains(configuration.getInstance().getURI())) {
                    filteredConfigurations.add(configuration);
                }
            }

            configurations = filteredConfigurations;
        }

        return configurations;
    }

    @Override
    public DocumentReplicationControllerInstance getReceiveConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> configurations =
            getReplicationConfiguration(this.messageReader.getDocumentReference(message));

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
        this.sender.sendDocument(document, true, true, getMetadata(document), DocumentReplicationLevel.REFERENCE, null);
    }

    @Override
    public void onDocumentUpdated(XWikiDocument document) throws ReplicationException
    {
        // There is no point in sending a message for each update if the instance is only allowed to
        // replicate the reference
        this.sender.sendDocument(document, false, false, getMetadata(document), DocumentReplicationLevel.ALL, null);
    }

    @Override
    public void onDocumentDeleted(XWikiDocument document) throws ReplicationException
    {
        this.sender.sendDocumentDelete(document.getDocumentReferenceWithLocale(), getMetadata(document), null);
    }

    @Override
    public void onDocumentHistoryDelete(XWikiDocument document, String from, String to) throws ReplicationException
    {
        this.sender.sendDocumentHistoryDelete(document.getDocumentReferenceWithLocale(), from, to,
            getMetadata(document), null);
    }

    @Override
    public void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel) throws ReplicationException
    {
        this.sender.send(messageProducer, entityReference, minimumLevel, getMetadata(entityReference), null);
    }

    @Override
    public void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel, Collection<String> receivers) throws ReplicationException
    {
        this.sender.send(messageProducer, entityReference, minimumLevel, receivers, getMetadata(entityReference), null);
    }

    @Override
    public void replicateDocument(DocumentReference documentReference, Collection<String> receivers)
        throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> configurations =
            getReplicationConfiguration(documentReference, receivers);

        if (!configurations.isEmpty()) {
            this.sender.replicateDocument(documentReference, receivers, getMetadata(documentReference), configurations);
        }
    }

    @Override
    public void sendCompleteDocument(XWikiDocument document) throws ReplicationException
    {
        this.sender.sendDocument(document, true, false, getMetadata(document), DocumentReplicationLevel.REFERENCE,
            null);
    }

    @Override
    public void sendDocumentRepair(XWikiDocument document, Collection<String> authors) throws ReplicationException
    {
        this.sender.sendDocumentRepair(document, authors, getMetadata(document), null);
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
}
