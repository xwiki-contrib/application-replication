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
package org.xwiki.contrib.replication.entity.internal;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.xwiki.contrib.replication.ReplicationInstanceRecoverHandler;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.eventstream.Event;

/**
 * @version $Id$
 * @since 1.1
 */
public abstract class AbstractEntityReplicationInstanceRecoverHandler implements ReplicationInstanceRecoverHandler
{
    protected static final String EVENT_FIELD_METADATA_REFERENCE =
        ReplicationMessageEventQuery.customMetadataName(AbstractEntityReplicationMessage.METADATA_ENTITY_REFERENCE);

    protected static final String EVENT_FIELD_METADATA_RECOVER_TYPE =
        ReplicationMessageEventQuery.customMetadataName(AbstractEntityReplicationMessage.METADATA_ENTITY_RECOVER_TYPE);

    protected String getCustomMetadata(Event event, String field)
    {
        List<String> values = (List<String>) event.getCustom().get(field);
        if (CollectionUtils.isNotEmpty(values)) {
            return values.get(0);
        }

        return null;
    }
}
