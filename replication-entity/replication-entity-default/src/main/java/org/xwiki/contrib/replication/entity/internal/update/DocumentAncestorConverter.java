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
package org.xwiki.contrib.replication.entity.internal.update;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.internal.ReplicationUtils;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

/**
 * Convert a {@link DocumentAncestor} from/to a String.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DocumentAncestorConverter extends AbstractConverter<DocumentAncestor>
{
    /**
     * @param value the value to convert
     * @return the {@link DocumentAncestor} created from the passed value
     * @throws ReplicationException when failing to get registered instances
     */
    public static DocumentAncestor toDocumentAncestor(Object value) throws ReplicationException
    {
        DocumentAncestor instance = null;

        if (value != null) {
            instance = toDocumentAncestor(value.toString());
        }

        return instance;
    }

    /**
     * @param value the value to convert
     * @return the {@link DocumentAncestor} created from the passed value
     * @throws ReplicationException when failing to get registered instances
     */
    public static DocumentAncestor toDocumentAncestor(String value) throws ReplicationException
    {
        int index = value.indexOf(':');

        // Version
        String version = value.substring(0, index);

        // Date
        Date date = ReplicationUtils.toDate(value.substring(index + 1));

        return new DocumentAncestor(version, date);
    }

    /**
     * @param values the values to convert
     * @return the list of {@link DocumentAncestor}s created from the passed value
     * @throws ReplicationException when failing to access instances
     */
    public static List<DocumentAncestor> toDocumentAncestors(Collection<?> values) throws ReplicationException
    {
        if (values == null) {
            return null;
        }

        List<DocumentAncestor> result = new ArrayList<>(values.size());
        for (Object value : values) {
            DocumentAncestor instance = toDocumentAncestor(value);

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
    public static String toString(DocumentAncestor value)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(value.getVersion());
        builder.append(':');
        builder.append(ReplicationUtils.toString(value.getDate()));

        return builder.toString();
    }

    /**
     * @param values the list of {@link DocumentReplicationControllerInstance}s to serialize
     * @return the String version of an {@link DocumentReplicationControllerInstance}s list
     */
    public static List<String> toStrings(Collection<DocumentAncestor> values)
    {
        if (values == null) {
            return null;
        }

        return values.stream().map(DocumentAncestorConverter::toString).collect(Collectors.toList());
    }

    @Override
    protected DocumentAncestor convertToType(Type targetType, Object value)
    {
        try {
            return toDocumentAncestor(value);
        } catch (ReplicationException e) {
            throw new ConversionException("Failed to get instances", e);
        }
    }

    @Override
    protected String convertToString(DocumentAncestor value)
    {
        return toString(value);
    }
}
