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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.SerializableReplicationReceiverMessage;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

/**
 * @version $Id$
 * @since 2.3.0
 */
@Component
@Named("ReplicationReceiverMessageConverter")
@Singleton
public class ReplicationReceiverMessageConverter extends AbstractEventConverter
{
    private static final Set<String> CONVERTED_MESSAGES = new HashSet<>(Arrays.asList(ReplicationMessage.TYPE_ANSWER));

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private Logger logger;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getEvent() instanceof ReplicationReceiverMessageEvent) {
            ReplicationReceiverMessageEvent messageEvent = (ReplicationReceiverMessageEvent) localEvent.getEvent();

            if (CONVERTED_MESSAGES.contains(messageEvent.getType())) {
                try {
                    remoteEvent.setSource(new SerializableReplicationReceiverMessage(
                        (ReplicationReceiverMessage) localEvent.getSource()));
                    remoteEvent.setEvent((ReplicationReceiverMessageEvent) localEvent.getEvent());

                    return true;
                } catch (IOException e) {
                    this.logger.error("Failed to serialize the message [{}]", localEvent.getSource().toString(), e);
                }
            }
        }

        return false;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (remoteEvent.getEvent() instanceof ReplicationReceiverMessageEvent) {
            try {
                SerializableReplicationReceiverMessage message =
                    (SerializableReplicationReceiverMessage) remoteEvent.getSource();
                message.resolveInstance(this.instances);
                localEvent.setSource(message);

                localEvent.setEvent((ReplicationReceiverMessageEvent) remoteEvent.getEvent());

                return true;
            } catch (ReplicationException e) {
                this.logger.error("Failed to unserialize the message [{}]", localEvent.getSource().toString(), e);
            }
        }

        return false;
    }
}
