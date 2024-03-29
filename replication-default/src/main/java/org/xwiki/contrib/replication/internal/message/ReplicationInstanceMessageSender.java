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
package org.xwiki.contrib.replication.internal.message;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSender;

/**
 * Helper to send various instance related messages.
 * 
 * @version $Id$
 */
@Component(roles = ReplicationInstanceMessageSender.class)
@Singleton
public class ReplicationInstanceMessageSender
{
    @Inject
    private Provider<ReplicationInstanceUpdateMessage> messageProvider;

    @Inject
    private ReplicationSender sender;

    /**
     * Send an update of the current instance to linked instances.
     * 
     * @throws ReplicationException when failing to create the message
     */
    public void updateCurrentInstance() throws ReplicationException
    {
        updateCurrentInstance(null);
    }

    /**
     * Send an update of the current instance to linked instances.
     * 
     * @param target the instance to which to send the message
     * @throws ReplicationException when failing to create the message
     * @since 1.10.0
     */
    public void updateCurrentInstance(ReplicationInstance target) throws ReplicationException
    {
        ReplicationInstanceUpdateMessage message = this.messageProvider.get();
        message.initializeCurrent();

        if (target != null) {
            this.sender.send(message, List.of(target));
        } else {
            this.sender.send(message);
        }
    }
}
