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

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.EntityReplicationBuilders;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.user.UserReference;

/**
 * @version $Id$
 * @since 1.1
 */
@Component(roles = LikeMessageSender.class)
@Singleton
public class LikeMessageSender
{
    @Inject
    private Provider<LikeReplicationMessage> likeMessageProvider;

    @Inject
    private EntityReplicationBuilders builders;

    @Inject
    private DocumentReplicationController replicationController;

    /**
     * @param user the reference of the user who performs the like
     * @param entityReference the reference of the entity being target of the like
     * @param like true if it's a like and false if it's an unlike
     * @param receivers the instances which are supposed to handler the message
     * @throws ReplicationException when failing to send the message
     */
    public void send(UserReference user, EntityReference entityReference, boolean like, Collection<String> receivers)
        throws ReplicationException
    {
        this.replicationController.send(this.builders.entityMessageBuilder((id, level, readonly, extraMetadata) -> {
            LikeReplicationMessage likeMessage = this.likeMessageProvider.get();

            likeMessage.initialize(user, entityReference, like, receivers, extraMetadata);

            return likeMessage;
        }, entityReference).minimumLevel(DocumentReplicationLevel.ALL).receivers(receivers), null);
    }
}
