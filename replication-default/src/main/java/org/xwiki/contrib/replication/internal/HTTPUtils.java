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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.io.entity.EntityUtils;

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
     * @param response the response to parse
     * @param def the default value
     * @return the error in the response
     */
    public static String getContent(HttpEntityContainer response, String def)
    {
        try {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return def;
        }
    }
}
