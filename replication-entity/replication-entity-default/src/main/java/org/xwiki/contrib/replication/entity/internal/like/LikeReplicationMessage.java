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
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.internal.AbstractNoContentEntityReplicationMessage;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.user.UserReference;

/**
 * @version $Id$
 */
@Component(roles = LikeReplicationMessage.class)
public class LikeReplicationMessage extends AbstractNoContentEntityReplicationMessage<EntityReference>
{
    @Override
    public String getType()
    {
        return TYPE_LIKE;
    }

    /**
     * @param user the reference of the user who performs the like
     * @param entity the reference of the entity being target of the like
     * @param like true if it's a like and false if it's an unlike
     * @param receivers the instances which are supposed to handler the message
     * @param metadata custom metadata to add to the message
     */
    public void initialize(UserReference user, EntityReference entity, boolean like, Collection<String> receivers,
        Map<String, Collection<String>> metadata)
    {
        super.initialize(entity, receivers, metadata);

        putCustomMetadata(METADATA_ENTITY_CREATOR, user);
        putCustomMetadata(METADATA_LIKE, like);
    }
}
