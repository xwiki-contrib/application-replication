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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * @version $Id$
 * @since 1.12.0
 */
@Component
@Named(ReplicationDocumentIndexListener.NAME)
@Singleton
public class ReplicationDocumentIndexListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "ReplicationDocumentIndexListener";

    @Inject
    private ReplicationDocumentStoreCache cache;

    @Inject
    private Provider<ReplicationDocumentStore> storeProvider;

    /**
     * Setup name and events to listen.
     */
    public ReplicationDocumentIndexListener()
    {
        super(NAME, new DocumentIndexDeletedEvent(), new DocumentOwnerUpdatedEvent(),
            new DocumentReadonlyUpdatedEvent(), new DocumentConflictUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.cache.remove(this.storeProvider.get().getCacheKey(((AbstractDocumentIndexEvent) event).getReference()));
    }
}
