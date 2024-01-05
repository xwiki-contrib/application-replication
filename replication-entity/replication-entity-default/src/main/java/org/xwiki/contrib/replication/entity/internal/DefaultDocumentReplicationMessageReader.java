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

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.BooleanUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationMessageReader;
import org.xwiki.contrib.replication.entity.DocumentReplicationMessageReader;
import org.xwiki.contrib.replication.entity.internal.update.DocumentUpdateReplicationMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.user.UserReference;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultDocumentReplicationMessageReader implements DocumentReplicationMessageReader
{
    @Inject
    private ReplicationMessageReader reader;

    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentEntityResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentResolver;

    @Override
    public DocumentReference getDocumentReference(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return getDocumentReference(message, getEntityReference(message));
    }

    @Override
    public DocumentReference getDocumentReference(ReplicationMessage message, EntityReference reference)
        throws InvalidReplicationMessageException
    {
        Locale locale = this.reader.getMetadata(message, AbstractEntityReplicationMessage.METADATA_ENTITY_LOCALE, true,
            Locale.class);

        return new DocumentReference(reference, locale);
    }

    @Override
    public EntityReference getEntityReference(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return getEntityReference(message, true);
    }

    @Override
    public EntityReference getEntityReference(ReplicationMessage message, boolean mandatory)
        throws InvalidReplicationMessageException
    {
        EntityReference reference = this.reader.getMetadata(message,
            AbstractEntityReplicationMessage.METADATA_ENTITY_REFERENCE, mandatory, EntityReference.class);

        return reference != null ? this.currentEntityResolver.resolve(reference, reference.getType()) : null;
    }

    @Override
    public DocumentReference getContextUser(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return this.reader.getMetadata(message, AbstractEntityReplicationMessage.METADATA_ENTITY_CONTEXT_USER, false,
            DocumentReference.class);
    }

    @Override
    public String getDocumentVersion(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return this.reader.getMetadata(message, DocumentUpdateReplicationMessage.METADATA_DOCUMENT_UPDATE_VERSION,
            false);
    }

    @Override
    public boolean isComplete(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return BooleanUtils.toBoolean(this.reader.getMetadata(message,
            DocumentUpdateReplicationMessage.METADATA_DOCUMENT_UPDATE_COMPLETE, false));
    }

    @Override
    public UserReference getCreatorReference(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return this.reader.getMetadata(message, AbstractEntityReplicationMessage.METADATA_ENTITY_CREATOR, false,
            UserReference.class);
    }
}
