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
package org.xwiki.contrib.replication.internal.instance;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.replication.ReplicationInstance;

/**
 * @version $Id$
 */
public class DefaultReplicationInstance implements ReplicationInstance
{
    private final String name;

    private final String uri;

    private Status status;

    /**
     * @param name the display name of the instance
     * @param uri the base URI of the instance (generally of the form https://www.xwiki.org/xwiki/)
     * @param status the status of the instance
     */
    public DefaultReplicationInstance(String name, String uri, Status status)
    {
        this.name = name;
        this.uri = cleanURI(uri);
        this.status = status;
    }

    /**
     * Make sure the URI has the standard format.
     * 
     * @param uri the uri to clean
     * @return the clean version of the URI
     */
    public static String cleanURI(String uri)
    {
        // Cleanup trailing / to avoid empty path element
        return StringUtils.stripEnd(uri, "/");
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getURI()
    {
        return this.uri;
    }

    @Override
    public Status getStatus()
    {
        return this.status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }
}
