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
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationMessageReader;
import org.xwiki.properties.ConverterManager;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultReplicationMessageReader implements ReplicationMessageReader
{
    @Inject
    protected ConverterManager converter;

    @Override
    public String getMetadata(ReplicationMessage message, String key, boolean mandatory)
        throws InvalidReplicationMessageException
    {
        return getMetadata(message, key, mandatory, null);
    }

    @Override
    public <T> T getMetadata(ReplicationMessage message, String key, boolean mandatory, Type type)
        throws InvalidReplicationMessageException
    {
        return getMetadata(message, key, mandatory, type, null);
    }

    @Override
    public <T> T getMetadata(ReplicationMessage message, String key, boolean mandatory, T def)
        throws InvalidReplicationMessageException
    {
        return getMetadata(message, key, mandatory, def != null ? def.getClass() : null, def);
    }

    @Override
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
