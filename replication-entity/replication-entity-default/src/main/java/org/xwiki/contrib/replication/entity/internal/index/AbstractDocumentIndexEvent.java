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

import java.io.Serializable;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

/**
 * Base class for all document index related events.
 * 
 * @version $Id: eb7140cbd432aebb0e6c50bf1607a8472880a3c1 $
 * @since 1.12.0
 */
public abstract class AbstractDocumentIndexEvent implements Event, Serializable
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    private final DocumentReference reference;

    /**
     * This event will match any other document index event of the same type.
     */
    protected AbstractDocumentIndexEvent()
    {
        this(null);
    }

    /**
     * This event will match only events impacting a specific document.
     * 
     * @param reference the reference of the document
     */
    protected AbstractDocumentIndexEvent(DocumentReference reference)
    {
        this.reference = reference;
    }

    /**
     * @return the reference
     */
    public DocumentReference getReference()
    {
        return this.reference;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent != null && otherEvent.getClass() == getClass()) {
            return getReference() == null
                || getReference().equals(((AbstractDocumentIndexEvent) otherEvent).getReference());
        }

        return false;
    }
}
