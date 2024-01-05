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
package org.xwiki.contrib.replication.entity.internal.repair;

import java.util.Collection;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.internal.update.DocumentUpdateReplicationMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

/**
 * @version $Id$
 */
@Component(roles = DocumentRepairReplicationMessage.class)
public class DocumentRepairReplicationMessage extends DocumentUpdateReplicationMessage
{
    /**
     * The message type for these messages.
     */
    public static final String TYPE = TYPE_PREFIX + "repair";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the authors involved in the conflict.
     */
    public static final String METADATA_CONFLICT_AUTHORS = METADATA_PREFIX + "AUTHORS";

    @Override
    public String getType()
    {
        return TYPE;
    }

    /**
     * Initialize a message for a complete replication.
     * 
     * @param documentReference the reference of the document affected by this message
     * @param version the version of the document
     * @param creator the user who created the document
     * @param authors the users involved in the conflict
     * @param receivers the instances which are supposed to handler the message
     * @param metadata custom metadata to add to the message
     * @throws ReplicationException when failing to initialize the message
     * @since 1.1
     */
    public void initializeRepair(DocumentReference documentReference, String version, UserReference creator,
        Collection<String> authors, Collection<String> receivers, Map<String, Collection<String>> metadata)
        throws ReplicationException
    {
        initialize(documentReference, version, true, creator, receivers, metadata);

        putCustomMetadata(METADATA_CONFLICT_AUTHORS, authors);
    }
}
