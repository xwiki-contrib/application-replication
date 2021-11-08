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
public interface DocumentReplicationController
{
    /**
     * Indicate the list of registered instances this document should be replicated to.
     * 
     * @param documentReference the reference of the document about to be replicated
     * @return the registered instances on which to replicate the document
     * @throws ReplicationException when failing to get the instances
     */
    List<DocumentReplicationControllerInstance> getReplicationConfiguration(DocumentReference documentReference)
        throws ReplicationException;

    /**
     * Indicate the list of registered instances this document's messages should be relayed to.
     * 
     * @param documentReference the reference of the document about to be replicated
     * @return the registered instances on which to replicate the document
     * @throws ReplicationException when failing to get the instances
     */
    List<DocumentReplicationControllerInstance> getRelayConfiguration(DocumentReference documentReference)
        throws ReplicationException;
}
