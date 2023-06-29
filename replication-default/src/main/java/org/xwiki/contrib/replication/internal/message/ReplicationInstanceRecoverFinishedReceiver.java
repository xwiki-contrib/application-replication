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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.AbstractReplicationReceiver;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * Receive the {@link ReplicationInstanceRecoverFinisheddMessage} message.
 * 
 * @version $Id$
 * @since 1.2.0
 */
@Component
@Singleton
@Named(ReplicationMessage.TYPE_INSTANCE_RECOVER_FINISHED)
public class ReplicationInstanceRecoverFinishedReceiver extends AbstractReplicationReceiver
{
    @Override
    public void receive(ReplicationReceiverMessage message) throws ReplicationException
    {
        // Nothing to do, this message is just logged right now
    }
}
