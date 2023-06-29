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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationInstanceRecoverHandler;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named(ReplicationMessage.TYPE_INSTANCE_UPDATE)
public class ReplicationInstanceUpdateRecoverHandler implements ReplicationInstanceRecoverHandler
{
    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private ReplicationInstanceMessageSender sender;

    @Override
    public void receive(Date dateMin, Date dateMax, ReplicationReceiverMessage message) throws ReplicationException
    {
        ReplicationInstance sourceInstance = this.instances.getInstanceByURI(message.getSource());

        // Taking care of this only in direct linked instances is enough
        if (sourceInstance == null) {
            return;
        }

        // Just send a new message with the current state
        this.sender.updateCurrentInstance();
    }
}
