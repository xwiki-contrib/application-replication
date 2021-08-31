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
package org.xwiki.contrib.replication.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.internal.delete.DocumentDeleteReplicationMessage;
import org.xwiki.contrib.replication.internal.history.DocumentHistoryDeleteReplicationMessage;
import org.xwiki.contrib.replication.internal.update.DocumentUpdateReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = DocumentReplicationSender.class)
@Singleton
public class DocumentReplicationSender
{
    @Inject
    private ReplicationSender sender;

    @Inject
    private Provider<DocumentUpdateReplicationMessage> documentMessageProvider;

    @Inject
    private Provider<DocumentDeleteReplicationMessage> documentDeleteMessageProvider;

    @Inject
    private Provider<DocumentHistoryDeleteReplicationMessage> historyMessageProvider;

    /**
     * @param document the document to send
     * @param complete true of the complete document should be send (including history and all attachments)
     * @throws ReplicationException when failing to queue the replication message
     */
    public void sendDocument(XWikiDocument document, boolean complete) throws ReplicationException
    {
        DocumentUpdateReplicationMessage message = this.documentMessageProvider.get();

        if (complete) {
            message.initialize(document.getDocumentReferenceWithLocale());
        } else {
            message.initialize(document.getDocumentReferenceWithLocale(), document.getVersion(),
                document.getOriginalDocument().isNew() ? null : document.getOriginalDocument().getVersion(),
                document.getOriginalDocument().isNew() ? null : document.getOriginalDocument().getDate());
        }

        this.sender.send(message);
    }

    /**
     * @param documentReference the reference of the document to delete
     * @throws ReplicationException when failing to queue the replication message
     */
    public void sendDocumentDelete(DocumentReference documentReference) throws ReplicationException
    {
        DocumentDeleteReplicationMessage message = this.documentDeleteMessageProvider.get();

        message.initialize(documentReference);

        this.sender.send(message);
    }

    /**
     * @param documentReference the reference of the document to send
     * @param from the lowest version to delete
     * @param to the highest version to delete
     * @throws ReplicationException when failing to queue the replication message
     */
    public void sendDocumentHistoryDelete(DocumentReference documentReference, String from, String to)
        throws ReplicationException
    {
        DocumentHistoryDeleteReplicationMessage message = this.historyMessageProvider.get();
        message.initialize(documentReference, from, to);

        this.sender.send(message);
    }
}
