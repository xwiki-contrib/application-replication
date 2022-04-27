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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.internal.AbstractEntityReplicationReceiver;
import org.xwiki.like.LikeException;
import org.xwiki.like.LikeManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(LikeMessage.TYPE)
public class LikeReceiver extends AbstractEntityReplicationReceiver
{
    @Inject
    private LikeManager likes;

    @Override
    protected void receiveEntity(ReplicationReceiverMessage message, EntityReference entityReference,
        XWikiContext xcontext) throws ReplicationException
    {
        boolean like = this.messageReader.getMetadata(message, LikeMessage.METADATA_LIKE, true, Boolean.class);
        UserReference userReference =
            this.messageReader.getMetadata(message, LikeMessage.METADATA_CREATOR, true, UserReference.class);

        try {
            if (like) {
                this.likes.saveLike(userReference, entityReference);
            } else {
                this.likes.removeLike(userReference, entityReference);
            }
        } catch (LikeException e) {
            throw new ReplicationException("Failed to store like", e);
        }
    }
}
