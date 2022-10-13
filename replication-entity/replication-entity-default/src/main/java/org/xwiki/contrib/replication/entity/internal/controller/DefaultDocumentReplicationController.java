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
package org.xwiki.contrib.replication.entity.internal.controller;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerConfiguration;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.ReplicationSenderMessageProducer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of DocumentReplicationController redirecting to actual {@link DocumentReplicationController}
 * according to the configuration.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultDocumentReplicationController implements DocumentReplicationController
{
    @Inject
    private DocumentReplicationControllerConfiguration configuration;

    private DocumentReplicationController getController(EntityReference documentReference) throws ReplicationException
    {
        return this.configuration.resolveDocumentReplicationController(documentReference);
    }

    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference)
        throws ReplicationException
    {
        return getController(entityReference).getReplicationConfiguration(entityReference);
    }

    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference,
        Collection<String> receivers) throws ReplicationException
    {
        return getController(entityReference).getReplicationConfiguration(entityReference, receivers);
    }

    @Override
    public List<DocumentReplicationControllerInstance> getRelayConfiguration(EntityReference entityReference)
        throws ReplicationException
    {
        return getController(entityReference).getRelayConfiguration(entityReference);
    }

    @Override
    public void onDocumentCreated(XWikiDocument document) throws ReplicationException
    {
        getController(document.getDocumentReference()).onDocumentCreated(document);
    }

    @Override
    public void onDocumentUpdated(XWikiDocument document) throws ReplicationException
    {
        getController(document.getDocumentReference()).onDocumentUpdated(document);
    }

    @Override
    public void onDocumentDeleted(XWikiDocument document) throws ReplicationException
    {
        getController(document.getDocumentReference()).onDocumentDeleted(document);
    }

    @Override
    public void onDocumentHistoryDelete(XWikiDocument document, String from, String to) throws ReplicationException
    {
        getController(document.getDocumentReference()).onDocumentHistoryDelete(document, from, to);
    }

    @Override
    public void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel) throws ReplicationException
    {
        getController(entityReference).send(messageProducer, entityReference, minimumLevel);
    }

    @Override
    public void send(ReplicationSenderMessageProducer messageProducer, EntityReference entityReference,
        DocumentReplicationLevel minimumLevel, Collection<String> receivers) throws ReplicationException
    {
        getController(entityReference).send(messageProducer, entityReference, minimumLevel, receivers);
    }

    @Override
    public void replicateDocument(DocumentReference documentReference, Collection<String> receivers)
        throws ReplicationException
    {
        getController(documentReference).replicateDocument(documentReference, receivers);
    }

    @Override
    public void sendCompleteDocument(XWikiDocument document) throws ReplicationException
    {
        getController(document.getDocumentReference()).sendCompleteDocument(document);
    }

    @Override
    public void sendDocumentRepair(XWikiDocument document, Collection<String> authors) throws ReplicationException
    {
        getController(document.getDocumentReference()).sendDocumentRepair(document, authors);
    }

    @Override
    public boolean receiveREFERENCEDocument(XWikiDocument document, ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return getController(document.getDocumentReference()).receiveREFERENCEDocument(document, message);
    }
}
