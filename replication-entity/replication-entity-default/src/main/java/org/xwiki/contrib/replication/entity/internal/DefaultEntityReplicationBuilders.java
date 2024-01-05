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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.DocumentReplicationMessageReader;
import org.xwiki.contrib.replication.entity.DocumentReplicationSenderMessageBuilder;
import org.xwiki.contrib.replication.entity.EntityReplicationBuilders;
import org.xwiki.contrib.replication.entity.EntityReplicationSenderMessageBuilder;
import org.xwiki.contrib.replication.entity.EntityReplicationSenderMessageBuilderProducer;
import org.xwiki.contrib.replication.entity.internal.conflict.DocumentReplicationConflictMessage;
import org.xwiki.contrib.replication.entity.internal.delete.DocumentDeleteReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.history.DocumentHistoryDeleteReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
import org.xwiki.contrib.replication.entity.internal.reference.DocumentReferenceReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.repair.DocumentRepairRequestReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.unreplicate.DocumentUnreplicateReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.update.DocumentUpdateReplicationMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link EntityReplicationBuilders}.
 * 
 * @version $Id$
 * @since 2.0.0
 */
@Component
@Singleton
public class DefaultEntityReplicationBuilders implements EntityReplicationBuilders
{
    @Inject
    private Provider<DocumentReferenceReplicationMessage> documentReferenceMessageProvider;

    @Inject
    private Provider<DocumentUpdateReplicationMessage> documentUpdateMessageProvider;

    @Inject
    private Provider<DocumentDeleteReplicationMessage> documentDeleteMessageProvider;

    @Inject
    private Provider<DocumentUnreplicateReplicationMessage> documentUnreplicateMessageProvider;

    @Inject
    private Provider<DocumentHistoryDeleteReplicationMessage> historyMessageProvider;

    @Inject
    private Provider<DocumentReplicationConflictMessage> conflictMessageProvider;

    @Inject
    private Provider<DocumentRepairRequestReplicationMessage> repairRequestMessageProvider;

