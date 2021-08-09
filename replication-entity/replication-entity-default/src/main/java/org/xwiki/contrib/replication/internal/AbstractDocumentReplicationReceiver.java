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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationReceiver;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.properties.ConverterManager;

/**
 * @version $Id$
 */
public abstract class AbstractDocumentReplicationReceiver implements ReplicationReceiver
{
    @Inject
    @Named("current")
    protected EntityReferenceResolver<String> currentEntityResolver;

    @Inject
    protected ConverterManager converter;

    @Inject
    protected Logger logger;

    protected DocumentReference getDocumentReference(ReplicationReceiverMessage message)
        throws InvalidReplicationMessageException
    {
        String referenceString = getMetadata(message, AbstractDocumentReplicationMessage.METADATA_REFERENCE, true);
        Locale locale = getMetadata(message, AbstractDocumentReplicationMessage.METADATA_LOCALE, true, Locale.class);

        return new DocumentReference(this.currentEntityResolver.resolve(referenceString, EntityType.DOCUMENT), locale);
    }

    protected String getMetadata(ReplicationReceiverMessage message, String key, boolean mandatory)
        throws InvalidReplicationMessageException
    {
        return getMetadata(message, key, mandatory, null);
    }

    protected <T> T getMetadata(ReplicationReceiverMessage message, String key, boolean mandatory, Type type)
        throws InvalidReplicationMessageException
    {
        Collection<String> values = message.getCustomMetadata().get(key);

        if (CollectionUtils.isEmpty(values)) {
            if (mandatory) {
                throw new InvalidReplicationMessageException("Received an invalid document message with id ["
                    + message.getId() + "]: missing mandatory metadata [" + key + "]");
            } else {
                return null;
            }
        }

        String value = values.iterator().next();

        if (type != null) {
            return this.converter.convert(type, value);
        }

        return (T) value;
    }
}
