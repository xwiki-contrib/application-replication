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
package org.xwiki.contrib.replication.entity.notification;

import java.util.Set;

import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.TargetableEvent;
import org.xwiki.model.reference.DocumentReference;

/**
 * Recordable even used to notify that a conflict has been resolved on a document.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: "replication"</li>
 * <li>data: the current {com.xpn.xwiki.doc.XWikiDocument} instance</li>
 * </ul>
 * 
 * @version $Id$
 */
public class ReplicationDocumentConflictEvent implements RecordableEvent, TargetableEvent
{
    /**
     * Event name to be used in the components referring to that event.
     */
    public static final String EVENT_NAME = "replication.documentconflict";

    private final DocumentReference documentReference;

    private final Set<String> targets;

    /**
     * Matches all ReplicationDocumentConflict.
     */
    public ReplicationDocumentConflictEvent()
    {
        this(null, null);
    }

    /**
     * @param documentReference the reference of the conflicting document
     * @param authors the users involved in the conflict
     */
    public ReplicationDocumentConflictEvent(DocumentReference documentReference, Set<String> authors)
    {
        this.documentReference = documentReference;
        this.targets = authors;
    }

    /**
     * @return the reference of the conflicting document
     */
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public Set<String> getTarget()
    {
        return this.targets;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent == this) {
            return true;
        }

        return otherEvent instanceof ReplicationDocumentConflictEvent
            && matchesReference(((ReplicationDocumentConflictEvent) otherEvent).getDocumentReference());
    }

    /**
     * Try to match the provided reference.
     *
     * @param otherReference the reference to match
     * @return true if the provided reference is matched
     */
    private boolean matchesReference(DocumentReference otherReference)
    {
        return getDocumentReference() == null || getDocumentReference().equals(otherReference);
    }
}
