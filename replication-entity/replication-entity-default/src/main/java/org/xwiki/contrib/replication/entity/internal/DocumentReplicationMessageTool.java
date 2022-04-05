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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.update.DocumentUpdateReplicationMessage;
import org.xwiki.contrib.replication.internal.ReplicationUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.properties.ConverterManager;

/**
 * @version $Id$
 */
@Component(roles = DocumentReplicationMessageTool.class)
@Singleton
public class DocumentReplicationMessageTool
{
    @Inject
    protected ConverterManager converter;

    @Inject
    @Named("current")
    protected EntityReferenceResolver<EntityReference> currentEntityResolver;

    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> currentDocumentResolver;

    /**
     * @param message the received message
     * @return the document associated with the message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public DocumentReference getDocumentReference(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return getDocumentReference(message, getEntityReference(message));
    }

    /**
     * @param message the received message
     * @param reference the reference of the entity (without the locale)
     * @return the document associated with the message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public DocumentReference getDocumentReference(ReplicationMessage message, EntityReference reference)
        throws InvalidReplicationMessageException
    {
        Locale locale = getMetadata(message, AbstractEntityReplicationMessage.METADATA_LOCALE, true, Locale.class);

        return new DocumentReference(reference, locale);
    }

    /**
     * @param message the received message
     * @return the entity associated with the message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public EntityReference getEntityReference(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return getEntityReference(message, true);
    }

    /**
     * @param message the received message
     * @param mandatory true of the property is mandatory
     * @return the entity associated with the message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public EntityReference getEntityReference(ReplicationMessage message, boolean mandatory)
        throws InvalidReplicationMessageException
    {
        EntityReference reference =
            getMetadata(message, AbstractEntityReplicationMessage.METADATA_REFERENCE, mandatory, EntityReference.class);

        return this.currentEntityResolver.resolve(reference, reference.getType());
    }

    /**
     * @param message the received message
     * @return the user reference from the context when the message was created
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public DocumentReference getContextUser(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return getMetadata(message, AbstractEntityReplicationMessage.METADATA_CONTEXT_USER, false,
            DocumentReference.class);
    }

    /**
     * @param message the received message
     * @return true if the document in the message is complete
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public boolean isComplete(ReplicationMessage message) throws InvalidReplicationMessageException
    {
        return BooleanUtils.toBoolean(getMetadata(message, DocumentUpdateReplicationMessage.METADATA_COMPLETE, false));
    }

    /**
     * @param message the received message
     * @param key the key metadata in the message
     * @param mandatory true of the property is mandatory
     * @return the metadata value
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public String getMetadata(ReplicationMessage message, String key, boolean mandatory)
        throws InvalidReplicationMessageException
    {
        return getMetadata(message, key, mandatory, null);
    }

    /**
     * @param <T> the type of the metadata
     * @param message the received message
     * @param key the key metadata in the message
     * @param mandatory true of the property is mandatory
     * @param type the type to convert the metadata to
     * @return the metadata value
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public <T> T getMetadata(ReplicationMessage message, String key, boolean mandatory, Type type)
        throws InvalidReplicationMessageException
    {
        return getMetadata(message, key, mandatory, type, null);
    }

    /**
     * @param <T> the type of the metadata
     * @param message the received message
     * @param key the key metadata in the message
     * @param mandatory true of the property is mandatory
     * @param def the default value to return if none could be found
     * @return the metadata value
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public <T> T getMetadata(ReplicationMessage message, String key, boolean mandatory, T def)
        throws InvalidReplicationMessageException
    {
        return getMetadata(message, key, mandatory, def != null ? def.getClass() : null, def);
    }

    /**
     * @param <T> the type of the metadata
     * @param message the received message
     * @param key the key metadata in the message
     * @param mandatory true of the property is mandatory
     * @param type the type to convert the metadata to
     * @param def the default value to return if none could be found
     * @return the metadata value
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public <T> T getMetadata(ReplicationMessage message, String key, boolean mandatory, Type type, T def)
        throws InvalidReplicationMessageException
    {
        Collection<String> values = message.getCustomMetadata().get(key);

        if (CollectionUtils.isEmpty(values)) {
            if (mandatory) {
                throw new InvalidReplicationMessageException("Received an invalid document message with id ["
                    + message.getId() + "]: missing mandatory metadata [" + key + "]");
            } else {
                return def;
            }
        }

        String value = values.iterator().next();

        if (type != null) {
            if (type == Date.class) {
                // Standard Date converter does not support Date -> String -> Date
                return value != null ? (T) ReplicationUtils.toDate(value) : null;
            } else {
                return this.converter.convert(type, value);
            }
        }

        return (T) value;
    }
}
