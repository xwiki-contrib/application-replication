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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.DocumentReplicationSender;
import org.xwiki.contrib.replication.entity.internal.create.DocumentCreateReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.delete.DocumentDeleteReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.history.DocumentHistoryDeleteReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.reference.DocumentReferenceReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.update.DocumentUpdateReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

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
    private Provider<DocumentHistoryDeleteReplicationMessage> historyMessageProvider;

    @Inject
    private Provider<DocumentReplicationController> controllerProvider;

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
    public void sendDocument(XWikiDocument document, boolean complete, boolean create,
        Map<String, Collection<String>> metadata, DocumentReplicationLevel minimumLevel,
        Collection<DocumentReplicationControllerInstance> inputConfigurations) throws ReplicationException
    {
        Collection<DocumentReplicationControllerInstance> configurations = inputConfigurations;
        if (configurations == null) {
            configurations = this.controllerProvider.get().getReplicationConfiguration(document.getDocumentReference());
        }

        // The message to send to instances allowed to receive full document
        sendDocument(document, complete, create, metadata, DocumentReplicationLevel.ALL, minimumLevel, configurations);

        // The message to send to instances allowed to receive only the reference
        sendDocument(document, complete, create, metadata, DocumentReplicationLevel.REFERENCE, minimumLevel,
            configurations);
    }

    private void sendDocument(XWikiDocument document, boolean complete, boolean create,
        Map<String, Collection<String>> metadata, DocumentReplicationLevel level, DocumentReplicationLevel minimumLevel,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException
    {
        if (level.ordinal() < minimumLevel.ordinal()) {
            // We don't want to send any message for this level of replication
            return;
        }

        List<ReplicationInstance> instances = getInstances(level, configurations);

        if (instances.isEmpty()) {
            // No instance to send the message to
            return;
        }

        ReplicationSenderMessage message;

        if (create) {
            // TODO: register as owner of the document
        }

        if (level == DocumentReplicationLevel.REFERENCE) {
            // Sending a document place holder
            message = this.documentReferenceMessageProvider.get();

            ((DocumentReferenceReplicationMessage) message).initialize(document.getDocumentReferenceWithLocale(),
                document.getAuthors().getCreator(), create, metadata);
        } else if (create) {
            // Sending the creation of a new fulldocument
            message = this.documentCreateMessageProvider.get();

            ((DocumentCreateReplicationMessage) message).initializeComplete(document.getDocumentReferenceWithLocale(),
                document.getAuthors().getCreator(), document.getVersion(), metadata);
        } else {
            // Sending the update of a document
            message = this.documentUpdateMessageProvider.get();

            if (complete) {
                ((DocumentUpdateReplicationMessage) message).initializeComplete(
                    document.getDocumentReferenceWithLocale(), document.getAuthors().getCreator(),
                    document.getVersion(), metadata);
            } else {
                ((DocumentUpdateReplicationMessage) message).initializeUpdate(document,
                    getModifiedAttachments(document), metadata);
            }
        }

        this.sender.send(message, instances);
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
        Collection<DocumentReplicationControllerInstance> inputConfigurations) throws ReplicationException
    {
        Collection<DocumentReplicationControllerInstance> configurations = inputConfigurations;
        if (configurations == null) {
            configurations = this.controllerProvider.get().getReplicationConfiguration(documentReference);
        }

        List<ReplicationInstance> instances = getInstancesFrom(DocumentReplicationLevel.REFERENCE, configurations);

        DocumentDeleteReplicationMessage message = this.documentDeleteMessageProvider.get();

        message.initialize(documentReference, metadata);

        this.sender.send(message, instances);
    }

    @Override
    public void sendDocumentHistoryDelete(DocumentReference documentReference, String from, String to,
        Map<String, Collection<String>> metadata, Collection<DocumentReplicationControllerInstance> inputConfigurations)
        throws ReplicationException
    {
        Collection<DocumentReplicationControllerInstance> configurations = inputConfigurations;
        if (configurations == null) {
            configurations = this.controllerProvider.get().getReplicationConfiguration(documentReference);
        }

        // Sending history update only make sense to instance allowed to contains complete documents
        List<ReplicationInstance> instances = getInstancesFrom(DocumentReplicationLevel.ALL, configurations);

        if (!CollectionUtils.isEmpty(instances)) {
            DocumentHistoryDeleteReplicationMessage message = this.historyMessageProvider.get();
            message.initialize(documentReference, from, to, metadata);

            this.sender.send(message, instances);
        }
    }

    private List<ReplicationInstance> getInstances(DocumentReplicationLevel level,
        Collection<DocumentReplicationControllerInstance> configurations)
    {
        return configurations.stream().filter(c -> c.getLevel() == level)
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }

    private List<ReplicationInstance> getInstancesFrom(DocumentReplicationLevel minimumLevel,
        Collection<DocumentReplicationControllerInstance> configurations)
    {
        return configurations.stream().filter(c -> c.getLevel().ordinal() >= minimumLevel.ordinal())
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }
}
