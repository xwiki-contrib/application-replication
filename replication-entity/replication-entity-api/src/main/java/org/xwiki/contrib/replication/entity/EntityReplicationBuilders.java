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

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provider builders used to produce messages on demand.
 * 
 * @version $Id$
 * @since 2.0.0
 */
@Role
public interface EntityReplicationBuilders
{
    /**
     * @param messageProducer called to generate the message to send
     * @param document the document associated with the message
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     */
    DocumentReplicationSenderMessageBuilder documentMessageBuilder(
        EntityReplicationSenderMessageBuilderProducer<DocumentReplicationSenderMessageBuilder> messageProducer,
        XWikiDocument document) throws ReplicationException;

    /**
     * @param messageProducer called to generate the message to send
     * @param document the document associated with the message
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentMessageBuilder(
        EntityReplicationSenderMessageBuilderProducer<DocumentReplicationSenderMessageBuilder> messageProducer,
        DocumentReference document) throws ReplicationException;

    /**
     * @param messageProducer called to generate the message to send
     * @param document the document associated with the message
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    EntityReplicationSenderMessageBuilder entityMessageBuilder(
        EntityReplicationSenderMessageBuilderProducer<EntityReplicationSenderMessageBuilder> messageProducer,
        EntityReference document) throws ReplicationException;

    /**
     * Provide a builder which produce a REFERENCE replication message.
     * 
     * @param document the document associated with the message
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentReferenceMessageBuilder(XWikiDocument document)
        throws ReplicationException;

    /**
     * Provide a builder which produce a partial document update replication message.
     * 
     * @param document the document associated with the message
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentPartialUpdateMessageBuilder(XWikiDocument document)
        throws ReplicationException;

    /**
     * Provide a builder which produce a complete document replication message.
     * 
     * @param document the document associated with the message
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentCompleteUpdateMessageBuilder(XWikiDocument document)
        throws ReplicationException;

    /**
     * Provide a builder which produce a document creation replication message.
     * 
     * @param document the document associated with the message
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentCreateMessageBuilder(XWikiDocument document)
        throws ReplicationException;

    /**
     * Provide a builder which produce a document delete replication message.
     * 
     * @param documentReference the reference of the document to delete
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentDeleteMessageBuilder(DocumentReference documentReference)
        throws ReplicationException;

    /**
     * Provide a builder which produce a document delete replication message.
     * 
     * @param document the document to delete
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentDeleteMessageBuilder(XWikiDocument document)
        throws ReplicationException;

    /**
     * Provide a builder which produce a document unreplicate message.
     * 
     * @param document the document to unreplicate
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentUnreplicateMessageBuilder(XWikiDocument document)
        throws ReplicationException;

    /**
     * Provide a builder which produce a document history versions delete replication message.
     * 
     * @param document the document from which to delete the versions
     * @param from the version from which to start deleting
     * @param to the version where to stop deleting
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentHistoryMessageBuilder(XWikiDocument document, String from,
        String to) throws ReplicationException;

    /**
     * Provide a builder which produce a document history versions delete replication message.
     * 
     * @param documentReference the reference of the document to repair
     * @param sourceOnly true if the repair should be send back only to the source, false for a network wide repair
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentRepairRequestMessageBuilder(DocumentReference documentReference,
        boolean sourceOnly) throws ReplicationException;

    /**
     * Provide a builder which produce a document conflict status update replication message.
     * 
     * @param documentReference the reference of the document to repair
     * @return the document sender message builder
     * @throws ReplicationException when failing to create the document sender message builder
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentConflictUpdateMessageBuilder(DocumentReference documentReference)
        throws ReplicationException;

    /**
     * Force replicate the complete current state of the document to configured instances. The type of message is
     * selected based on the status of the document and on the replication configuration.
     * <p>
     * If the document does not exist a delete replication message is produced (but it won't be sent anything in
     * practice if there is no replication configuration associated with this document ghost document).
     * 
     * @param documentReference the reference of the document to replicate
     * @return the document sender message builder
     * @throws ReplicationException when failing to replicate the document
     * @since 2.0.0
     */
    DocumentReplicationSenderMessageBuilder documentMessageBuilder(DocumentReference documentReference)
        throws ReplicationException;
}
