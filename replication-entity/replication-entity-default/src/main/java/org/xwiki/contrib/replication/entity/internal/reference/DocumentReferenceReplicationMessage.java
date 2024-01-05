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
package org.xwiki.contrib.replication.entity.internal.reference;

import java.util.Collection;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.DocumentReplicationSenderMessageBuilder;
import org.xwiki.contrib.replication.entity.internal.AbstractNoContentDocumentReplicationMessage;
import org.xwiki.user.UserReference;

/**
 * @version $Id$
 */
@Component(roles = DocumentReferenceReplicationMessage.class)
public class DocumentReferenceReplicationMessage extends AbstractNoContentDocumentReplicationMessage
{
    @Override
    public String getType()
    {
        return TYPE_DOCUMENT_REFERENCE;
    }

    /**
     * @param builder the builder used to produce the message
     * @param creatorReference the reference of the creator of the document
     * @param extraMetadata custom metadata to add to the message
     * @since 2.0.0
     */
    public void initialize(DocumentReplicationSenderMessageBuilder builder, UserReference creatorReference,
        Map<String, Collection<String>> extraMetadata)
    {
        initialize(builder, extraMetadata);

        putCustomMetadata(METADATA_ENTITY_CREATOR, creatorReference);

        // REFERENCE documents are readonly by definition
        putCustomMetadata(METADATA_DOCUMENT_UPDATE_READONLY, true);
    }
}
