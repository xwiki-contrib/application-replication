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

import java.util.Date;

/**
 * @version $Id$
 */
public class DocumentAncestor
{
    private final String version;

    private final Date date;

    /**
     * @param version the version of the ancestor
     * @param date the date of the ancestor
     */
    public DocumentAncestor(String version, Date date)
    {
        this.version = version;
        this.date = date;
    }

    /**
     * @return the version of the ancestor
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * @return the date of the ancestor
     */
    public Date getDate()
    {
        return date;
    }
}
