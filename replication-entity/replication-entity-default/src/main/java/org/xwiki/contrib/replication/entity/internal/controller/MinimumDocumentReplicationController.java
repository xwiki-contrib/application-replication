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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.EntityReplicationSenderMessageBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The minimum controller which does not replicate anything.
 * 
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named("minimum")
public class MinimumDocumentReplicationController implements DocumentReplicationController
{
    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference)
        throws ReplicationException
    {
        return List.of();
    }

    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(XWikiDocument document)
        throws ReplicationException
    {
        return List.of();
    }

    @Override
    public List<DocumentReplicationControllerInstance> getRelayConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return List.of();
    }

    @Override
    public DocumentReplicationControllerInstance getReceiveConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return null;
    }

    @Override
    public void onDocumentCreated(XWikiDocument document) throws ReplicationException
    {
        // Do nothing
    }

    @Override
    public void onDocumentUpdated(XWikiDocument document) throws ReplicationException
    {
        // Do nothing
    }

    @Override
    public void onDocumentDeleted(XWikiDocument document) throws ReplicationException
    {
        // Do nothing
    }

    @Override
    public void onDocumentHistoryDelete(XWikiDocument document, String from, String to) throws ReplicationException
    {
        // Do nothing
    }

    @Override
    public boolean receiveREFERENCEDocument(XWikiDocument document, ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return false;
    }

    @Override
    public ReplicationReceiverMessage filter(ReplicationReceiverMessage message) throws ReplicationException
    {
        return message;
    }

    @Override
    public void send(EntityReplicationSenderMessageBuilder messageBuilder) throws ReplicationException
    {
        // Do nothing
    }

    @Override
    public void send(EntityReplicationSenderMessageBuilder messageBuilder,
        List<DocumentReplicationControllerInstance> customConfigurations) throws ReplicationException
    {
        // Do nothing
    }

    @Override
    public void sendDocument(DocumentReference documentReference) throws ReplicationException
    {
        // Do nothing
    }
}
