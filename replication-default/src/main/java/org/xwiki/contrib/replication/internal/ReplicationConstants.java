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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWiki;

/**
 * Various common constants related to replication.
 * 
 * @version $Id$
 */
public interface ReplicationConstants
{
    /**
     * The local reference of the space where all replication related information are stored.
     */
    EntityReference REPLICATION_HOME =
        new EntityReference("Replication", EntityType.SPACE, new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    /**
     * The local reference of the space where all replication related information are stored as a String.
     */
    String REPLICATION_HOME_STRING = XWiki.SYSTEM_SPACE + ".Replication";
}
