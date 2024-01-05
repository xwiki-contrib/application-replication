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
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.EntityReplicationMessage;
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
@Named(EntityReplicationMessage.TYPE_DOCUMENT_REFERENCE)
public class DocumentReferenceReplicationReceiver extends AbstractDocumentReplicationReceiver
{
    private static final String REFERENCE_CONTENT =
        "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}";

    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        if (this.documentStore.getOwner(documentReference) != null) {
            // If there is a previous replication which is not a REFERENCE replication, unreplicate it first
            XWikiDocument existingDocument;
            try {
                existingDocument = xcontext.getWiki().getDocument(documentReference, xcontext);
            } catch (XWikiException e) {
                throw new ReplicationException("Failed to access existing document", e);
            }
            if (!existingDocument.isNew() && !REFERENCE_CONTENT.equals(existingDocument.getContent())) {
                unreplicate(existingDocument, xcontext);
            }
        }

        // Create an empty document
        XWikiDocument document = new XWikiDocument(documentReference);

        // Just indicate who created it
        UserReference creatorReference = this.messageReader.getMetadata(message,
            EntityReplicationMessage.METADATA_ENTITY_CREATOR, true, UserReference.class);
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
            REFERENCE_CONTENT);

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

        // REFERENCE documents are readonly by definition
        this.entityReplication.setReadonly(documentReference, true);
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        // Don't send REFERENCE replication to instance expecting higher level
        return this.controller.relay(message, DocumentReplicationLevel.REFERENCE, DocumentReplicationLevel.REFERENCE);
    }
}
