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
package org.xwiki.contrib.replication.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;

/**
 * @version $Id$
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractDocumentReplicationMessage implements ReplicationSenderMessage
{
    /**
     * The type of message supported by this receiver.
     */
    public static final String TYPE_PREFIX = "entity_document_";

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

    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> localSerializer;

    @Inject
    @Named("uid")
    protected EntityReferenceSerializer<String> uidSerializer;

    @Inject
    protected ConverterManager converter;

    protected final Date date = new Date();

    protected DocumentReference documentReference;

    protected String id;

    protected Map<String, Collection<String>> metadata;

    /**
     * @param documentReference the reference of the document affected by this message
     */
    public void initialize(DocumentReference documentReference)
    {
        this.documentReference = documentReference;

        this.metadata = new HashMap<>();
        putMetadata(METADATA_REFERENCE, documentReference);
        putMetadata(METADATA_LOCALE, documentReference.getLocale());

        this.id = getType() + '/' + getDate().getTime() + '/' + this.uidSerializer.serialize(documentReference);
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
            stringValue = String.valueOf(((Date) value).getTime());
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
    public ReplicationInstance getSource()
    {
        // Will be filled by the sender
        return null;
    }

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        return this.metadata;
    }
}
