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
package org.xwiki.contrib.replication.entity.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.event.ReplicationInstanceRegisteredEvent;
import org.xwiki.contrib.replication.event.ReplicationInstanceUnregisteredEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Register the entity replication mapping.
 *
 * @version $Id: f9ed06c92322e4b3ba6505fc7b87ea7a142d246d $
 */
@Component
@Named("EntityReplicationCleanupListener")
@Singleton
public class EntityReplicationCleanupListener extends AbstractEventListener
{
    @Inject
    private EntityReplicationCache cache;

    @Inject
    private Provider<EntityReplicationStore> storeProvider;

    /**
     * Setup the listener.
     */
    public EntityReplicationCleanupListener()
    {
        super("EntityReplicationCacheInvalidationListener", new ReplicationInstanceRegisteredEvent(),
            new ReplicationInstanceUnregisteredEvent(), new DocumentDeletedEvent(), new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Clean the store
        if (event instanceof ReplicationInstanceUnregisteredEvent) {
            // Remove any entry associated to an instance which is not replicated anymore
            this.storeProvider.get().deleteInstance(((ReplicationInstanceUnregisteredEvent) event).getURI());
        } else if (event instanceof DocumentDeletedEvent) {
            // TODO: remove the entity configuration from the store ?
            this.cache.onDelete(((XWikiDocument) source).getDocumentReference());
        } else {
            this.cache.removeAll();
        }
    }
}
