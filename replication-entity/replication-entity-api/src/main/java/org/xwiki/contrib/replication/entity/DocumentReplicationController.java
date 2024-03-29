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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Role
public interface DocumentReplicationController
{
    // Configuration

    /**
     * Indicate the list of registered instances this document should be replicated to.
     * 
     * @param entityReference the reference of the entity for which we want to know the replication configuration
     * @return the replication configuration
     * @throws ReplicationException when failing to get the configuration
     */
    List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference)
        throws ReplicationException;

    /**
     * Indicate the list of registered instances this document should be replicated to.
     * 
     * @param document the document for which we want to know the replication configuration
     * @return the replication configuration
     * @throws ReplicationException when failing to get the configuration
     * @since 2.0.0
     */
    List<DocumentReplicationControllerInstance> getReplicationConfiguration(XWikiDocument document)
        throws ReplicationException;

    /**
     * Indicate the list of registered instances this message should be relayed to.
     * 
     * @param message the message to relay
     * @return the replication configuration
     * @throws ReplicationException when failing to get the configuration
     * @since 1.6.0
     */
    List<DocumentReplicationControllerInstance> getRelayConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException;

    /**
     * Indicate the replication configuration associated with this received message.
     * 
     * @param message the message to relay
     * @return the replication configuration
     * @throws ReplicationException when failing to get the configuration
     * @since 2.0.0
     */
    DocumentReplicationControllerInstance getReceiveConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException;

    /**
     * A user interface oriented report of the replication associated to the passed entity. It's generally a mix of
     * direct and relayed replication configuration.
     * 
     * @param entityReference the reference of the entity for which we want to know the replication configuration
     * @return the replication configuration
     * @throws ReplicationException when failing to get the configuration
     * @since 2.0.0
     */
    List<DocumentReplicationControllerInstance> getDisplayReplicationConfiguration(EntityReference entityReference)
        throws ReplicationException;

    // Events

    /**
     * @param document the created document
     * @throws ReplicationException when failing to replicate the document creation
     */
    void onDocumentCreated(XWikiDocument document) throws ReplicationException;

    /**
     * @param document the updated document
     * @throws ReplicationException when failing to replicate the document update
     */
    void onDocumentUpdated(XWikiDocument document) throws ReplicationException;

    /**
     * @param document the deleted document
     * @throws ReplicationException when failing to replicate the document update
     */
    void onDocumentDeleted(XWikiDocument document) throws ReplicationException;

    /**
     * @param document the document from which version has been deleted
     * @param from the version from which to start deleting
     * @param to the version where to stop deleting
     * @throws ReplicationException when failing to replicate the document history delete
     */
    void onDocumentHistoryDelete(XWikiDocument document, String from, String to) throws ReplicationException;

    // Send

    /**
     * @param messageBuilder the instance in charge of provide various document related information about the message
     *            and eventually produce it
     * @throws ReplicationException when failing to send the message
     * @since 2.0.0
     */
    void send(EntityReplicationSenderMessageBuilder messageBuilder) throws ReplicationException;

    /**
     * @param messageBuilder the instance in charge of provide various document related information about the message
     *            and eventually produce it
     * @param customConfigurations optional custom replication configuration to use instead of the configured ones for
     *            the document
     * @throws ReplicationException when failing to send the message
     * @since 2.0.0
     */
    void send(EntityReplicationSenderMessageBuilder messageBuilder,
        Collection<DocumentReplicationControllerInstance> customConfigurations) throws ReplicationException;

    /**
     * Force sending to configured instances the current status of the document.
     * 
     * @param documentReference the reference of the document
     * @throws ReplicationException when failing to send the document
     * @see EntityReplicationBuilders#documentMessageBuilder(DocumentReference)
     * @since 2.0.0
     */
    void sendDocument(DocumentReference documentReference) throws ReplicationException;

    /**
     * Force sending to configured instances the current status of the document.
     * 
     * @param documentReference the reference of the document
     * @param customConfigurations optional custom replication configuration to use instead of the configured ones for
     *            the document
     * @throws ReplicationException when failing to send the document
     * @see EntityReplicationBuilders#documentMessageBuilder(DocumentReference)
     * @since 2.0.0
     */
    void sendDocument(DocumentReference documentReference,
        Collection<DocumentReplicationControllerInstance> customConfigurations) throws ReplicationException;

    // Receiver

    /**
     * SolrInputDocument Give a chance to the controller to modify the document created for REFERENCE level replication
     * before it's saved.
     * 
     * @param document the document to modify
     * @param message the received message
     * @return true if the document was modified
     * @throws ReplicationException when failing to execute the controller
     */
    boolean receiveREFERENCEDocument(XWikiDocument document, ReplicationReceiverMessage message)
        throws ReplicationException;

    /**
     * @param message the received message
     * @return the message to handle/relay
     * @throws InvalidReplicationMessageException when the message format is wrong
     * @throws ReplicationException when failing to filter the message
     * @since 2.0.0
     */
    ReplicationReceiverMessage filter(ReplicationReceiverMessage message) throws ReplicationException;

    // Relay

    /**
     * @param message the message to relay
     * @param minimumLevel the minimum level required to relay the message
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to queue the replication message
     * @since 2.0.0
     * @since 1.12.11
     */
    CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel) throws ReplicationException;

    /**
     * @param message the message to relay
     * @param minimumLevel the minimum level required to relay the message
     * @param maximumLevel the maximum level required to relay the message
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to queue the replication message
     * @since 2.0.0
     * @since 1.12.11
     */
    CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        DocumentReplicationLevel minimumLevel, DocumentReplicationLevel maximumLevel) throws ReplicationException;

    /**
     * @param message the document update message to relay
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to queue the replication message
     * @since 2.0.0
     * @since 1.12.11
     */
    CompletableFuture<ReplicationSenderMessage> relayDocumentUpdate(ReplicationReceiverMessage message)
        throws ReplicationException;
}
