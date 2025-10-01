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
package org.xwiki.contrib.replication.internal.message;

import java.io.Serializable;

import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.observation.event.Event;

/**
 * Event triggered by some receiver to inform listeners that a message has been received.
 * <p>
 * The following information are also sent:
 * <ul>
 * <li>source: the {@link ReplicationReceiverMessage}</li>
 * <li>data: null</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.3.0
 */
public class ReplicationReceiverMessageEvent implements Event, Serializable
{
    private static final long serialVersionUID = 1L;

    private String type;

    /**
     * Matches all types of messages.
     */
    public ReplicationReceiverMessageEvent()
    {
    }

    /**
     * @param type the type of messages
     */
    public ReplicationReceiverMessageEvent(String type)
    {
        this.type = type;
    }

    /**
     * @return the type of message
     */
    public String getType()
    {
        return this.type;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ReplicationReceiverMessageEvent;
    }
}
