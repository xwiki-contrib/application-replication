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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
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
import com.xpn.xwiki.doc.XWikiDocumentArchive;
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
     * @param anscestors the previous version expected by the received update
     * @param currentDocument the current version
     * @param newDocument the received version
     * @param xcontext the XWiki context
     * @throws ReplicationException when failing to merge documents
     */
    public void merge(List<DocumentAncestor> anscestors, XWikiDocument currentDocument, XWikiDocument newDocument,
        XWikiContext xcontext) throws ReplicationException
    {
        // Get expected previous version from the history
        XWikiDocument ancestorDocument;
        try {
            ancestorDocument = getAncestorDocument(currentDocument, anscestors, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to load ancestor document", e);
        }

        // The the ancestor document cannot be found merge from an empty document
        if (ancestorDocument == null) {
            ancestorDocument = new XWikiDocument(currentDocument.getDocumentReference(), currentDocument.getLocale());
        }

        // Remember last version
        String newVersion = newDocument.getVersion();

        // Execute the merge
        MergeDocumentResult mergeResult =
            this.mergeManager.mergeDocument(ancestorDocument, currentDocument, newDocument, new MergeConfiguration());

        // Save the merged version if anything changed
        if (mergeResult.isModified()) {
            try {
                xcontext.getWiki().saveDocument(newDocument,
                    "Merge [" + currentDocument.getVersion() + "] and [" + newVersion + "] versions", true, xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to save merged document", e);
            }
        }

        Set<String> authors;

        // Notify involved authors about the conflict resolution but only if the merge had a real conflict
        if (mergeResult.getLog().hasLogLevel(LogLevel.ERROR)) {
            // Find all authors involved
            authors = findAuthors(ancestorDocument, currentDocument, newDocument, xcontext);

            // Notify about the conflict
            notifyConflict(newDocument, authors);
        } else {
            // Not a real conflict so no need to send the authors
            authors = null;
        }

        // Send the complete document with updated history to other instances so that they synchronize
        try {
            this.controller.sendDocumentRepair(newDocument, authors);
        } catch (ReplicationException e) {
            this.logger.error("Failed to send back a conflict repair for document[{}]",
                newDocument.getDocumentReferenceWithLocale(), e);
        }
    }

    private XWikiDocument getAncestorDocument(XWikiDocument currentDocument, List<DocumentAncestor> ancestors,
        XWikiContext xcontext) throws XWikiException
    {
        XWikiDocumentArchive archive = currentDocument.getDocumentArchive(xcontext);

        Collection<XWikiRCSNodeInfo> nodes = archive.getNodes();
        Iterator<XWikiRCSNodeInfo> nodeIterator = nodes.iterator();

        XWikiRCSNodeInfo node = null;
        if (nodeIterator.hasNext()) {
            node = nodeIterator.next();

            for (DocumentAncestor ancestor : ancestors) {
                Version ancestorVersion = new Version(ancestor.getVersion());

                while (node != null && node.getVersion().isGreaterThan(ancestorVersion)) {
                    if (nodeIterator.hasNext()) {
                        node = nodeIterator.next();
                    } else {
                        // No common ancestor could be found
                        return null;
                    }
                }

                // Found the common ancestor
                if (node.getVersion().equals(ancestorVersion) && node.getDate().equals(ancestor.getDate())) {
                    return this.revisionProvider.getRevision(currentDocument, node.getVersion().toString());
                }
            }
        }

        // No common ancestor could be found
        return null;
    }

    private Set<String> findAuthors(XWikiDocument ancestorDocument, XWikiDocument currentDocument,
        XWikiDocument newDocument, XWikiContext xcontext) throws ReplicationException
    {
        Set<String> authors = new HashSet<>();
        Collection<XWikiRCSNodeInfo> nodes;
        try {
            nodes = newDocument.getDocumentArchive(xcontext).getNodes(newDocument.getRCSVersion(),
                ancestorDocument.getRCSVersion());
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

    /**
     * Notify about a conflict in a specific document.
     * 
     * @param document the document involved in the conflict
     * @param authors the authors
     */
    public void notifyConflict(XWikiDocument document, Collection<String> authors)
    {
        // Create a notification
        this.observation.notify(
            new ReplicationDocumentConflictEvent(document.getDocumentReferenceWithLocale(), authors), "replication",
            document);

        // TODO: mark the document as in conflict
    }
}
