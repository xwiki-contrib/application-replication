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
package org.xwiki.contrib.replication.entity.internal.history;

import java.util.Collection;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.internal.AbstractNoContentEntityReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 */
@Component(roles = DocumentHistoryDeleteReplicationMessage.class)
public class DocumentHistoryDeleteReplicationMessage
    extends AbstractNoContentEntityReplicationMessage<DocumentReference>
{
    /**
     * The message type for these messages.
     */
    public static final String TYPE = TYPE_PREFIX + "_history";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the lowest version to delete from the document history.
     */
    public static final String METADATA_VERSION_FROM = METADATA_PREFIX + "VERSION_FROM";

    /**
     * The name of the metadata containing the highest version to delete from the document history.
     */
    public static final String METADATA_VERSION_TO = METADATA_PREFIX + "VERSION_TO";

    /**
     * @param documentReference the reference of the document affected by this message
     * @param from the lowest version to delete
     * @param to the highest version to delete
     * @param metadata custom metadata to add to the message
     */
    public void initialize(DocumentReference documentReference, String from, String to,
        Map<String, Collection<String>> metadata)
    {
        initialize(documentReference, metadata);

        putMetadata(METADATA_VERSION_FROM, from);
        putMetadata(METADATA_VERSION_TO, to);
    }

    @Override
    public String getType()
    {
        return TYPE;
    }
}
