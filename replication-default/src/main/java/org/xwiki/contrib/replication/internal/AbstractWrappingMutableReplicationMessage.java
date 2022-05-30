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

import org.xwiki.contrib.replication.MutableReplicationMessage;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.properties.ConverterManager;

/**
 * @param <M> the type of {@link ReplicationMessage}
 * @version $Id$
 */
public abstract class AbstractWrappingMutableReplicationMessage<M extends ReplicationMessage>
    implements MutableReplicationMessage
{
    protected final M message;

    private final Map<String, Collection<String>> metadata;

    private final ConverterManager converter;

    /**
     * @param message the message to wrap
     * @param converter used to convert values to String
     */
    protected AbstractWrappingMutableReplicationMessage(M message, ConverterManager converter)
    {
        this.converter = converter;
        this.message = message;

        this.metadata = new HashMap<>(message.getCustomMetadata());
    }

    @Override
    public String getId()
    {
        return this.message.getId();
    }

    @Override
    public Date getDate()
    {
        return this.message.getDate();
    }

    @Override
    public String getSource()
    {
        return this.message.getSource();
    }

    @Override
    public String getType()
    {
        return this.message.getType();
    }

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        return this.metadata;
    }

    @Override
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
}
