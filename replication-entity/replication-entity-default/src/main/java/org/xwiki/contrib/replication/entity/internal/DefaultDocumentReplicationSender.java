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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.DocumentReplicationSender;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.contrib.replication.entity.EntityReplicationSenderMessageProducer;
import org.xwiki.contrib.replication.entity.ReplicationSenderMessageProducer;
import org.xwiki.contrib.replication.entity.internal.create.DocumentCreateReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.delete.DocumentDeleteReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.history.DocumentHistoryDeleteReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
import org.xwiki.contrib.replication.entity.internal.reference.DocumentReferenceReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.repair.DocumentRepairReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.unreplicate.DocumentUnreplicateReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.update.DocumentUpdateReplicationMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultDocumentReplicationSender implements DocumentReplicationSender
{
    @Inject
    private ReplicationSender sender;

    @Inject
    private Provider<DocumentReferenceReplicationMessage> documentReferenceMessageProvider;

    @Inject
    private Provider<DocumentUpdateReplicationMessage> documentUpdateMessageProvider;

    @Inject
    private Provider<DocumentCreateReplicationMessage> documentCreateMessageProvider;

    @Inject
    private Provider<DocumentDeleteReplicationMessage> documentDeleteMessageProvider;

    @Inject
    private Provider<DocumentUnreplicateReplicationMessage> documentUnreplicateMessageProvider;

    @Inject
    private Provider<DocumentHistoryDeleteReplicationMessage> historyMessageProvider;

    @Inject
    private Provider<DocumentReplicationController> controllerProvider;

    @Inject
    private Provider<DocumentRepairReplicationMessage> repairMessageProvider;

    @Inject
    private ReplicationDocumentStore documentStore;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private EntityReplication entityReplication;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void sendDocument(DocumentReference documentReference, boolean complete, boolean create,
        Map<String, Collection<String>> metadata, DocumentReplicationLevel minimumLevel,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(documentReference, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to get the document content", e);
        }

        sendDocument(document, complete, create, metadata, minimumLevel, configurations);
    }

    @Override
    public void sendDocumentRepair(XWikiDocument document, Collection<String> authors,
        Map<String, Collection<String>> metadata, Collection<DocumentReplicationControllerInstance> configurations)
        throws ReplicationException
    {
        sendDocument((id, level, readonly, m) -> {
            DocumentRepairReplicationMessage message = this.repairMessageProvider.get();

            message.initializeRepair(id, document, readonly, authors, null, m);

            return message;
        }, document.getDocumentReference(), DocumentReplicationLevel.ALL, null, metadata, configurations);
    }

    @Override
    public void sendDocument(EntityReplicationSenderMessageProducer messageProducer, EntityReference documentReference,
        DocumentReplicationLevel minimumLevel, Collection<String> receivers, Map<String, Collection<String>> metadata,
        Collection<DocumentReplicationControllerInstance> inputConfigurations) throws ReplicationException
    {
        Collection<DocumentReplicationControllerInstance> configurations = inputConfigurations;
        if (configurations == null) {
            configurations = this.controllerProvider.get().getReplicationConfiguration(documentReference, receivers);
        }

        // No replication configuration
        if (configurations == null || configurations.isEmpty()) {
            // No instance to send the message to
            return;
        }

        // Use the same ids for all the possible variations of that message so that an instance can only receive one no
        // matter the taken routes
        String id = UUID.randomUUID().toString();

        // The message to send to instances allowed to receive full document and send it back
        sendDocument(messageProducer, id, DocumentReplicationLevel.ALL, true, minimumLevel, metadata, configurations);

        // The message to send to instances allowed to receive full document but not to send it back
        sendDocument(messageProducer, id, DocumentReplicationLevel.ALL, false, minimumLevel, metadata, configurations);

        // The message to send to instances allowed to receive only the reference
        sendDocument(messageProducer, id, DocumentReplicationLevel.REFERENCE, true, minimumLevel, metadata,
            configurations);
    }

    private void sendDocument(EntityReplicationSenderMessageProducer messageProducer, String id,
        DocumentReplicationLevel level, Boolean readonly, DocumentReplicationLevel minimumLevel,
        Map<String, Collection<String>> metadata, Collection<DocumentReplicationControllerInstance> configurations)
        throws ReplicationException
    {
        if (level.ordinal() < minimumLevel.ordinal()) {
            // We don't want to send any message for this level of replication
            return;
        }

        List<ReplicationInstance> instances = getInstances(level, readonly, configurations);

        if (instances.isEmpty()) {
            // No instance to send the message to
            return;
        }

        this.sender.send(messageProducer.produce(id, level, readonly, metadata), instances);
    }

    @Override
    public void sendDocument(XWikiDocument document, boolean complete, boolean create,
        Map<String, Collection<String>> metadata, DocumentReplicationLevel minimumLevel,
        Collection<DocumentReplicationControllerInstance> inputConfigurations) throws ReplicationException
    {
        sendDocument(document, complete, create, null, metadata, minimumLevel, inputConfigurations);
    }

    private void sendDocument(XWikiDocument document, boolean complete, boolean create, Collection<String> receivers,
        Map<String, Collection<String>> metadata, DocumentReplicationLevel minimumLevel,
        Collection<DocumentReplicationControllerInstance> inputConfigurations) throws ReplicationException
    {
        Collection<DocumentReplicationControllerInstance> configurations = inputConfigurations;
        if (configurations == null) {
            configurations =
                this.controllerProvider.get().getReplicationConfiguration(document.getDocumentReference(), receivers);
        }

        // No replication configuration
        if (configurations == null || configurations.isEmpty()) {
            // No instance to send the message to
            return;
        }

        sendDocument((id, level, readonly, m) -> {
            ReplicationSenderMessage message;

            if (create && document.getLocale().equals(Locale.ROOT)) {
                // Register as owner of the document the instance which created that document
                // But only if there is not already a owner (might be a create after a delete)
                if (this.documentStore.getOwner(document.getDocumentReference()) == null) {
                    this.documentStore.setOwner(document.getDocumentReference(),
                        this.instanceManager.getCurrentInstance().getURI());
                }
            }

            if (level == DocumentReplicationLevel.REFERENCE) {
                // Sending a document place holder
                message = this.documentReferenceMessageProvider.get();

                ((DocumentReferenceReplicationMessage) message).initialize(id,
                    document.getDocumentReferenceWithLocale(), document.getAuthors().getCreator(), receivers, m);
            } else if (create) {
                // Sending the creation of a new fulldocument
                message = this.documentCreateMessageProvider.get();

                ((DocumentCreateReplicationMessage) message).initializeComplete(id, document, readonly, receivers, m);
            } else {
                // Sending the update of a document
                message = this.documentUpdateMessageProvider.get();

                if (complete) {
                    ((DocumentUpdateReplicationMessage) message).initializeComplete(id, document, readonly, receivers,
                        m);
                } else {
                    ((DocumentUpdateReplicationMessage) message).initializeUpdate(id, document, readonly,
                        getModifiedAttachments(document), receivers, m);
                }
            }

            return message;
        }, document.getDocumentReference(), minimumLevel, receivers, metadata, configurations);
    }

    @Override
    public void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel, Map<String, Collection<String>> metadata,
        Collection<DocumentReplicationControllerInstance> inputConfigurations) throws ReplicationException
    {
        send(messageProducer, entityReference, minimumLevel, null, metadata, inputConfigurations);
    }

    @Override
    public void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel, Collection<String> receivers, Map<String, Collection<String>> metadata,
        Collection<DocumentReplicationControllerInstance> inputConfigurations) throws ReplicationException
    {
        sendDocument((id, level, readonly, m) -> messageProducer.produce(m), entityReference, minimumLevel, receivers,
            metadata, inputConfigurations);
    }

    private Set<String> getModifiedAttachments(XWikiDocument document)
    {
        Set<String> attachments = null;

        // Find out which attachments were modified
        XWikiDocument originalDocument = document.getOriginalDocument();
        for (XWikiAttachment attachment : document.getAttachmentList()) {
            // Check if the attachment has been updated
            if (originalDocument != null) {
                XWikiAttachment originalAttachment = originalDocument.getAttachment(attachment.getFilename());

                if (originalAttachment != null && originalAttachment.getVersion().equals(attachment.getVersion())) {
                    // TODO: compare also the actual content ?
                    continue;
                }
            }

            // The attachment is different
            if (attachments == null) {
                attachments = new HashSet<>(document.getAttachmentList().size());
            }
            attachments.add(attachment.getFilename());
        }

        return attachments;
    }

    @Override
    public void sendDocumentDelete(DocumentReference documentReference, Map<String, Collection<String>> metadata,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException
    {
        sendDocumentDelete(documentReference, null, metadata, configurations);
    }

    /**
     * @param documentReference the reference of the document to delete
     * @param receivers the instances which are supposed to handler the message
     * @param metadata custom metadata to add to the message
     * @param configurations the replication configuration to follow or null if it should be asked to the controller
     * @throws ReplicationException when failing to send the document delete
     */
    private void sendDocumentDelete(DocumentReference documentReference, Collection<String> receivers,
        Map<String, Collection<String>> metadata, Collection<DocumentReplicationControllerInstance> configurations)
        throws ReplicationException
    {
        send(m -> {
            DocumentDeleteReplicationMessage message = this.documentDeleteMessageProvider.get();

            message.initialize(documentReference, receivers, m);

            return message;
        }, documentReference, DocumentReplicationLevel.REFERENCE, receivers, metadata, configurations);
    }

    @Override
    public void sendDocumentUnreplicate(DocumentReference documentReference, Map<String, Collection<String>> metadata,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException
    {
        send(m -> {
            DocumentUnreplicateReplicationMessage message = this.documentUnreplicateMessageProvider.get();

            message.initialize(documentReference, null, m);

            return message;
        }, documentReference, DocumentReplicationLevel.REFERENCE, metadata, configurations);
    }

    @Override
    public void sendDocumentHistoryDelete(DocumentReference documentReference, String from, String to,
        Map<String, Collection<String>> metadata, Collection<DocumentReplicationControllerInstance> configurations)
        throws ReplicationException
    {
        send(m -> {
            DocumentHistoryDeleteReplicationMessage message = this.historyMessageProvider.get();

            message.initialize(documentReference, from, to, m);

            return message;
        }, documentReference, DocumentReplicationLevel.ALL, metadata, configurations);
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
    public void replicateDocument(DocumentReference documentReference, Collection<String> receivers,
        Map<String, Collection<String>> metadata, Collection<DocumentReplicationControllerInstance> inputConfigurations)
        throws ReplicationException
    {
        Collection<DocumentReplicationControllerInstance> configurations = inputConfigurations;
        if (configurations == null) {
            configurations = this.controllerProvider.get().getReplicationConfiguration(documentReference, receivers);
        }

        // No replication configuration
        if (configurations == null || configurations.isEmpty()) {
            // No instance to send the message to
            return;
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(documentReference, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to get the document content", e);
        }

        if (document.isNew()) {
            sendDocumentDelete(documentReference, receivers, metadata, configurations);
        } else {
            // Check if current instance is the owner of the document
            ReplicationInstance currentInstance = this.instanceManager.getCurrentInstance();
            String ownerURI = this.entityReplication.getOwner(documentReference);

            sendDocument(document, true, currentInstance.getURI().equals(ownerURI), receivers, metadata,
                DocumentReplicationLevel.REFERENCE, configurations);
        }
    }
}
