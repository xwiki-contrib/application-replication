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

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance.Level;
import org.xwiki.contrib.replication.entity.internal.delete.DocumentDeleteReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.history.DocumentHistoryDeleteReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.update.DocumentUpdateReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = DocumentReplicationSender.class)
@Singleton
public class DocumentReplicationSender
{
    @Inject
    private ReplicationSender sender;

    @Inject
    private Provider<DocumentUpdateReplicationMessage> documentMessageProvider;

    @Inject
    private Provider<DocumentDeleteReplicationMessage> documentDeleteMessageProvider;

    @Inject
    private Provider<DocumentHistoryDeleteReplicationMessage> historyMessageProvider;

    @Inject
    private DocumentReplicationController controller;

    /**
     * @param document the document to send
     * @param complete true of the complete document should be send (including history and all attachments)
     * @param minimumLevel the minimum that need to be replicated from the document
     * @throws ReplicationException when failing to queue the replication message
     */
    public void sendDocument(XWikiDocument document, boolean complete, Level minimumLevel) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> instances =
            this.controller.getTargetInstances(document.getDocumentReference());

        // The message to send to instances allowed to receive full document
        sendDocument(document, complete, Level.ALL, minimumLevel, instances);

        // The message to send to instances allowed to receive only the reference
        sendDocument(document, complete, Level.REFERENCE, minimumLevel, instances);
    }

    private void sendDocument(XWikiDocument document, boolean complete, Level level, Level minimumLevel,
        List<DocumentReplicationControllerInstance> instances) throws ReplicationException
    {
        if (level.ordinal() < minimumLevel.ordinal()) {
            // We don't want to send any message for this level of replication
            return;
        }

        List<ReplicationInstance> allInstances = getInstances(Level.ALL, instances);

        DocumentUpdateReplicationMessage message = this.documentMessageProvider.get();

        if (complete) {
            message.initialize(document.getDocumentReferenceWithLocale(), level);
        } else {
            message.initialize(document.getDocumentReferenceWithLocale(), document.getVersion(),
                document.getOriginalDocument().isNew() ? null : document.getOriginalDocument().getVersion(),
                document.getOriginalDocument().isNew() ? null : document.getOriginalDocument().getDate());
        }

        this.sender.send(message, allInstances);
    }

    /**
     * @param documentReference the reference of the document to delete
     * @param minimumLevel the minimum that need to be replicated from the document
     * @throws ReplicationException when failing to queue the replication message
     */
    public void sendDocumentDelete(DocumentReference documentReference, Level minimumLevel) throws ReplicationException
    {
        List<ReplicationInstance> instances = getInstances(documentReference, Level.REFERENCE);

        DocumentDeleteReplicationMessage message = this.documentDeleteMessageProvider.get();

        message.initialize(documentReference);

        this.sender.send(message, instances);
    }

    /**
     * @param documentReference the reference of the document to send
     * @param from the lowest version to delete
     * @param to the highest version to delete
     * @throws ReplicationException when failing to queue the replication message
     */
    public void sendDocumentHistoryDelete(DocumentReference documentReference, String from, String to)
        throws ReplicationException
    {
        // Sending history update only make sense to instance allowed to contains complete documents
        List<ReplicationInstance> instances = getInstances(documentReference, Level.ALL);

        if (!CollectionUtils.isEmpty(instances)) {
            DocumentHistoryDeleteReplicationMessage message = this.historyMessageProvider.get();
            message.initialize(documentReference, from, to);

            this.sender.send(message, instances);
        }
    }

    private List<ReplicationInstance> getInstances(Level level, List<DocumentReplicationControllerInstance> instances)
    {
        return instances.stream().filter(i -> i.getLevel() == level)
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }

    private List<ReplicationInstance> getInstances(DocumentReference reference, Level minimumLevel)
        throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> instances = this.controller.getTargetInstances(reference);

        return instances.stream().filter(i -> i.getLevel().ordinal() >= minimumLevel.ordinal())
            .map(DocumentReplicationControllerInstance::getInstance).collect(Collectors.toList());
    }
}
