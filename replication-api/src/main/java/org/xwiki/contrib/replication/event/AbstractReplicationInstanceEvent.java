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
package org.xwiki.contrib.replication.event;

import java.util.Objects;

import org.xwiki.observation.event.Event;

/**
 * Base class for instances modifications events.
 * 
 * @version $Id$
 */
public abstract class AbstractReplicationInstanceEvent implements Event
{
    private final String uri;

    /**
     * Matches everything.
     */
    AbstractReplicationInstanceEvent()
    {
        this.uri = null;
    }

    /**
     * @param uri the uri of the instance
     */
    AbstractReplicationInstanceEvent(String uri)
    {
        this.uri = uri;
    }

    /**
     * @return the uri of the instance
     */
    public String getURI()
    {
        return this.uri;
    }

    @Override
    public int hashCode()
    {
        return getURI().hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }

        if (object == this) {
            return true;
        }

        if (this.getClass() == object.getClass()) {
            return Objects.equals(getURI(), ((AbstractReplicationInstanceEvent) object).getURI());
        }

        return getURI().equals(object);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent != null && otherEvent.getClass() == this.getClass()) {
            return getURI() == null || getURI().equals(((AbstractReplicationInstanceEvent) otherEvent).getURI());
        }

        return false;
    }
}
