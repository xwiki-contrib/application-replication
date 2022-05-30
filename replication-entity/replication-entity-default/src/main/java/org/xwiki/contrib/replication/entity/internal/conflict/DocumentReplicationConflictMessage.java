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
package org.xwiki.contrib.replication.entity.internal.conflict;

import java.util.Collection;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.internal.AbstractNoContentEntityReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 */
@Component(roles = DocumentReplicationConflictMessage.class)
public class DocumentReplicationConflictMessage extends AbstractNoContentEntityReplicationMessage<DocumentReference>
{
    /**
     * The message type for these messages.
     */
    public static final String TYPE = TYPE_PREFIX + "conflict";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE.toUpperCase() + '_';

    /**
     * The name of the metadata indicating if the conflict marker should be set or removed.
     */
    public static final String METADATA_CONFLICT = METADATA_PREFIX + "CONFLICT";

    @Override
    public String getType()
    {
        return TYPE;
    }

    /**
     * @param documentReference the reference of the document for which to replicated the conflict status
     * @param conflict true if the document has a replication conflict
     * @param metadata custom metadata to add to the message
     */
    public void initialize(DocumentReference documentReference, boolean conflict,
        Map<String, Collection<String>> metadata)
    {
        super.initialize(documentReference, metadata);

        putCustomMetadata(METADATA_CONFLICT, conflict);
    }
}
