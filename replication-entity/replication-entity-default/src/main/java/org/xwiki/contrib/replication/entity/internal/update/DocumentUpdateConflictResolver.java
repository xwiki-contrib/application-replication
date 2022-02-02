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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.notification.ReplicationDocumentConflictEvent;
import org.xwiki.logging.LogLevel;
import org.xwiki.observation.ObservationManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * @version $Id$
 */
@Component(roles = DocumentUpdateConflictResolver.class)
@Singleton
public class DocumentUpdateConflictResolver
{
    @Inject
    private DocumentReplicationController controller;

    @Inject
    private ObservationManager observation;

    @Inject
    private UserReferenceResolver<String> userResolver;

    @Inject
    private UserReferenceSerializer<String> userSerializer;

    @Inject
    private DocumentRevisionProvider revisionProvider;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private Logger logger;

    /**
     * @param previousVersion the common previous version
     * @param currentDocument the current version
     * @param newDocument the received version
     * @param xcontext the XWiki context
     * @throws ReplicationException when failing to merge documents
     */
    public void merge(String previousVersion, XWikiDocument currentDocument, XWikiDocument newDocument,
        XWikiContext xcontext) throws ReplicationException
    {
        // Get expected previous version from the history
        XWikiDocument previousDocument;
        if (previousVersion == null) {
            // Previous version is an empty document
            previousDocument = new XWikiDocument(newDocument.getDocumentReference(), newDocument.getLocale());
        } else {
            // TODO: REPLICAT-57 search for the right previous version
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

        // Notify involved authors about the conflict resolution but only if the merge had a real conflict
        if (mergeResult.getLog().hasLogLevel(LogLevel.ERROR)) {
            // Find all authors involved
            Set<String> authors = findAuthors(previousDocument, currentDocument, newDocument, xcontext);

            this.observation.notify(
                new ReplicationDocumentConflictEvent(newDocument.getDocumentReferenceWithLocale(), authors),
                "replication", newDocument);
        }
    }

    private Set<String> findAuthors(XWikiDocument previousDocument, XWikiDocument currentDocument,
        XWikiDocument newDocument, XWikiContext xcontext) throws ReplicationException
    {
        Set<String> authors = new HashSet<>();
        Collection<XWikiRCSNodeInfo> nodes;
        try {
            nodes = newDocument.getDocumentArchive(xcontext).getNodes(newDocument.getRCSVersion(),
                previousDocument.getRCSVersion());
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to get the document history", e);
        }
        for (XWikiRCSNodeInfo node : nodes) {
            UserReference authorReference =
                this.userResolver.resolve(node.getAuthor(), newDocument.getDocumentReference());
            authors.add(this.userSerializer.serialize(authorReference));
        }

        return authors;
    }
}
