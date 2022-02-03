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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

/**
 * Convert a {@link DocumentReplicationControllerInstance} from/to a String.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DocumentReplicationControllerInstanceConverter
    extends AbstractConverter<DocumentReplicationControllerInstance>
{
    @Inject
    private ReplicationInstanceManager manager;

    /**
     * @param value the serialized level
     * @return the actual level
     */
    public static DocumentReplicationLevel toLevel(Object value)
    {
        if (value == null) {
            return null;
        }

        if (value instanceof DocumentReplicationLevel) {
            return (DocumentReplicationLevel) value;
        }

        String valueString = value.toString();

        if (StringUtils.isEmpty(valueString)) {
            return null;
        }

        return DocumentReplicationLevel.valueOf(((String) value).toUpperCase());
    }

    /**
     * @param value the value to convert
     * @param instances the {@link ReplicationInstanceManager} component to find registered instances
     * @return the {@link DocumentReplicationControllerInstance} created from the passed value
     * @throws ReplicationException when failing to get registered instances
     */
    public static DocumentReplicationControllerInstance toControllerInstance(Object value,
        ReplicationInstanceManager instances) throws ReplicationException
    {
        DocumentReplicationControllerInstance instance = null;

        if (value != null) {
            if (value instanceof Map) {
                instance = toControllerInstance((Map) value, instances);
            } else {
                instance = toControllerInstance(value.toString(), instances);
            }
        }

        return instance;
    }

    /**
     * @param value the value to convert
     * @param instances the {@link ReplicationInstanceManager} component to find registered instances
     * @return the {@link DocumentReplicationControllerInstance} created from the passed value
     * @throws ReplicationException when failing to get registered instances
     */
    public static DocumentReplicationControllerInstance toControllerInstance(Map value,
        ReplicationInstanceManager instances) throws ReplicationException
    {
        // Level
        DocumentReplicationLevel level = toLevel(value.get("level"));

        // Read only
        boolean readonly = false;
        Object valueReadOnly = value.get("readonly");
        if (valueReadOnly != null) {
            if (valueReadOnly instanceof Boolean) {
                readonly = ((Boolean) valueReadOnly).booleanValue();
            } else {
                readonly = Boolean.parseBoolean(valueReadOnly.toString());
            }
        }

        // Instance
        ReplicationInstance instance = null;
        Object valueInstance = value.get("instance");
        if (valueInstance != null) {
            if (valueInstance instanceof ReplicationInstance) {
                instance = (ReplicationInstance) valueInstance;
            } else {
                String valueString = valueInstance.toString();
                if (StringUtils.isNotEmpty(valueString)) {
                    instance = instances.getInstanceByURI(valueInstance.toString());
                    if (instance == null) {
                        // Not a valid instance
                        return null;
                    }
                }
            }
        }

        return new DocumentReplicationControllerInstance(instance, level, readonly);
    }

    /**
     * @param value the value to convert
     * @param instances the {@link ReplicationInstanceManager} component to find registered instances
     * @return the {@link DocumentReplicationControllerInstance} created from the passed value
     * @throws ReplicationException when failing to get registered instances
     */
    public static DocumentReplicationControllerInstance toControllerInstance(String value,
        ReplicationInstanceManager instances) throws ReplicationException
    {
        int index = value.indexOf(':');

        // Level
        DocumentReplicationLevel level;
        if (index > 0) {
            level = toLevel(value.substring(0, index));
        } else {
            level = null;
        }

        // Read only
        boolean readonly = false;
        if (index > 0) {
            int readonlyIndex = value.indexOf(':', index + 1);

            if (readonlyIndex > 0) {
                readonly = Boolean.parseBoolean(value.substring(index + 1, readonlyIndex));
                index = readonlyIndex;
            }
        }

        // Instance
        ReplicationInstance instance = null;
        if ((index + 1) < value.length()) {
            instance = instances.getInstanceByURI(value.substring(index + 1));
            if (instance == null) {
                // Not a valid instance
                return null;
            }
        }

        return new DocumentReplicationControllerInstance(instance, level, readonly);
    }

    /**
     * @param values the values to convert
     * @param instances the {@link ReplicationInstanceManager} component to find registered instances
     * @return the list of {@link DocumentReplicationControllerInstance}s created from the passed value
     * @throws ReplicationException when failing to access instances
     */
    public static List<DocumentReplicationControllerInstance> toControllerInstances(Collection<?> values,
        ReplicationInstanceManager instances) throws ReplicationException
    {
        if (values == null) {
            return null;
        }

        List<DocumentReplicationControllerInstance> result = new ArrayList<>(values.size());
        for (Object value : values) {
            DocumentReplicationControllerInstance instance = toControllerInstance(value, instances);

            if (instance != null) {
                result.add(instance);
            }
        }

        return result;
    }

    /**
     * @param value the {@link DocumentReplicationControllerInstance} to serialize
     * @return the String version of an {@link DocumentReplicationControllerInstance}
     */
    public static String toString(DocumentReplicationControllerInstance value)
    {
        StringBuilder builder = new StringBuilder();

        if (value.getLevel() != null) {
            builder.append(value.getLevel());
        }
        builder.append(':');
        builder.append(value.isReadonly());
        builder.append(':');
        if (value.getInstance() != null) {
            builder.append(value.getInstance().getURI());
        }

        return builder.toString();
    }

    /**
     * @param values the list of {@link DocumentReplicationControllerInstance}s to serialize
     * @return the String version of an {@link DocumentReplicationControllerInstance}s list
     */
    public static List<String> toStrings(Collection<DocumentReplicationControllerInstance> values)
    {
        if (values == null) {
            return null;
        }

        return values.stream().map(DocumentReplicationControllerInstanceConverter::toString)
            .collect(Collectors.toList());
    }

    @Override
    protected DocumentReplicationControllerInstance convertToType(Type targetType, Object value)
    {
        try {
            return toControllerInstance(value, this.manager);
        } catch (ReplicationException e) {
            throw new ConversionException("Failed to get instances", e);
        }
    }

    @Override
    protected String convertToString(DocumentReplicationControllerInstance value)
    {
        return toString(value);
    }
}
