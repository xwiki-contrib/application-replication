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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.properties.converter.AbstractConverter;

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
        if (value == null || (value instanceof String && StringUtils.isEmpty((String) value))) {
            return null;
        }

        return DocumentReplicationLevel.valueOf((String) value);
    }

    /**
     * @param value the value to convert
     * @param instances the {@link ReplicationInstanceManager} component to find registered instances
     * @return the {@link DocumentReplicationControllerInstance} created from the passed value
     */
    public static DocumentReplicationControllerInstance toControllerInstance(Object value,
        ReplicationInstanceManager instances)
    {
        if (value != null) {
            String valueString = value.toString();

            int index = valueString.indexOf(':');

            DocumentReplicationLevel level;
            if (index > 0) {
                level = toLevel(valueString.substring(0, index));
            } else {
                level = null;
            }

            ReplicationInstance instance;
            if ((index + 1) < valueString.length()) {
                instance = instances.getInstance(valueString.substring(index + 1));
                if (instance == null) {
                    return null;
                }
            } else {
                instance = null;
            }

            return new DocumentReplicationControllerInstance(instance, level);
        }

        return null;
    }

    /**
     * @param values the values to convert
     * @param instances the {@link ReplicationInstanceManager} component to find registered instances
     * @return the list of {@link DocumentReplicationControllerInstance}s created from the passed value
     */
    public static List<DocumentReplicationControllerInstance> toControllerInstances(Collection<?> values,
        ReplicationInstanceManager instances)
    {
        if (values == null) {
            return null;
        }

        return values.stream().map(v -> toControllerInstance(v, instances)).filter(Objects::nonNull)
            .collect(Collectors.toList());
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

        return values.stream().map(DocumentReplicationControllerInstance::toString).collect(Collectors.toList());
    }

    @Override
    protected DocumentReplicationControllerInstance convertToType(Type targetType, Object value)
    {
        return toControllerInstance(value, this.manager);
    }

    @Override
    protected String convertToString(DocumentReplicationControllerInstance value)
    {
        return toString(value);
    }
}
