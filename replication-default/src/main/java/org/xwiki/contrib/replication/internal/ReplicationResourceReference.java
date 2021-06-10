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

import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;

/**
 * The reference of an replication message.
 * 
 * @version $Id: a1c421eb909c6540119e266dd77ec1fc2f50f62c $
 */
public class ReplicationResourceReference extends AbstractResourceReference
{
    private final String dataType;

    private final String source;

    /**
     * Default constructor.
     * 
     * @param type see {@link #getType()}
     * @param dataType the type of data received
     * @param source the source which sent the data
     */
    public ReplicationResourceReference(ResourceType type, String dataType, String source)
    {
        setType(type);

        this.dataType = dataType;
        this.source = source;
    }

    /**
     * @return the dataType
     */
    public String getDataType()
    {
        return this.dataType;
    }

    /**
     * @return the source
     */
    public String getSource()
    {
        return this.source;
    }
}
