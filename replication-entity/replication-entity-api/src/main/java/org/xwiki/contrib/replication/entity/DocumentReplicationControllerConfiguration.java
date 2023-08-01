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

import java.util.Map;
import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Indicate which controller to use for a given entity.
 * 
 * @version $Id$
 * @since 1.1
 */
@Role
public interface DocumentReplicationControllerConfiguration
{
    /**
     * @return the available controllers or null if the configuration is dynamic
     * @throws ReplicationException when failing to lookup components
     */
    Optional<Map<String, DocumentReplicationController>> getControllers() throws ReplicationException;

    /**
     * @param entityReference the reference of the entity to replicate
     * @return the replication controller in charge of the document
     * @throws ReplicationException when failing to retrieve the configured controller
     */
    DocumentReplicationController resolveDocumentReplicationController(EntityReference entityReference)
        throws ReplicationException;

    /**
     * @param message the message to relay
     * @return the replication controller in charge of the document
     * @throws ReplicationException when failing to retrieve the configured controller
     * @since 1.7.0
     */
    DocumentReplicationController resolveDocumentReplicationController(ReplicationReceiverMessage message)
        throws ReplicationException;

    /**
     * @param document the deleted document to replicate
     * @return the replication controller in charge of the deleted document
     * @throws ReplicationException when failing to retrieve the configured controller
     * @since 2.0.0
     */
    DocumentReplicationController resolveDocumentReplicationController(XWikiDocument document)
        throws ReplicationException;
}
