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
package org.xwiki.contrib.replication.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.RelayReplicationSender;
import org.xwiki.contrib.replication.RelayedReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.ReplicationSenderMessage;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultRelayReplicationSender implements RelayReplicationSender
{
    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private ReplicationSender sender;

    @Override
    public List<ReplicationInstance> getRelayedInstances(ReplicationReceiverMessage message,
        Collection<ReplicationInstance> instances)
    {
        List<ReplicationInstance> targets = new ArrayList<>(instances.size());
        for (ReplicationInstance instance : instances) {
            if (!instance.getURI().equals(message.getSource()) && instance != message.getInstance()) {
                targets.add(instance);
            }
        }

        return targets;
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        // Relay the message
        return relay(message, this.instances.getRegisteredInstances());
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        Collection<ReplicationInstance> targets) throws ReplicationException
    {
        return this.sender.send(new RelayedReplicationSenderMessage(message), getRelayedInstances(message, targets));
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message,
        Map<String, Collection<String>> metadata, Collection<ReplicationInstance> targets) throws ReplicationException
    {
        return this.sender.send(new RelayedReplicationSenderMessage(message, metadata),
            getRelayedInstances(message, targets));
    }
}
