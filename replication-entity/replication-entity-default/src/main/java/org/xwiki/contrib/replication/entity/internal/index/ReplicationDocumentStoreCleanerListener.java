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

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(ReplicationDocumentStoreCleanerListener.NAME)
public class ReplicationDocumentStoreCleanerListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "ReplicationDocumentStoreCleanerListener";

    @Inject
    private Provider<ReplicationDocumentStore> store;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ReplicationDocumentStoreCleanerListener()
    {
        super(NAME, new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentReference documentReference = ((XWikiDocument) source).getDocumentReference();
        try {
            this.store.get().deleteDocument(documentReference);
        } catch (ReplicationException e) {
            this.logger.error("Failed to remove metadata associated with document [{}]", documentReference, e);
        }
    }
}
