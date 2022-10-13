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
package org.xwiki.contrib.replication.entity.internal.like;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.like.events.LikeEvent;
import org.xwiki.like.events.UnlikeEvent;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.user.UserReference;

/**
 * @version $Id$
 */
@Component
@Named(LikeReplicationListener.NAME)
@Singleton
public class LikeReplicationListener extends AbstractEventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME = "LikeReplicationListener";

    @Inject
    private ReplicationContext replicationContext;

    @Inject
    private RemoteObservationManagerContext remoteContext;

    @Inject
    private LikeMessageSender likeSender;

    @Inject
    private Logger logger;

    /**
     * Init the listener.
     */
    public LikeReplicationListener()
    {
        super(NAME, new LikeEvent(), new UnlikeEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Ignore the modification if it's been caused by a replication or a remote event
        if (this.replicationContext.isReplicationMessage() || this.remoteContext.isRemoteState()) {
            return;
        }

        boolean like = event instanceof LikeEvent;
        UserReference userReference = (UserReference) source;
        EntityReference entityReference = (EntityReference) data;

        try {
            this.likeSender.send(userReference, entityReference, like, null);
        } catch (ReplicationException e) {
            this.logger.error("Failed to send a replication message for [{}] done by user [{}] on entity [{}]",
                like ? "like" : "unlike", userReference, entityReference);
        }
    }
}
