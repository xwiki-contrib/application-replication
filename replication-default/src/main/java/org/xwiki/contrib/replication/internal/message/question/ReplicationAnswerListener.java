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
package org.xwiki.contrib.replication.internal.message.question;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.message.ReplicationReceiverMessageEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * Handle receive answer messages.
 * 
 * @version $Id$
 * @since 2.3.0
 */
@Component
@Named(ReplicationAnswerListener.NAME)
@Singleton
public class ReplicationAnswerListener extends AbstractEventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME =
        "org.xwiki.contrib.replication.internal.message.question.ReplicationAnswerListener";

    @Inject
    private Provider<ReplicationAnswerManager> managerProvider;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ReplicationAnswerListener()
    {
        super(NAME, new ReplicationReceiverMessageEvent(ReplicationMessage.TYPE_ANSWER));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ReplicationReceiverMessageEvent && this.remoteObservationManagerContext.isRemoteState()) {
            try {
                this.managerProvider.get().onReceive((ReplicationReceiverMessage) source);
            } catch (InvalidReplicationMessageException e) {
                this.logger.error("Failed to handle received answer message");
            }
        }
    }
}
