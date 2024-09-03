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

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentVersionRangeDeletedEvent;
import org.xwiki.bridge.event.DocumentVersionRangeDeletingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listen to documents modification and give them to the replication sender.
 * 
 * @version $Id$
 */
@Component
@Named(DocumentReplicationListener.NAME)
@Singleton
public class DocumentReplicationListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "DocumentReplicationListener";

    private static final DocumentVersionRangeDeletingEvent HISTORY_DELETING = new DocumentVersionRangeDeletingEvent();

    @Inject
    private ReplicationContext replicationContext;

    @Inject
    private ObservationContext observationcontext;

    @Inject
    private RemoteObservationManagerContext remoteContext;

    @Inject
    private Provider<DocumentReplicationController> controllerProvider;

    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public DocumentReplicationListener()
    {
        super(NAME, new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent(),
            new DocumentVersionRangeDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Ignore the modification if:
        // * it's been caused by a replication
        // * it's been caused by a remote event
        if (this.replicationContext.isReplicationMessage() || this.remoteContext.isRemoteState()) {
            return;
        }

        XWikiDocument document = (XWikiDocument) source;

        // Send the message
        try {
            if (event instanceof DocumentVersionRangeDeletedEvent) {
                this.controllerProvider.get().onDocumentHistoryDelete(document,
                    ((DocumentVersionRangeDeletedEvent) event).getFrom(),
                    ((DocumentVersionRangeDeletedEvent) event).getTo());
            } else if (event instanceof DocumentDeletedEvent) {
                this.controllerProvider.get().onDocumentDeleted(document.getOriginalDocument());
            } else {
                // Don't send document update which are the result of an history modification
                if (!this.observationcontext.isIn(HISTORY_DELETING)) {
                    if (event instanceof DocumentCreatedEvent) {
                        this.controllerProvider.get().onDocumentCreated(document);
                    } else {
                        this.controllerProvider.get().onDocumentUpdated(document);
                    }
                }
            }
        } catch (ReplicationException e) {
            this.logger.error("Failed to send a replication message for document [{}] in version [{}]",
                document.getDocumentReferenceWithLocale(), document.getVersion(), e);
        }
    }
}
