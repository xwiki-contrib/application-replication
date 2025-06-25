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

import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.contrib.replication.entity.EntityReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Base class for replication receivers handling message affecting document.
 * 
 * @version $Id$
 */
public abstract class AbstractDocumentReplicationReceiver extends AbstractEntityReplicationReceiver
{
    @Inject
    protected DocumentReplicationController controller;

    @Inject
    protected EntityReplication entityReplication;

    @Inject
    protected ReplicationInstanceManager instances;

    @Inject
    protected ReplicationDocumentStore documentStore;

    @Override
    protected void receiveEntity(ReplicationReceiverMessage message, EntityReference entityReference,
        XWikiContext xcontext) throws ReplicationException
    {
        DocumentReference documentReference = this.documentMessageReader.getDocumentReference(message, entityReference);

        receiveDocument(message, documentReference, xcontext);
    }

    /**
     * @param message the received message
     * @param documentReference the reference of the document associated with the message
     * @param level how much of the document is replicated
     * @throws ReplicationException
     */
    protected void handlerOwnerAndLevel(ReplicationReceiverMessage message, DocumentReference documentReference,
        DocumentReplicationLevel level) throws ReplicationException
    {
        // Owner
        String currentOwner = this.entityReplication.getOwner(documentReference);
        if (currentOwner == null) {
            // There is no owner yet so try to find one
            // Check if one is explicitly provided
            currentOwner =
                this.messageReader.getMetadata(message, EntityReplicationMessage.METADATA_DOCUMENT_UPDATE_OWNER, false);

            // Fallback on the source
            if (currentOwner == null) {
                currentOwner = message.getSource();
            }

            // Set the document owner
            this.documentStore.setOwnerAndLevel(documentReference, currentOwner, level);
        } else {
            DocumentReplicationLevel currentLevel = this.entityReplication.getLevel(documentReference);

            if (currentLevel != level) {
                // Update the document replication level
                this.documentStore.setLevel(documentReference, level);
            }
        }
    }

    protected void unreplicate(XWikiDocument document, XWikiContext xcontext) throws ReplicationException
    {
        try {
            // Skip the trash bin since that instance is not supposed to know about this document at all anymore
            xcontext.getWiki().deleteDocument(document, false, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to delete the document", e);
        }
    }

    protected abstract void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException;
}
