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
package org.xwiki.contrib.replication.entity.internal.message;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.model.reference.EntityReference;

/**
 * Synchronize replication configuration between instances.
 * 
 * @version $Id$
 */
@Component(roles = EntityReplicationControllerSender.class)
@Singleton
public class EntityReplicationControllerSender
{
    @Inject
    private Provider<EntityReplicatioControllerMessage> messageProvider;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private ReplicationSender sender;

    /**
     * @param reference the reference of the configured entity
     * @param configuration the replication configuration of the entity
     * @throws ReplicationException when failing to send replication configuration to other instances
     */
    public void send(EntityReference reference, List<DocumentReplicationControllerInstance> configuration)
        throws ReplicationException
    {
        // Send the configuration to everyone, each instance will decide what to do with it (including when they are not
        // or not anymore supposed to be part of the replication)
        // TODO: cut this in two different messaging for adding new instance and removing instances ?
        Collection<ReplicationInstance> instances = this.instanceManager.getRegisteredInstances();

        if (!instances.isEmpty()) {
            EntityReplicatioControllerMessage message = this.messageProvider.get();

            message.initialize(reference, configuration);

            this.sender.send(message, instances);
        }
    }
}
