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
package org.xwiki.contrib.replication.entity.internal.index;

import org.xwiki.model.reference.DocumentReference;

/**
 * An event triggered after a replicated document conflict state is updated.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: null</li>
 * <li>data: null</li>
 * </ul>
 * 
 * @version $Id: 35bb9c21466d3392388103b72f40a8844758165d $
 * @since 1.12.0
 */
public class DocumentConflictUpdatedEvent extends AbstractDocumentIndexEvent
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    private final Boolean conflict;

    /**
     * Matches all {@link DocumentConflictUpdatedEvent} events.
     */
    public DocumentConflictUpdatedEvent()
    {
        this(null, null);
    }

    /**
     * @param reference the reference of the document
     * @param conflict true if the document is in conflict
     */
    public DocumentConflictUpdatedEvent(DocumentReference reference, Boolean conflict)
    {
        super(reference);

        this.conflict = conflict;
    }

    /**
     * @return true if the document is in conflict
     */
    public Boolean isConflict()
    {
        return this.conflict;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (super.matches(otherEvent)) {
            return isConflict() == null
                || isConflict().equals(((DocumentConflictUpdatedEvent) otherEvent).isConflict());
        }

        return false;
    }
}
