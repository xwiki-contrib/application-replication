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

import java.util.Collections;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.internal.AbstractNoContentDocumentReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 */
@Component(roles = DocumentReferenceReplicationMessage.class)
public class DocumentReferenceReplicationMessage extends AbstractNoContentDocumentReplicationMessage
{
    /**
     * The message type for these messages.
     */
    public static final String TYPE = TYPE_PREFIX + "reference";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the creator of the document.
     */
    public static final String METADATA_CREATOR = METADATA_PREFIX + "CREATOR";

    @Override
    public String getType()
    {
        return TYPE;
    }

    /**
     * @param documentReference the reference of the document to replicate
     * @param creatorReference the reference of the creator of the document
     */
    public void initialize(DocumentReference documentReference, DocumentReference creatorReference)
    {
        super.initialize(documentReference);

        putMetadata(METADATA_CREATOR, creatorReference);

        this.metadata = Collections.unmodifiableMap(this.metadata);
    }
}
