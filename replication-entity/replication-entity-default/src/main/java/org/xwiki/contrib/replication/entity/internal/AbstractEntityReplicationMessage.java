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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.internal.ReplicationUtils;
import org.xwiki.model.reference.AbstractLocalizedEntityReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.user.UserReferenceSerializer;

/**
 * @param <E> the type of reference
 * @version $Id$
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractEntityReplicationMessage<E extends EntityReference> implements ReplicationSenderMessage
{
    /**
     * The type of message supported by this receiver.
     */
    public static final String TYPE_PREFIX = "entity_";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE_PREFIX.toUpperCase();

    /**
     * The name of the metadata containing the reference of the entity in the message.
     */
    public static final String METADATA_REFERENCE = METADATA_PREFIX + "REFERENCE";

    /**
     * The name of the metadata containing the locale of the entity in the message.
     */
    public static final String METADATA_LOCALE = METADATA_PREFIX + "LOCALE";

    /**
     * The name of the metadata containing the reference of the user in the context.
     */
    public static final String METADATA_CONTEXT_USER = METADATA_PREFIX + "CONTEXT_USER";

    /**
     * The name of the metadata containing the creator of the document.
     */
    public static final String METADATA_CREATOR = METADATA_PREFIX + "CREATOR";

    @Inject
    protected ConverterManager converter;

    @Inject
    protected UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    protected String id;

    protected String source;

    protected Date date = new Date();

    protected E entityReference;

    protected Map<String, Collection<String>> metadata;

    /**
     * @param entityReference the reference of the document affected by this message
     * @param extraMetadata custom metadata to add to the message
     */
    protected void initialize(E entityReference, Map<String, Collection<String>> extraMetadata)
    {
        this.entityReference = entityReference;

        this.metadata = new HashMap<>();

        if (extraMetadata != null) {
            this.metadata.putAll(extraMetadata);
        }

        // Make sure to use the EntityReference converter (otherwise it won't unserialize to the right type)
        putMetadata(METADATA_REFERENCE, entityReference.getClass() == EntityReference.class ? entityReference
            : new EntityReference(entityReference));

        if (entityReference instanceof AbstractLocalizedEntityReference) {
            putMetadata(METADATA_LOCALE, ((AbstractLocalizedEntityReference) entityReference).getLocale());
        }

        putMetadata(METADATA_CONTEXT_USER, this.documentAccessBridge.getCurrentUserReference());

        // Make sure the id is unique but not too big
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Associate a custom metadata with the message.
     * 
     * @param key the name of the metadata
     * @param value the value of the metadata
     */
    public void putMetadata(String key, Object value)
    {
        String stringValue;
        if (value instanceof Date) {
            stringValue = ReplicationUtils.toString((Date) value);
        } else {
            stringValue = this.converter.convert(String.class, value);
        }

        this.metadata.put(key, Collections.singleton(stringValue));
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public Date getDate()
    {
        return this.date;
    }

    @Override
    public String getSource()
    {
        return this.source;
    }

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        return this.metadata;
    }
}
