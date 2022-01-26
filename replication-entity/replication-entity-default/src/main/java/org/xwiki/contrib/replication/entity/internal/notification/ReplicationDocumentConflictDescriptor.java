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
package org.xwiki.contrib.replication.entity.internal.notification;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.notification.ReplicationDocumentConflictEvent;
import org.xwiki.eventstream.RecordableEventDescriptor;

/**
 * Descriptor allowing to enable notification for {@link ReplicationDocumentConflictEvent}.
 * 
 * @version $Id$
 */
@Component
@Named(ReplicationDocumentConflictEvent.EVENT_NAME)
@Singleton
public class ReplicationDocumentConflictDescriptor implements RecordableEventDescriptor
{
    @Override
    public String getEventType()
    {
        return ReplicationDocumentConflictEvent.EVENT_NAME;
    }

    @Override
    public String getDescription()
    {
        return "replication.event.documentconflict.description";
    }

    @Override
    public String getApplicationName()
    {
        return "replication.application.name";
    }

    @Override
    public String getApplicationIcon()
    {
        return "arrows";
    }
}
