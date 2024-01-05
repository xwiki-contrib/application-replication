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
package org.xwiki.contrib.replication.entity.internal.reference;

import java.util.concurrent.CompletableFuture;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(DocumentReferenceReplicationMessage.TYPE)
public class DocumentReferenceReplicationReceiver extends AbstractDocumentReplicationReceiver
{
    @Override
    protected void checkMessageInstance(ReplicationReceiverMessage message, DocumentReference documentReference,
        DocumentReplicationControllerInstance currentConfiguration) throws ReplicationException
    {
        // It's forbidden to send unreplicate messages to the owner
        if (this.replicationUtils.isOwner(documentReference)) {
            throw new InvalidReplicationMessageException(
                "It's forbidden to send REFERENCE messages to the owner instance");
        }
    }

    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        // Create an empty document
        XWikiDocument document = new XWikiDocument(documentReference);

        // Just indicate who created it
        UserReference creatorReference = this.messageReader.getMetadata(message,
            DocumentReferenceReplicationMessage.METADATA_CREATOR, true, UserReference.class);
        document.getAuthors().setCreator(creatorReference);
        document.getAuthors().setContentAuthor(creatorReference);
        document.getAuthors().setEffectiveMetadataAuthor(creatorReference);
        document.getAuthors().setOriginalMetadataAuthor(creatorReference);
        // Those place holders should be hidden
        document.setHidden(true);
        // Set a message explaining what this document is
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setContent(
            // TODO: go through an xobject and a sheet instead to keep an empty document content (less impacting)
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");

        // Ask the controller for modification before save
        this.controller.receiveREFERENCEDocument(document, message);

        // Save the document
        try {
            xcontext.getWiki().saveDocument(document, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to save the document", e);
        }

        // Owner
        handlerOwner(message, documentReference);
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        // Don't send REFERENCE replication to instance expecting higher level
        return this.documentRelay.relay(message, DocumentReplicationLevel.REFERENCE,
            DocumentReplicationLevel.REFERENCE);
    }
}
