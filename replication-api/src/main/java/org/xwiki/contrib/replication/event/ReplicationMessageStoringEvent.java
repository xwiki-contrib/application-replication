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

import org.xwiki.contrib.replication.MutableReplicationMessage;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.observation.event.AbstractCancelableEvent;

/**
 * Event sent before {@link ReplicationReceiverMessage} is stored in the queue of messages to send.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: a version of the {@link MutableReplicationMessage} about to be sent in which the custom metadata is
 * modifiable</li>
 * </ul>
 * 
 * @version $Id$
 */
public class ReplicationMessageStoringEvent extends AbstractCancelableEvent
{
}
