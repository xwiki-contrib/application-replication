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
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.contrib.replication.entity.internal.DocumentReplicationControllerUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;

import com.google.common.base.Objects;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

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
    private DocumentRevisionProvider revisionProvider;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private DocumentReplicationControllerUtils controllerUtils;

    @Inject
    private DocumentReplicationController controller;

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
        String previousVersion = this.documentMessageTool.getMetadata(message,
            DocumentUpdateReplicationMessage.METADATA_PREVIOUSVERSION, false);
        Date previousVersionDate = this.documentMessageTool.getMetadata(message,
            DocumentUpdateReplicationMessage.METADATA_PREVIOUSVERSION_DATE, false, Date.class);

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
        newDocument.setAuthorReference(document.getAuthorReference());
        newDocument.setContentAuthorReference(document.getContentAuthorReference());
        newDocument.setCreatorReference(document.getCreatorReference());
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
            // Get previous database version
            String currentVersion = currentDocument.isNew() ? null : currentDocument.getVersion();
            Date currentVersionDate = currentDocument.isNew() ? null : currentDocument.getDate();

            // Check if the previous version is the expected one
            if (!Objects.equal(currentVersion, previousVersion)
                || !Objects.equal(currentVersionDate, previousVersionDate)) {
                // If not create and save a merged version of the document
                merge(previousVersion, currentDocument, newDocument, xcontext);
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

    private void merge(String previousVersion, XWikiDocument currentDocument, XWikiDocument newDocument,
        XWikiContext xcontext)
    {
        // Get expected previous version from the history
        XWikiDocument previousDocument;
        if (previousVersion == null) {
            // Previous version is an empty document
            previousDocument = new XWikiDocument(newDocument.getDocumentReference(), newDocument.getLocale());
        } else {
            try {
                previousDocument =
                    this.revisionProvider.getRevision(newDocument.getDocumentReferenceWithLocale(), previousVersion);
            } catch (XWikiException e) {
                this.logger.error("Failed to access the expected previous version", e);

                return;
            }
            // If the previous version does not exist anymore don't merge
            if (previousDocument == null) {
                return;
            }
        }

        // Remember last version
        String newVersion = newDocument.getVersion();

        // Execute the merge
        MergeDocumentResult mergeResult =
            this.mergeManager.mergeDocument(previousDocument, currentDocument, newDocument, new MergeConfiguration());

        // Save the merged version if anything changed
        if (mergeResult.isModified()) {
            try {
                xcontext.getWiki().saveDocument(newDocument,
                    "Merge [" + currentDocument.getVersion() + "] and [" + newVersion + "] versions", true, xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to save merged document", e);
            }
        }

        // Send the complete document with updated history to other instances so that they synchronize
        try {
            this.controller.sendCompleteDocument(newDocument);
        } catch (ReplicationException e) {
            this.logger.error("Failed to send back the corrected complete document for reference [{}]",
                newDocument.getDocumentReferenceWithLocale(), e);
        }
    }

    @Override
    public void relay(ReplicationReceiverMessage message) throws ReplicationException
    {
        this.documentRelay.relayDocumentUpdate(message);
    }
}
