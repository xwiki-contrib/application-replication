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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 */
@Role
public interface EntityReplication
{
    /**
     * @param documentReference the reference of the document
     * @return the owner instance of the document
     * @throws ReplicationException when failing to get the owner
     */
    String getOwner(DocumentReference documentReference) throws ReplicationException;

    /**
     * @param documents the references of the documents
     * @return the owner instances of the provided documents
     * @throws ReplicationException when failing to get the owners
     */
    List<String> getOwners(List<DocumentReference> documents) throws ReplicationException;

    /**
     * @param documentReference the reference of the document
     * @return true if the document has a replication conflict
     * @throws ReplicationException when failing to get the owner
     */
    boolean getConflict(DocumentReference documentReference) throws ReplicationException;

    /**
     * @param documentReference the reference of the document
     * @param conflict true if the document has a replication conflict
     * @throws ReplicationException when failing to update the conflict marker
     */
    void setConflict(DocumentReference documentReference, boolean conflict) throws ReplicationException;

    /**
     * @param documentReference the reference of the document
     * @return true if the document is readonly
     * @throws ReplicationException when failing to get the readonly status
     * @since 1.12.0
     */
    default boolean isReadonly(DocumentReference documentReference) throws ReplicationException
    {
        return false;
    }

    /**
     * @param documentReference the reference of the document
     * @param readonly true if the document is readonly
     * @throws ReplicationException when failing to update the conflict marker
     * @since 1.12.0
     */
    default void setReadonly(DocumentReference documentReference, boolean readonly) throws ReplicationException
    {
        
    }

    /**
     * @param documentReference the reference of the document
     * @throws ReplicationException when failing to remove the document from the index
     * @since 1.12.0
     */
    default void remove(DocumentReference documentReference) throws ReplicationException
    {

    }

    /**
     * Check and update the readonly status of the document.
     * 
     * @param documentReference the reference of the document
     * @throws ReplicationException when failing to check or update the document readonly status
     * @since 1.12.0
     */
    default void updateDocumentReadonly(DocumentReference documentReference) throws ReplicationException
    {

    }
}
