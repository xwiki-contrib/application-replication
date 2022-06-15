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

import org.apache.commons.collections4.map.AbstractHashedMap;

/**
 * Force upper case of the keys.
 * 
 * @param <V> the type of the values in this map
 * @version $Id$
 */
public class UpperCaseMap<V> extends AbstractHashedMap<String, V>
{
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    /**
     * Constructs an empty {@code UpperCaseMap} with the default initial capacity (16).
     */
    public UpperCaseMap()
    {
        super(DEFAULT_INITIAL_CAPACITY);
    }

    @Override
    protected Object convertKey(Object key)
    {
        return key != null ? ((String) key).toUpperCase() : NULL;
    }
}
