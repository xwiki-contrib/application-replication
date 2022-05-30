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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.properties.ConverterManager;

/**
 * @version $Id$
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractReplicationMessage implements ReplicationMessage
{
    protected final Map<String, Collection<String>> modifiableMap = new HashMap<>();

    protected final Map<String, Collection<String>> unmodifiableMap = Collections.unmodifiableMap(this.modifiableMap);

    @Inject
    private ConverterManager converter;

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        return this.unmodifiableMap;
    }

    /**
     * Associate a custom metadata with the message.
     * 
     * @param key the name of the metadata
     * @param value the value of the metadata
     */
    protected void putMetadata(String key, Object value)
    {
        if (value == null) {
            this.modifiableMap.remove(key);
        } else if (value instanceof Iterable) {
            List<String> listValue = new ArrayList<>();
            for (Object element : (Iterable) value) {
                listValue.add(toString(element));
            }
            this.modifiableMap.put(key, Collections.unmodifiableList(listValue));
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<String> listValue = new ArrayList<>(length);
            for (int i = 0; i < length; ++i) {
                listValue.add(toString(Array.get(value, i)));
            }
            this.modifiableMap.put(key, Collections.unmodifiableList(listValue));
        } else {
            this.modifiableMap.put(key, Collections.singleton(toString(value)));
        }
    }

    protected String toString(Object value)
    {
        String stringValue;
        if (value instanceof Date) {
            stringValue = ReplicationUtils.toString((Date) value);
        } else {
            stringValue = this.converter.convert(String.class, value);
        }

        return stringValue;
    }
}
