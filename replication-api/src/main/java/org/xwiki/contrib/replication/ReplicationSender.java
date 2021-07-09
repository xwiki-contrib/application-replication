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
package org.xwiki.contrib.replication;

import java.util.Collection;

import org.xwiki.component.annotation.Role;

/**
 * @version $Id$
 */
@Role
public interface ReplicationSender
{
    /**
     * Asynchronously send a message to all registered instances.
     * 
     * @param message the data to send
     * @throws ReplicationException when failing to queue the replication message
     */
    void send(ReplicationSenderMessage message) throws ReplicationException;

    /**
     * Asynchronously send a message to passed instances.
     * 
     * @param message the data to send
     * @param targets the instances to send data to
     * @throws ReplicationException when failing to queue the replication message
     */
    void send(ReplicationSenderMessage message, Collection<ReplicationInstance> targets) throws ReplicationException;
}
