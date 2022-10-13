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

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.replication.AbstractReplicationReceiver;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceRecoverHandler;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * Dispatch recovering request to the various {@link ReplicationInstanceRecoverHandler} implementations.
 * 
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named(ReplicationInstanceRecoverMessage.TYPE)
public class ReplicationInstanceRecoverReceiver extends AbstractReplicationReceiver
{
    @Inject
    private ComponentManager componentManager;

    @Override
    public void receive(ReplicationReceiverMessage message) throws ReplicationException
    {
        List<ReplicationInstanceRecoverHandler> handlers;
        try {
            handlers = this.componentManager.getInstanceList(ReplicationInstanceRecoverHandler.class);
        } catch (ComponentLookupException e) {
            throw new ReplicationException("Failed to lookup ReplicationInstanceRecoverHandler instances", e);
        }

        Date dateMin = this.messageReader.getMetadata(message, ReplicationInstanceRecoverMessage.METADATA_DATE_MIN,
            true, Date.class);
        Date dateMax = this.messageReader.getMetadata(message, ReplicationInstanceRecoverMessage.METADATA_DATE_MAX,
            true, Date.class);

        for (ReplicationInstanceRecoverHandler handler : handlers) {
            handler.receive(dateMin, dateMax, message);
        }
    }
}
