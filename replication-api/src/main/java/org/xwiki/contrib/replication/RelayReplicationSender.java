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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.xwiki.component.annotation.Role;

/**
 * Various helpers to relay messages.
 * 
 * @version $Id$
 */
@Role
public interface RelayReplicationSender
{
    /**
     * @param message the message to relay
     * @param instances the instances to send the message to
     * @return the instance to relay the message to striped from those which already received it
     */
    List<ReplicationInstance> getRelayedInstances(ReplicationReceiverMessage message,
        Collection<ReplicationInstance> instances);

    /**
     * @param message the message to relay
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to relay the message
     */
    CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message) throws ReplicationException;

    /**
     * @param message the message to relay
     * @param targets the instances to send message to
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to relay the message
     */
    CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        Collection<ReplicationInstance> targets) throws ReplicationException;

    /**
     * @param message the message to relay
     * @param metadata custom metadata to relay
     * @param targets the instances to send message to
     * @return the new {@link CompletableFuture} providing the stored {@link ReplicationSenderMessage} before it's sent
     * @throws ReplicationException when failing to relay the message
     */
    CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        Map<String, Collection<String>> metadata, Collection<ReplicationInstance> targets) throws ReplicationException;
}
