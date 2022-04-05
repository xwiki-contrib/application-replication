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

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Role
public interface DocumentReplicationController
{
    /**
     * Indicate the list of registered instances this document should be replicated to.
     * 
     * @param entityReference the reference of the entity for which we want to know the replication configuration
     * @return the registered instances on which to replicate the document
     * @throws ReplicationException when failing to get the configuration
     */
    List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference)
        throws ReplicationException;

    /**
     * Indicate the list of registered instances this document's messages should be relayed to.
     * 
     * @param entityReference the reference of the entity for which we want to know the replication configuration in the
     *            case of a relay
     * @return the registered instances on which to replicate the document
     * @throws ReplicationException when failing to get the configuration
     */
    List<DocumentReplicationControllerInstance> getRelayConfiguration(EntityReference entityReference)
        throws ReplicationException;

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

    /**
     * @param messageProducer called to generate the message to send
     * @param entityReference the entity associated with the message
     * @param minimumLevel the minimum level required from an instance configuration to receive the document
     * @throws ReplicationException when failing to send the message
     */
    void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel) throws ReplicationException;

    /**
     * Force pushing a complete document to allowed instances.
     * 
     * @param document the document to send
     * @throws ReplicationException when failing to replicate the document
     */
    void sendCompleteDocument(XWikiDocument document) throws ReplicationException;

    /**
     * Force pushing a complete document to allowed instances.
     * 
     * @param document the document to send
     * @param authors the users involved in the conflict
     * @throws ReplicationException when failing to replicate the document
     */
    void sendDocumentRepair(XWikiDocument document, Collection<String> authors) throws ReplicationException;

    /**
     * Give a chance to the controller to modify the document created for REFERENCE level replication before it's saved.
     * 
     * @param document the document to modify
     * @param message the received message
     * @return true if the document was modified
     * @throws ReplicationException when failing to execute the controller
     */
    boolean receiveREFERENCEDocument(XWikiDocument document, ReplicationReceiverMessage message)
        throws ReplicationException;
}
