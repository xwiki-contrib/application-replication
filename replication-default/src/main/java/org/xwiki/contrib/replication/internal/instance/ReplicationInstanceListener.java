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
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * @version $Id$
 */
@Component
@Named(ReplicationInstanceListener.NAME)
@Singleton
public class ReplicationInstanceListener extends AbstractEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "ReplicationInstanceListener";

    @Inject
    private ReplicationInstanceCache cache;

    @Inject
    private Provider<ReplicationInstanceStore> store;

    @Inject
    private Provider<ReplicationInstanceManager> instances;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ReplicationInstanceListener()
    {
        super(NAME, new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ApplicationReadyEvent) {
            reload();
        } else {
            // TODO
        }
    }

    private void reload()
    {
        try {
            this.instances.get().reload();
        } catch (ReplicationException e) {
            this.logger.error("Failed to reload stored instances", e);
        }
    }
}
