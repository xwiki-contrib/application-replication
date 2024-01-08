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
package org.xwiki.contrib.replication.entity.internal.history;

import java.util.concurrent.CompletableFuture;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(DocumentHistoryDeleteReplicationMessage.TYPE)
public class DocumentHistoryReplicationReceiver extends AbstractDocumentReplicationReceiver
{
    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        String fromVersion = this.messageReader.getMetadata(message,
            DocumentHistoryDeleteReplicationMessage.METADATA_VERSION_FROM, true);
        String toVersion =
            this.messageReader.getMetadata(message, DocumentHistoryDeleteReplicationMessage.METADATA_VERSION_TO, true);

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(documentReference, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to load document to update", e);
        }

        WikiReference currentWiki = xcontext.getWikiReference();
        try {
            // Workaround https://jira.xwiki.org/browse/XWIKI-21761 (XWiki#deleteDocumentVersions does not work when
            // called from a different wiki) by making sure to witch to the wiki of the document before calling it
            // TODO: remove when upgrading to a version of XWiki Standard where the bug is fixed
            xcontext.setWikiReference(document.getDocumentReference().getWikiReference());

            xcontext.getWiki().deleteDocumentVersions(document, fromVersion, toVersion, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to delete document versions", e);
        } finally {
            xcontext.setWikiReference(currentWiki);
        }
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return this.documentRelay.relay(message, DocumentReplicationLevel.ALL);
    }
}
