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
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * @version $Id$
 */
@Role
public interface DocumentReplicationMessageReader
{
    /**
     * @param message the message
     * @return the document associated with the message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    DocumentReference getDocumentReference(ReplicationMessage message) throws InvalidReplicationMessageException;

    /**
     * @param message the message
     * @param reference the reference of the entity (without the locale)
     * @return the document associated with the message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    DocumentReference getDocumentReference(ReplicationMessage message, EntityReference reference)
        throws InvalidReplicationMessageException;

    /**
     * @param message the message
     * @return the entity associated with the message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    EntityReference getEntityReference(ReplicationMessage message) throws InvalidReplicationMessageException;

    /**
     * @param message the message
     * @param mandatory true of the property is mandatory
     * @return the entity associated with the message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    EntityReference getEntityReference(ReplicationMessage message, boolean mandatory)
        throws InvalidReplicationMessageException;

    /**
     * @param message the message
     * @return the version of the document associated with the message
     * @throws InvalidReplicationMessageException
     */
    String getDocumentVersion(ReplicationMessage message) throws InvalidReplicationMessageException;

    /**
     * @param message the message
     * @return the user reference from the context when the message was created
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    DocumentReference getContextUser(ReplicationMessage message) throws InvalidReplicationMessageException;

    /**
     * @param message the message
     * @return true if the document in the message is complete
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    boolean isComplete(ReplicationMessage message) throws InvalidReplicationMessageException;
}
