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
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.internal.AbstractNoContentEntityReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.DocumentReplicationMessageTool;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

/**
 * @version $Id$
 */
@Component(roles = DocumentReferenceReplicationMessage.class)
public class DocumentReferenceReplicationMessage extends AbstractNoContentEntityReplicationMessage<DocumentReference>
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
     * The name of the metadata indicating if the documented just started to be replicated.
     */
    public static final String METADATA_CREATE = METADATA_PREFIX + "CREATE";

    @Inject
    private DocumentReplicationMessageTool documentMessageTool;

    @Override
    public String getType()
    {
        return TYPE;
    }

    /**
     * @param documentReference the reference of the document to replicate
     * @param creatorReference the reference of the creator of the document
     * @param create true if the documented just started to be replicated
     * @param metadata custom metadata to add to the message
     */
    public void initialize(DocumentReference documentReference, UserReference creatorReference, boolean create,
        Map<String, Collection<String>> metadata)
    {
        super.initialize(documentReference, metadata);

        putMetadata(METADATA_CREATOR, creatorReference);
        putMetadata(METADATA_CREATE, create);

        this.metadata = Collections.unmodifiableMap(this.metadata);
    }

    /**
     * @param message the document update message to convert
     * @throws InvalidReplicationMessageException when the document update message is invalid
     */
    public void initialize(ReplicationReceiverMessage message) throws InvalidReplicationMessageException
    {
        initialize(this.documentMessageTool.getDocumentReference(message),
            this.documentMessageTool.getMetadata(message, METADATA_CREATOR, true, UserReference.class),
            this.documentMessageTool.getMetadata(message, METADATA_CREATOR, true, true), message.getCustomMetadata());

        // Relay the source information
        this.id = message.getId();
        this.source = message.getSource();
        this.date = message.getDate();
    }
}
