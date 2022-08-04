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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntityContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @version $Id$
 */
public final class HTTPUtils
{
    private HTTPUtils()
    {
    }

    /**
     * @param value the value as {@link String}
     * @return the value as {@link List}
     */
    public static List<String> toList(String value)
    {
        List<String> list = new ArrayList<>();

        boolean escaped = false;

        StringBuilder currentElement = new StringBuilder();
        for (int i = 0; i < value.length(); ++i) {
            char currentChar = value.charAt(i);

            if (escaped) {
                currentElement.append(currentChar);

                // Reset escaped flag
                escaped = false;
            } else {
                if (currentChar == '\\') {
                    // Next character is escaped
                    escaped = true;
                } else if (currentChar == '|') {
                    // Close the current element
                    list.add(currentElement.toString());

                    // Reset the builder
                    currentElement.setLength(0);
                } else {
                    currentElement.append(currentChar);
                }
            }
        }

        // The the last element
        list.add(currentElement.toString());

        return list;
    }

    /**
     * @param list the value as {@link List}
     * @return the value as {@link String}
     */
    public static String toString(Collection<String> list)
    {
        StringBuilder builder = new StringBuilder();

        boolean empty = true;
        for (String element : list) {
            if (!empty) {
                builder.append('|');
            }

            builder.append(element.replace("\\", "\\\\").replace("|", "\\|"));

            empty = false;
        }

        return builder.toString();
    }

    /**
     * @param map the map to serialize
     * @return the JSON version of the map
     * @throws JsonProcessingException when failing to serialize the map to JSON
     */
    public static String toJSON(Map<String, Object> map) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(map);
    }

    /**
     * @param json the JSON version to parse
     * @return the unserialized map
     * @throws IOException when failing to parse the json input
     */
    public static Map<String, Object> fromJSON(String json) throws IOException
    {
        if (StringUtils.isEmpty(json)) {
            return null;
        }

        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>()
        {
        };
        return new ObjectMapper().readValue(json, typeRef);
    }

    /**
     * @param entity the HTTP response to parse
     * @return the unserialized map
     * @throws IOException when failing to parse the json input
     */
    public static Map<String, Object> fromJSON(HttpEntityContainer entity) throws IOException
    {
        if (entity.getEntity().getContentLength() > 0) {
            try (InputStream stream = entity.getEntity().getContent()) {
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>()
                {
                };
                return new ObjectMapper().readValue(stream, typeRef);
            }
        }

        return Collections.emptyMap();
    }
}
