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
package org.xwiki.contrib.replication.internal.enpoint;

import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Dummy type for Replication entry point.
 *
 * @version $Id: 97638fe25bc709cd9296ea452b5d13077aab014b $
 */
public class ReplicationResourceReference extends AbstractResourceReference
{
    /**
     * Represents a Replication Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType(ReplicationResourceReferenceHandler.HINT);

    private String path;

    /**
     * @param path the path after the main endpoint
     */
    public ReplicationResourceReference(String path)
    {
        setType(TYPE);

        this.path = path;
    }

    /**
     * @return the path starting with the endpoint
     */
    public String getPath()
    {
        return this.path;
    }

    @Override
    public String toString()
    {
        XWikiToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.appendSuper(super.toString());
        builder.append("path", getPath());

        return builder.toString();
    }
}