    @Inject
    private ReplicationDocumentStore documentStore;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private DocumentReplicationMessageReader documentMessageReader;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

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
    public DocumentReplicationSenderMessageBuilder documentMessageBuilder(
        EntityReplicationSenderMessageBuilderProducer<DocumentReplicationSenderMessageBuilder> messageProducer,
        DocumentReference document) throws ReplicationException
    {
        return new ProducerDocumentReplicationSenderMessageBuilder(messageProducer, document);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentMessageBuilder(
        EntityReplicationSenderMessageBuilderProducer<DocumentReplicationSenderMessageBuilder> messageProducer,
        XWikiDocument document) throws ReplicationException
    {
        return new ProducerDocumentReplicationSenderMessageBuilder(messageProducer, document);
    }

    @Override
    public EntityReplicationSenderMessageBuilder entityMessageBuilder(
        EntityReplicationSenderMessageBuilderProducer<EntityReplicationSenderMessageBuilder> messageProducer,
        EntityReference document) throws ReplicationException
    {
        return new ProducerEntityReplicationSenderMessageBuilder(messageProducer, document);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentReferenceMessageBuilder(XWikiDocument document)
        throws ReplicationException
    {
        return documentMessageBuilder(
            (builder, level, readonly, extraMetadata) -> referenceMessage(builder, document, extraMetadata), document)
                .minimumLevel(DocumentReplicationLevel.REFERENCE);
    }

    private DocumentReferenceReplicationMessage referenceMessage(DocumentReplicationSenderMessageBuilder builder,
        XWikiDocument document, Map<String, Collection<String>> extraMetadata)
    {
        DocumentReferenceReplicationMessage message = this.documentReferenceMessageProvider.get();

        message.initialize(builder, document.getAuthors().getCreator(), extraMetadata);

        return message;
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentReferenceMessageBuilder(ReplicationMessage message)
        throws ReplicationException
    {
        // Extract the reference of the document from the message
        DocumentReference documentReference = this.documentMessageReader.getDocumentReference(message);
        // Extract the creator of the document from the message
        UserReference creator = this.documentMessageReader.getCreatorReference(message);

        DocumentReplicationSenderMessageBuilder referenceBuilder = documentMessageBuilder(
            (builder, level, readonly, extraMetadata) -> referenceMessage(builder, creator, extraMetadata),
            documentReference).minimumLevel(DocumentReplicationLevel.REFERENCE);

        // Copy the source message plumbing
        referenceBuilder.message(message);

        return referenceBuilder;
    }

    private DocumentReferenceReplicationMessage referenceMessage(DocumentReplicationSenderMessageBuilder builder,
        UserReference creator, Map<String, Collection<String>> extraMetadata)
    {
        DocumentReferenceReplicationMessage message = this.documentReferenceMessageProvider.get();

        message.initialize(builder, creator, extraMetadata);

        return message;
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentPartialUpdateMessageBuilder(XWikiDocument document)
        throws ReplicationException
    {
        return documentMessageBuilder((builder, level, readonly, extraMetadata) -> {
            // Sending the update of a document
            ReplicationSenderMessage message = this.documentUpdateMessageProvider.get();

            ((DocumentUpdateReplicationMessage) message).initializePartial(builder, document, readonly,
                getModifiedAttachments(document), extraMetadata);

            return message;
        }, document).minimumLevel(DocumentReplicationLevel.ALL);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentCompleteUpdateMessageBuilder(XWikiDocument document)
        throws ReplicationException
    {
        return documentMessageBuilder((builder, level, readonly, extraMetadata) -> {
            ReplicationSenderMessage message;

            if (document.getLocale().equals(Locale.ROOT) && builder.getOwner() != null) {
                // Register as owner of the document the instance which created that document
                // But only if there is not already a owner (might be a create after a delete)
                if (this.documentStore.getOwner(document.getDocumentReference()) == null) {
                    this.documentStore.setOwner(document.getDocumentReference(), builder.getOwner());
                }
            }

            if (level == DocumentReplicationLevel.REFERENCE) {
                // Sending a document place holder
                message = referenceMessage(builder, document, extraMetadata);
            } else {
                message = this.documentUpdateMessageProvider.get();

                ((DocumentUpdateReplicationMessage) message).initializeComplete(builder, document, readonly,
                    extraMetadata);
            }

            return message;
        }, document).minimumLevel(DocumentReplicationLevel.REFERENCE);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentCreateMessageBuilder(XWikiDocument document)
        throws ReplicationException
    {
        return documentCompleteUpdateMessageBuilder(document).owner(this.instanceManager.getCurrentInstance().getURI());
    }

    private EntityReplicationSenderMessageBuilderProducer<DocumentReplicationSenderMessageBuilder> deleteProvider()
    {
        return (builder, level, readonly, extraMetadata) -> {
            DocumentDeleteReplicationMessage message = this.documentDeleteMessageProvider.get();

            message.initialize(builder, extraMetadata);

            return message;
        };
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentDeleteMessageBuilder(DocumentReference document)
        throws ReplicationException
    {
        return documentMessageBuilder(deleteProvider(), document).minimumLevel(DocumentReplicationLevel.REFERENCE);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentDeleteMessageBuilder(XWikiDocument document)
        throws ReplicationException
    {
        return documentMessageBuilder(deleteProvider(), document).minimumLevel(DocumentReplicationLevel.REFERENCE);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentUnreplicateMessageBuilder(XWikiDocument document)
        throws ReplicationException
    {
        return documentMessageBuilder((builder, level, readonly, extraMetadata) -> {
            DocumentUnreplicateReplicationMessage message = this.documentUnreplicateMessageProvider.get();

            message.initialize(builder, extraMetadata);

            return message;
        }, document).minimumLevel(DocumentReplicationLevel.REFERENCE);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentHistoryMessageBuilder(XWikiDocument document, String from,
        String to) throws ReplicationException
    {
        return documentMessageBuilder((builder, level, readonly, extraMetadata) -> {
            DocumentHistoryDeleteReplicationMessage message = this.historyMessageProvider.get();

            message.initialize(builder, from, to, extraMetadata);

            return message;
        }, document).minimumLevel(DocumentReplicationLevel.ALL);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentConflictUpdateMessageBuilder(
        DocumentReference documentReference) throws ReplicationException
    {
        return this.documentMessageBuilder((builder, level, readonly, extraMetadata) -> {
            DocumentReplicationConflictMessage message = this.conflictMessageProvider.get();

            message.initialize(builder, extraMetadata);

            return message;
        }, documentReference).minimumLevel(DocumentReplicationLevel.ALL);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentRepairRequestMessageBuilder(
        DocumentReference documentReference, boolean sourceOnly) throws ReplicationException
    {
        return this.documentMessageBuilder((builder, level, readonly, extraMetadata) -> {
            DocumentRepairRequestReplicationMessage message = this.repairRequestMessageProvider.get();

            message.initialize(builder, sourceOnly, extraMetadata);

            return message;
        }, documentReference).minimumLevel(null);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder documentMessageBuilder(DocumentReference documentReference)
        throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Load the document
        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(documentReference, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to load the document to send back", e);
        }

        // Trigger the right message types depending on the document status
        DocumentReplicationSenderMessageBuilder builder;
        if (document.isNew()) {
            // The document does not exist so send a DELETE message
            builder = documentDeleteMessageBuilder(documentReference);
        } else {
            // As a regular update
            builder = documentCompleteUpdateMessageBuilder(document);
        }

        return builder;
    }
}
