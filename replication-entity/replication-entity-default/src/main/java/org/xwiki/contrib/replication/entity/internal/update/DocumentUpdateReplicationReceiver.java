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
package org.xwiki.contrib.replication.entity.internal.update;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.contrib.replication.entity.internal.DocumentReplicationControllerUtils;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(DocumentUpdateReplicationMessage.TYPE)
public class DocumentUpdateReplicationReceiver extends AbstractDocumentReplicationReceiver
{
    @Inject
    private DocumentUpdateLoaded loader;

    @Inject
    private DocumentReplicationControllerUtils controllerUtils;

    @Inject
    private DocumentUpdateConflictResolver conflictResolver;

    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        boolean complete = this.documentMessageTool.isComplete(message);

        // Load the document
        XWikiDocument document = new XWikiDocument(documentReference, documentReference.getLocale());
        try (InputStream stream = message.open()) {
            this.loader.importDocument(document, stream);
        } catch (Exception e) {
            throw new ReplicationException("Failed to parse document message to update", e);
        }

        if (complete) {
            completeUpdate(document, xcontext);
        } else {
            update(message, documentReference, document, xcontext);
        }
    }

    private void update(ReplicationReceiverMessage message, DocumentReference documentReference, XWikiDocument document,
        XWikiContext xcontext) throws ReplicationException
    {
        // Load the current document
        XWikiDocument currentDocument;
        try {
            // Clone the document to not be disturbed by modifications made by other threads
            currentDocument = xcontext.getWiki().getDocument(documentReference, xcontext).clone();
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to load document to update", e);
        }

        // Keep a copy of the current document for later
        XWikiDocument newDocument = currentDocument.clone();

        // Update the document
        newDocument.apply(document, true);
        // Also copy some revision related properties
        newDocument.getAuthors().setCreator(document.getAuthors().getCreator());
        newDocument.getAuthors().setContentAuthor(document.getAuthors().getContentAuthor());
        newDocument.getAuthors().setEffectiveMetadataAuthor(document.getAuthors().getEffectiveMetadataAuthor());
        newDocument.getAuthors().setOriginalMetadataAuthor(document.getAuthors().getOriginalMetadataAuthor());
        newDocument.setDate(document.getDate());
        newDocument.setContentUpdateDate(document.getContentUpdateDate());

        // Save the updated document
        try {
            xcontext.getWiki().saveDocument(newDocument, document.getComment(), document.isMinorEdit(), xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to save document update", e);
        }

        // Deal with conflict if this instance is allowed to replicate content
        if (this.controllerUtils.isReplicated(documentReference)) {
            Collection<String> values =
                message.getCustomMetadata().get(DocumentUpdateReplicationMessage.METADATA_ANCESTORS);

            // If no ancestor is provided we cannot really know if there is a conflict
            if (values != null) {
                // Get previous database version
                String currentVersion = currentDocument.isNew() ? null : currentDocument.getVersion();
                Date currentVersionDate = currentDocument.isNew() ? null : currentDocument.getDate();

                List<DocumentAncestor> ancestors = DocumentAncestorConverter.toDocumentAncestors(values);

                String previousAncestorVersion = ancestors.get(0).getVersion();
                Date previousAncestorVersionDate = ancestors.get(0).getDate();

                // Check if the previous version is the expected one
                if (!Objects.equals(currentVersion, previousAncestorVersion)
                    || !Objects.equals(currentVersionDate, previousAncestorVersionDate)) {
                    // If not create and save a merged version of the document
                    this.conflictResolver.merge(ancestors, currentDocument, newDocument, xcontext);
                }
            }
        }
    }

    private void completeUpdate(XWikiDocument document, XWikiContext xcontext) throws ReplicationException
    {
        // We want to save the document as is
        document.setMetaDataDirty(false);
        document.setContentDirty(false);

        // Save the new complete document
        // The fact that isNew() return true makes saveDocument automatically delete the current document in
        // database and replace it with the received one
        try {
            xcontext.getWiki().saveDocument(document, document.getComment(), document.isMinorEdit(), xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to save complete document", e);
        }
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return this.documentRelay.relayDocumentUpdate(message);
    }
}
