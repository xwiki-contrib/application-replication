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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

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
    @Inject
    private DocumentReplicationController controller;

    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        // Create an empty document
        XWikiDocument document = new XWikiDocument(documentReference);

        // Just indicate who created it
        DocumentReference creatorReference = this.documentMessageTool.getMetadata(message,
            DocumentReferenceReplicationMessage.METADATA_CREATOR, true, DocumentReference.class);
        document.setCreatorReference(creatorReference);
        document.setAuthorReference(creatorReference);
        document.setContentAuthorReference(creatorReference);
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
    }

    @Override
    public void relay(ReplicationReceiverMessage message) throws ReplicationException
    {
        relay(message, DocumentReplicationLevel.REFERENCE);
    }
}
