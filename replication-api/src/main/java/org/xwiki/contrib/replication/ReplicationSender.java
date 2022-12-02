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
import java.util.concurrent.CompletableFuture;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;

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
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to queue the replication message
     */
    CompletableFuture<ReplicationSenderMessage> send(ReplicationSenderMessage message) throws ReplicationException;

    /**
     * Asynchronously send a message to passed instances.
     * 
     * @param message the data to send
     * @param targets the instances to send the message to
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to queue the replication message
     */
    CompletableFuture<ReplicationSenderMessage> send(ReplicationSenderMessage message,
        Collection<ReplicationInstance> targets) throws ReplicationException;

    /**
     * Notify the sender that the passed instance sent a ping. Among other things this suggest the sender to force
     * waking up the queue if it has messages to send.
     * 
     * @param instance the instance which sent a ping
     */
    void ping(ReplicationInstance instance);

    /**
     * @param query the query to match logged events to send
     * @param receivers the specific instances to send the message to, null for all instances
     * @throws ReplicationException when failing to query or send logged messages
     * @since 1.3.0
     */
    void resend(ReplicationMessageEventQuery query, Collection<String> receivers) throws ReplicationException;
}
