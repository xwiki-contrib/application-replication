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
package org.xwiki.contrib.replication.internal.instance;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.event.ReplicationInstanceUnregisteredEvent;
import org.xwiki.contrib.replication.internal.message.DefaultReplicationSender;
import org.xwiki.contrib.replication.internal.message.ReplicationSenderMessageQueue;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * @version $Id$
 * @since 2.0.0
 */
@Component
@Named(CleanupInstanceListener.NAME)
@Singleton
public class CleanupInstanceListener extends AbstractEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "CleanupInstanceListener";

    @Inject
    private Provider<ReplicationInstanceManager> instancesProvider;

    @Inject
    private Provider<ReplicationSender> senderProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public CleanupInstanceListener()
    {
        super(NAME, new ReplicationInstanceUnregisteredEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ReplicationInstanceUnregisteredEvent unregisteredEvent = (ReplicationInstanceUnregisteredEvent) event;

        String uri = unregisteredEvent.getURI();
        try {
            purge(uri);
        } catch (ReplicationException e) {
            this.logger.error("Failed to purge the unregistered instance with uri [{}]", uri);
        }
    }

    private void purge(String uri) throws ReplicationException
    {
        // Get the instance
        ReplicationInstance instance = this.instancesProvider.get().getInstanceByURI(uri);

        ReplicationSender sender = this.senderProvider.get();
        if (sender instanceof DefaultReplicationSender) {
            // Get the instance queue
            ReplicationSenderMessageQueue queue = ((DefaultReplicationSender) sender).getQueue(instance);

            queue.purge();
        }
    }
}
