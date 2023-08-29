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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.contrib.replication.entity.internal.DocumentReplicationUtils;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
import org.xwiki.contrib.replication.entity.internal.repairrequest.DocumentRepairRequestReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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
    private DocumentReplicationUtils controllerUtils;

    @Inject
    private DocumentUpdateConflictResolver conflictResolver;

    @Inject
    private ReplicationDocumentStore documentStore;

    @Inject
    private Provider<DocumentRepairRequestReplicationMessage> repairRequestMessageProvider;

    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        boolean complete = this.documentMessageReader.isComplete(message);

        // Load the document
        XWikiDocument replicationDocument = new XWikiDocument(documentReference, documentReference.getLocale());
        try (InputStream stream = message.open()) {
            this.loader.importDocument(replicationDocument, stream);
        } catch (Exception e) {
            throw new ReplicationException("Failed to parse document message to update", e);
        }

        if (complete) {
            completeUpdate(replicationDocument, xcontext);
        } else {
            update(message, documentReference, replicationDocument, xcontext);
        }
    }

    private void update(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiDocument replicationDocument, XWikiContext xcontext) throws ReplicationException
    {
        // Load the current document
        XWikiDocument databaseDocument;
        try {
            // Clone the document to not be disturbed by modifications made by other threads
            databaseDocument = xcontext.getWiki().getDocument(documentReference, xcontext).clone();
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to load document to update", e);
        }

        // Indicate the current document as original version of the new one
        replicationDocument.setOriginalDocument(databaseDocument);
        // Indicate if it's an update of an already existing document
        replicationDocument.setNew(databaseDocument.isNew());

        // Clone the current document to have a modifiable version
        XWikiDocument previousDocument = databaseDocument.clone();

        // Indicate in the new document the xobjects which don't exist anymore
        cleanXObjects(previousDocument, replicationDocument);

        // We need to do some manipulations on the attachments to end up with the expected result, especially regarding
        // the versionning and the content store
        prepareUpdateAttachments(previousDocument, replicationDocument);

        if (replicationDocument.getRCSVersion().isLessOrEqualThan(databaseDocument.getRCSVersion())) {
            // The replicated document version conflicts with an existing one (generally because several instances sent
            // concurrent modifications of the same document), we save it with a natural incrementation of the local
            // version
            replicationDocument.setRCSVersion(
                XWikiDocument.getNextVersion(databaseDocument.getRCSVersion(), replicationDocument.isMinorEdit()));
        }

        // We want to save the document as is (among other things we want to make sure to keep the same date as in
        // the message)
        replicationDocument.setMetaDataDirty(false);
        replicationDocument.setContentDirty(false);

        // Save the updated document
        try {
            xcontext.getWiki().saveDocument(replicationDocument, replicationDocument.getComment(),
                replicationDocument.isMinorEdit(), xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to save document update", e);
        }

        // Identify a conflict
        Collection<String> values =
            message.getCustomMetadata().get(DocumentUpdateReplicationMessage.METADATA_ANCESTORS);

        // If no ancestor is provided we cannot really know if there is a conflict
        if (values != null) {
            // Get previous database version
            String currentVersion = databaseDocument.isNew() ? null : databaseDocument.getVersion();
            Date currentVersionDate = databaseDocument.isNew() ? null : databaseDocument.getDate();

            List<DocumentAncestor> ancestors = DocumentAncestorConverter.toDocumentAncestors(values);

            String previousAncestorVersion = ancestors.get(0).getVersion();
            Date previousAncestorVersionDate = ancestors.get(0).getDate();

            // Check if the previous version is the expected one
            if (!Objects.equals(currentVersion, previousAncestorVersion)
                || !Objects.equals(currentVersionDate, previousAncestorVersionDate)) {
                if (this.controllerUtils.isOwner(documentReference)) {
                    // Actually handle the conflict only if the current instance is the owner
                    // Create and save a merged version of the document
                    this.conflictResolver.merge(ancestors, databaseDocument, replicationDocument, xcontext);
                } else {
                    // Otherwise just ask the owner for a repair (it case the owner did not notice)
                    requestRepair(documentReference);
                }
            }
        }
    }

    private void cleanXObjects(XWikiDocument previousDocument, XWikiDocument replicationDocument)
    {
        for (List<BaseObject> previousObjects : previousDocument.getXObjects().values()) {
            // Duplicate the list since we are potentially going to modify it
            for (BaseObject previousObject : previousObjects) {
                if (previousObject != null && replicationDocument.getXObject(previousObject.getXClassReference(),
                    previousObject.getNumber()) == null) {
                    // The xobject does not exist anymore, add it and then properly remove it
                    replicationDocument.setXObject(previousObject.getNumber(), previousObject);
                    replicationDocument.removeXObject(previousObject);
                }
            }
        }
    }

    private void prepareUpdateAttachments(XWikiDocument previousDocument, XWikiDocument replicationDocument)
    {
        if (previousDocument != null) {
            for (XWikiAttachment previousAttachment : previousDocument.getAttachmentList()) {
                XWikiAttachment replicationAttachment =
                    replicationDocument.getAttachment(previousAttachment.getFilename());
                if (replicationAttachment == null) {
                    // The attachment does not exist anymore, add it and then properly remove it
                    replicationDocument.setAttachment(previousAttachment);
                    replicationDocument.removeAttachment(previousAttachment);
                } else {
                    // Keep the same store and archive as the current attachment
                    replicationAttachment.setContentStore(previousAttachment.getContentStore());
                    replicationAttachment.setArchiveStore(previousAttachment.getArchiveStore());
                }
            }
        }

        // Prepare versions
        for (XWikiAttachment replicationAttachment : replicationDocument.getAttachmentList()) {
            // Prepare the version
            if (replicationAttachment.getAttachment_content() != null) {
                // Attachment for which a new content is provided is going to have its version incremented so me need to
                // make sure that when incremented we end up with the intended version
                Version previousVersion;
                int[] numbers = replicationAttachment.getRCSVersion().getNumbers();
                int versionIndex = numbers.length - 1;
                if (numbers[versionIndex] == 1) {
                    // Previous version of a new attachment is no version
                    previousVersion = null;
                } else {
                    // Decrement the version
                    numbers[versionIndex] = numbers[versionIndex] - 1;
                    previousVersion = new Version(numbers);
                }
                replicationAttachment.setRCSVersion(previousVersion);
            }
        }
    }

    private void requestRepair(DocumentReference documentReference) throws ReplicationException
    {
        String owner = this.documentStore.getOwner(documentReference);

        if (owner != null) {
            Set<String> receivers = Collections.singleton(owner);

            this.controller.send(m -> {
                DocumentRepairRequestReplicationMessage repairRequestMessage = this.repairRequestMessageProvider.get();

                repairRequestMessage.initialize(documentReference, receivers, m);

                return repairRequestMessage;
            }, documentReference, DocumentReplicationLevel.ALL, receivers);
        }
    }

    private void completeUpdate(XWikiDocument replicationDocument, XWikiContext xcontext) throws ReplicationException
    {
        // Make sure the document is going to be saved as is
        for (XWikiAttachment replicationAttachment : replicationDocument.getAttachmentList()) {
            replicationAttachment.setMetaDataDirty(false);
            if (replicationAttachment.getAttachment_content() != null) {
                replicationAttachment.getAttachment_content().setContentDirty(false);
            }
        }
        replicationDocument.setMetaDataDirty(false);
        replicationDocument.setContentDirty(false);

        // Save the new complete document
        // The fact that isNew() return true makes saveDocument automatically delete the current document in
        // database and replace it with the received one
        try {
            xcontext.getWiki().saveDocument(replicationDocument, replicationDocument.getComment(),
                replicationDocument.isMinorEdit(), xcontext);
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
