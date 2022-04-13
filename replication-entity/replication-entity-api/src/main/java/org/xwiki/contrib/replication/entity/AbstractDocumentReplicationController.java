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
package org.xwiki.contrib.replication.entity;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public abstract class AbstractDocumentReplicationController implements DocumentReplicationController
{
    @Inject
    protected DocumentReplicationSender sender;

    protected Map<String, Collection<String>> getMetadata(XWikiDocument document) throws ReplicationException
    {
        return null;
    }

    protected Map<String, Collection<String>> getMetadata(EntityReference entity) throws ReplicationException
    {
        return null;
    }

    @Override
    public void onDocumentCreated(XWikiDocument document) throws ReplicationException
    {
        this.sender.sendDocument(document, true, true, getMetadata(document), DocumentReplicationLevel.REFERENCE, null);
    }

    @Override
    public void onDocumentUpdated(XWikiDocument document) throws ReplicationException
    {
        // There is no point in sending a message for each update if the instance is only allowed to
        // replicate the reference
        this.sender.sendDocument(document, false, false, getMetadata(document), DocumentReplicationLevel.ALL, null);
    }

    @Override
    public void onDocumentDeleted(XWikiDocument document) throws ReplicationException
    {
        this.sender.sendDocumentDelete(document.getDocumentReferenceWithLocale(), getMetadata(document), null);
    }

    @Override
    public void onDocumentHistoryDelete(XWikiDocument document, String from, String to) throws ReplicationException
    {
        this.sender.sendDocumentHistoryDelete(document.getDocumentReferenceWithLocale(), from, to,
            getMetadata(document), null);
    }

    @Override
    public void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel) throws ReplicationException
    {
        this.sender.send(messageProducer, entityReference, minimumLevel, getMetadata(entityReference), null);
    }

    @Override
    public void sendCompleteDocument(XWikiDocument document) throws ReplicationException
    {
        this.sender.sendDocument(document, true, false, getMetadata(document), DocumentReplicationLevel.REFERENCE,
            null);
    }

    @Override
    public void sendDocumentRepair(XWikiDocument document, Collection<String> authors) throws ReplicationException
    {
        this.sender.sendDocumentRepair(document, authors, getMetadata(document), null);
    }

    @Override
    public boolean receiveREFERENCEDocument(XWikiDocument document, ReplicationReceiverMessage message)
    {
        return false;
    }
}
