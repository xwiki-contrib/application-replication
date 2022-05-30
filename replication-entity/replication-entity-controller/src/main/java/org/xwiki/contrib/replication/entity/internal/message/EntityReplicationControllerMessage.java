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
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.internal.AbstractNoContentEntityReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.DocumentReplicationControllerInstanceConverter;
import org.xwiki.model.reference.EntityReference;

/**
 * @version $Id$
 */
@Component(roles = EntityReplicationControllerMessage.class)
public class EntityReplicationControllerMessage extends AbstractNoContentEntityReplicationMessage<EntityReference>
{
    /**
     * The message type for these messages.
     */
    public static final String TYPE = TYPE_PREFIX + "controller";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the replication configuration for a given entity.
     */
    public static final String METADATA_CONFIGURATION = METADATA_PREFIX + "CONFIGURATION";

    @Override
    public String getType()
    {
        return TYPE;
    }

    /**
     * @param entityReference the reference of the entity to configure
     * @param configuration the configuration of the entity
     */
    public void initialize(EntityReference entityReference, List<DocumentReplicationControllerInstance> configuration)
    {
        super.initialize(entityReference, null);

        // Serialize the configuration
        setConfiguration(this.modifiableMetadata, configuration);
    }

    /**
     * @param metadata the message metadata
     * @param configuration the configuration of the entity
     */
    public static void setConfiguration(Map<String, Collection<String>> metadata,
        List<DocumentReplicationControllerInstance> configuration)
    {
        metadata.put(METADATA_CONFIGURATION, DocumentReplicationControllerInstanceConverter.toStrings(configuration));
    }
}
