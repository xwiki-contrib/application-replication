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
package org.xwiki.contrib.replication.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

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

    @Inject
    private Provider<ReplicationSender> senderProvider;

    @Inject
    private Provider<DocumentReplicationSenderMessage> messageProvider;

    @Inject
    private ReplicationContext replicationContext;

    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public DocumentReplicationListener()
    {
        super(NAME, new DocumentCreatedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Ignore the modification if it's been cause by a replication
        if (this.replicationContext.isReplicationMessage()) {
            return;
        }

        XWikiDocument document = (XWikiDocument) source;

        // TODO: make configurable the entities to replicate
        // Only replicate documents in Replication space for now
        if (!document.getDocumentReference().getSpaceReferences().get(0).getName().equals("Replication")) {
            return;
        }

        // Create a new message
        // Don't set a previous version if it's a new document
        DocumentReplicationSenderMessage message = this.messageProvider.get();
        message.initialize(document.getDocumentReferenceWithLocale(), document.getVersion(),
            event instanceof DocumentCreatedEvent ? null : document.getOriginalDocument().getVersion());

        // Send the message
        try {
            this.senderProvider.get().send(message);
        } catch (ReplicationException e) {
            this.logger.error("Failed to send a replication message for document [{}] in version [{}]",
                document.getDocumentReferenceWithLocale(), document.getVersion());
        }
    }
}
