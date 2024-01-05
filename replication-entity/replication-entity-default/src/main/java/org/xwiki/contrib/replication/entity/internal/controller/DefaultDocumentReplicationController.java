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
package org.xwiki.contrib.replication.entity.internal.controller;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerConfiguration;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.DocumentReplicationSenderMessageBuilder;
import org.xwiki.contrib.replication.entity.EntityReplicationSenderMessageBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of DocumentReplicationController redirecting to actual {@link DocumentReplicationController}
 * according to the configuration.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultDocumentReplicationController implements DocumentReplicationController
{
    @Inject
    private DocumentReplicationControllerConfiguration configuration;

    private DocumentReplicationController getController(EntityReference documentReference) throws ReplicationException
    {
        return this.configuration.resolveDocumentReplicationController(documentReference);
    }

    private DocumentReplicationController getController(XWikiDocument document) throws ReplicationException
    {
        return this.configuration.resolveDocumentReplicationController(document);
    }

    private DocumentReplicationController getController(ReplicationReceiverMessage message) throws ReplicationException
    {
        return this.configuration.resolveDocumentReplicationController(message);
    }

    private DocumentReplicationController getController(EntityReplicationSenderMessageBuilder messageBuilder)
        throws ReplicationException
    {
        if (messageBuilder instanceof DocumentReplicationSenderMessageBuilder) {
            XWikiDocument document = ((DocumentReplicationSenderMessageBuilder) messageBuilder).getDocument();

            if (document != null) {
                return getController(document);
            }
        }

        return getController(messageBuilder.getEntityReference());
    }

    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference)
        throws ReplicationException
    {
        return getController(entityReference).getReplicationConfiguration(entityReference);
    }

    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(XWikiDocument document)
        throws ReplicationException
    {
        return getController(document).getReplicationConfiguration(document);
    }

    @Override
    public List<DocumentReplicationControllerInstance> getRelayConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return getController(message).getRelayConfiguration(message);
    }

    @Override
    public DocumentReplicationControllerInstance getReceiveConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return getController(message).getReceiveConfiguration(message);
    }

    @Override
    public void onDocumentCreated(XWikiDocument document) throws ReplicationException
    {
        getController(document).onDocumentCreated(document);
    }

    @Override
    public void onDocumentUpdated(XWikiDocument document) throws ReplicationException
    {
        getController(document).onDocumentUpdated(document);
    }

    @Override
    public void onDocumentDeleted(XWikiDocument document) throws ReplicationException
    {
        getController(document).onDocumentDeleted(document);
    }

    @Override
    public void onDocumentHistoryDelete(XWikiDocument document, String from, String to) throws ReplicationException
    {
        getController(document).onDocumentHistoryDelete(document, from, to);
    }

    @Override
    public boolean receiveREFERENCEDocument(XWikiDocument document, ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return getController(document).receiveREFERENCEDocument(document, message);
    }

    @Override
    public ReplicationReceiverMessage filter(ReplicationReceiverMessage message) throws ReplicationException
    {
        return getController(message).filter(message);
    }

    @Override
    public void send(EntityReplicationSenderMessageBuilder messageBuilder) throws ReplicationException
    {
        getController(messageBuilder).send(messageBuilder);
    }

    @Override
    public void send(EntityReplicationSenderMessageBuilder messageBuilder,
        Collection<DocumentReplicationControllerInstance> customConfigurations) throws ReplicationException
    {
        getController(messageBuilder).send(messageBuilder, customConfigurations);
    }

    @Override
    public void sendDocument(DocumentReference documentReference) throws ReplicationException
    {
        getController(documentReference).sendDocument(documentReference);
    }

    @Override
    public void sendDocument(DocumentReference documentReference,
        Collection<DocumentReplicationControllerInstance> customConfigurations) throws ReplicationException
    {
        getController(documentReference).sendDocument(documentReference, customConfigurations);
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel) throws ReplicationException
    {
        return getController(message).relay(message, minimumLevel);
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel, DocumentReplicationLevel maximumLevel) throws ReplicationException
    {
        return getController(message).relay(message, minimumLevel, maximumLevel);
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relayDocumentUpdate(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return getController(message).relayDocumentUpdate(message);
    }
}
