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
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Role
public interface DocumentReplicationSender
{
    /**
     * @param messageProducer called to generate the message to send
     * @param entityReference the reference of the document to send
     * @param minimumLevel the minimum level required from an instance configuration to receive the document
     * @param configurations the replication configuration to follow or null if it should be asked to the controller
     * @param metadata custom metadata to add to the message
     * @throws ReplicationException when failing to send the document
     */
    void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel, Map<String, Collection<String>> metadata,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException;

    /**
     * @param documentReference the reference of the document to send
     * @param complete true if the complete document (with history and attachments content) should be sent
     * @param create true if it's a document creation
     * @param metadata custom metadata to add to the message
     * @param minimumLevel the minimum level required from an instance configuration to receive the document
     * @param configurations the replication configuration to follow or null if it should be asked to the controller
     * @throws ReplicationException when failing to send the document
     */
    void sendDocument(DocumentReference documentReference, boolean complete, boolean create,
        Map<String, Collection<String>> metadata, DocumentReplicationLevel minimumLevel,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException;

    /**
     * @param document the document to send
     * @param complete true if the complete document (with history and attachments content) should be sent
     * @param create true if it's a document creation
     * @param metadata custom metadata to add to the message
     * @param minimumLevel the minimum level required from an instance configuration to receive the document
     * @param configurations the replication configuration to follow or null if it should be asked to the controller
     * @throws ReplicationException when failing to send the document
     */
    void sendDocument(XWikiDocument document, boolean complete, boolean create,
        Map<String, Collection<String>> metadata, DocumentReplicationLevel minimumLevel,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException;

    /**
     * @param document the document to send
     * @param authors the users involved in the conflict
     * @param metadata custom metadata to add to the message
     * @param configurations the replication configuration to follow or null if it should be asked to the controller
     * @throws ReplicationException when failing to send the document
     */
    void sendDocumentRepair(XWikiDocument document, Collection<String> authors,
        Map<String, Collection<String>> metadata, Collection<DocumentReplicationControllerInstance> configurations)
        throws ReplicationException;

    /**
     * @param documentReference the reference of the document to delete
     * @param metadata custom metadata to add to the message
     * @param configurations the replication configuration to follow or null if it should be asked to the controller
     * @throws ReplicationException when failing to send the document delete
     */
    void sendDocumentDelete(DocumentReference documentReference, Map<String, Collection<String>> metadata,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException;

    /**
     * @param documentReference the reference of the document to delete
     * @param metadata custom metadata to add to the message
     * @param configurations the replication configuration to follow or null if it should be asked to the controller
     * @throws ReplicationException when failing to send the document delete
     */
    void sendDocumentUnreplicate(DocumentReference documentReference, Map<String, Collection<String>> metadata,
        Collection<DocumentReplicationControllerInstance> configurations) throws ReplicationException;

    /**
     * @param documentReference the reference of the document from which to delete the versions
     * @param from the version from which to start deleting
     * @param to the version where to stop deleting
     * @param metadata custom metadata to add to the message
     * @param configurations the replication configuration to follow or null if it should be asked to the controller
     * @throws ReplicationException when failing to send the document history delete
     */
    void sendDocumentHistoryDelete(DocumentReference documentReference, String from, String to,
        Map<String, Collection<String>> metadata, Collection<DocumentReplicationControllerInstance> configurations)
        throws ReplicationException;
}
