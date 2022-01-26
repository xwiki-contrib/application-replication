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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.notification.ReplicationDocumentConflictEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.model.reference.DocumentReference;

/**
 * Create {@link Event} from a {@link ReplicationDocumentConflictEvent}.
 * 
 * @version $Id$
 */
@Component
@Singleton
@Named(ReplicationDocumentConflictEvent.EVENT_NAME)
public class ReplicationDocumentConflictEventConverter implements RecordableEventConverter
{
    /**
     * Global prefix to be used for any parameters in events related to replication.
     */
    public static final String REPLICATION_PREFIX_PARAMETER_KEY = "replication.";

    /**
     * Global prefix to be used for any parameters in events related to replication document conflict.
     */
    public static final String REPLICATION_DOCUMENTCONFLICT_PREFIX_PARAMETER_KEY =
        REPLICATION_PREFIX_PARAMETER_KEY + "documentconflict.";

    // FIXME: not very happy with this hack
    private static final DocumentReference REPLICATION_USER = new DocumentReference("xwiki", "XWiki", "__replication");

    @Inject
    private RecordableEventConverter defaultConverter;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        Event result = this.defaultConverter.convert(recordableEvent, source, data);

        ReplicationDocumentConflictEvent conflictEvent = (ReplicationDocumentConflictEvent) recordableEvent;

        result.setUser(REPLICATION_USER);
        result.setType(ReplicationDocumentConflictEvent.EVENT_NAME);
        result.setRelatedEntity(conflictEvent.getDocumentReference());

        return result;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return Arrays.asList(new ReplicationDocumentConflictEvent());
    }
}
