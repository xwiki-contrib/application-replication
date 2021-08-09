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
package org.xwiki.contrib.replication.internal.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.BooleanUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.contrib.replication.internal.DocumentReplicationSender;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;

import com.google.common.base.Objects;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(DocumentUpdateReplicationMessage.TYPE)
public class DocumentUpdateReplicationReceiver extends AbstractDocumentReplicationReceiver
{
    @Inject
    // TODO: don't use internal tool
    private XWikiDocumentFilterUtils importer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentRevisionProvider revisionProvider;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private DocumentReplicationSender sender;

    @Override
    public void receive(ReplicationReceiverMessage message) throws ReplicationException
    {
        DocumentReference documentReference = getDocumentReference(message);

        XWikiContext xcontext = this.xcontextProvider.get();

        boolean complete =
            BooleanUtils.toBoolean(getMetadata(message, DocumentUpdateReplicationMessage.METADATA_COMPLETE, false));

        // Load the document
        XWikiDocument document = new XWikiDocument(documentReference, documentReference.getLocale());
        try (InputStream stream = message.open()) {
            importDocument(document, stream);
        } catch (Exception e) {
            throw new ReplicationException("Failed to parse document message to update", e);
        }

        if (complete) {
            completeUpdate(document, xcontext);
        } else {
            String previousVersion =
                getMetadata(message, DocumentUpdateReplicationMessage.METADATA_PREVIOUSVERSION, false);
            Date previousVersionDate =
                getMetadata(message, DocumentUpdateReplicationMessage.METADATA_PREVIOUSVERSION_DATE, false, Date.class);

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
            this.sender.sendDocument(newDocument, true);
        } catch (ReplicationException e) {
            this.logger.error("Failed to send back the corrected complete document for reference [{}]",
                newDocument.getDocumentReferenceWithLocale(), e);
        }
    }

    private void importDocument(XWikiDocument document, InputStream stream)
        throws FilterException, IOException, ComponentLookupException
    {
        // Output
        DocumentInstanceOutputProperties documentProperties = new DocumentInstanceOutputProperties();
        documentProperties.setDefaultReference(document.getDocumentReferenceWithLocale());

        // Input
        XARInputProperties xarProperties = new XARInputProperties();

        this.importer.importEntity(XWikiDocument.class, document, new DefaultInputStreamInputSource(stream),
            xarProperties, documentProperties);
    }
}
