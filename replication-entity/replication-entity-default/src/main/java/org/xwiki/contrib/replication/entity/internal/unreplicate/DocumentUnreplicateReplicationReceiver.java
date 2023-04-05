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
package org.xwiki.contrib.replication.entity.internal.unreplicate;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(DocumentUnreplicateReplicationMessage.TYPE)
public class DocumentUnreplicateReplicationReceiver extends AbstractDocumentReplicationReceiver
{
    @Inject
    private EntityReplication entityReplication;

    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        String ownerInstance = this.entityReplication.getOwner(documentReference);
        if (ownerInstance != null) {
            String sourceInstance = message.getSource();

            if (!ownerInstance.equals(sourceInstance)) {
                throw new InvalidReplicationMessageException(
                    "Instance [" + sourceInstance + "] is not allowed to unreplicate document [" + documentReference
                        + "] as it's owned by instance [" + ownerInstance + "]");
            }
        }

        // Load the document
        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(documentReference, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to load document", e);
        }

        // Delete the document
        // If the document is already deleted or does not exist for some reason, ignore it
        if (!document.isNew()) {
            try {
                // Skip the trash bin since that instance is not supposed to know about this document at all anymore
                xcontext.getWiki().deleteDocument(document, false, xcontext);
            } catch (XWikiException e) {
                throw new ReplicationException("Failed to delete the document", e);
            }
        }
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return this.documentRelay.relay(message, DocumentReplicationLevel.REFERENCE);
    }
}
