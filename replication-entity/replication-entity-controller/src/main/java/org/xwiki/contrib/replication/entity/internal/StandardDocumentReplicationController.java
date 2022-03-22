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
package org.xwiki.contrib.replication.entity.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.DocumentReplicationSender;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link DocumentReplicationController}.
 * 
 * @version $Id$
 */
@Component
@Named("standard")
@Singleton
public class StandardDocumentReplicationController implements DocumentReplicationController
{
    @Inject
    private EntityReplicationStore store;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private DocumentReplicationSender sender;

    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(DocumentReference documentReference)
        throws ReplicationException
    {
        return getConfiguration(documentReference, false);
    }

    @Override
    public List<DocumentReplicationControllerInstance> getRelayConfiguration(DocumentReference documentReference)
        throws ReplicationException
    {
        return getConfiguration(documentReference, true);
    }

    private DocumentReplicationControllerInstance getConfiguration(DocumentReference documentReference,
        ReplicationInstance instance) throws ReplicationException
    {
        try {
            return this.store.resolveHibernateEntityReplication(documentReference, instance);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to retrieve configuration for instance [" + instance.getURI() + "]",
                e);
        }
    }

    private List<DocumentReplicationControllerInstance> getConfiguration(DocumentReference documentReference,
        boolean relay) throws ReplicationException
    {
        // Get current instance configuration
        DocumentReplicationControllerInstance currentInstance =
            getConfiguration(documentReference, this.instanceManager.getCurrentInstance());

        // Don't replicate anything if the instance is not a relay and is forbidden from modifying the
        // document
        if (!relay
            && (currentInstance.isReadonly() || currentInstance.getLevel() == DocumentReplicationLevel.REFERENCE)) {
            return Collections.emptyList();
        }

        // Get full configuration
        Collection<DocumentReplicationControllerInstance> instances;
        try {
            instances = this.store.resolveHibernateEntityReplication(documentReference);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to retrieve instances from the store", e);
        }

        // Filter the instances
        List<DocumentReplicationControllerInstance> filteredInstances = new ArrayList<>(instances.size());
        for (DocumentReplicationControllerInstance instance : instances) {
            // Make sure to select only registered instances (in case the configuration is out of sync)
            // Don't relay messages to an instance with higher replication level
            if (instance.getInstance().getStatus() == Status.REGISTERED
                && ordinal(currentInstance.getLevel()) >= ordinal(instance.getLevel())) {
                filteredInstances.add(instance);
            }
        }

        return filteredInstances;
    }

    private int ordinal(DocumentReplicationLevel level)
    {
        return level != null ? level.ordinal() : -1;
    }

    @Override
    public void onDocumentCreated(XWikiDocument document) throws ReplicationException
    {
        this.sender.sendDocument(document, true, true, null, DocumentReplicationLevel.REFERENCE, null);
    }

    @Override
    public void onDocumentUpdated(XWikiDocument document) throws ReplicationException
    {
        // There is no point in sending a message for each update if the instance is only allowed to
        // replicate the reference
        this.sender.sendDocument(document, false, false, null, DocumentReplicationLevel.ALL, null);
    }

    @Override
    public void onDocumentDeleted(XWikiDocument document) throws ReplicationException
    {
        this.sender.sendDocumentDelete(document.getDocumentReferenceWithLocale(), null, null);
    }

    @Override
    public void onDocumentHistoryDelete(XWikiDocument document, String from, String to) throws ReplicationException
    {
        this.sender.sendDocumentHistoryDelete(document.getDocumentReferenceWithLocale(), from, to, null, null);
    }

    @Override
    public void sendCompleteDocument(XWikiDocument document) throws ReplicationException
    {
        this.sender.sendDocument(document, true, false, null, DocumentReplicationLevel.REFERENCE, null);
    }

    @Override
    public void sendDocumentRepair(XWikiDocument document, Collection<String> authors) throws ReplicationException
    {
        this.sender.sendDocumentRepair(document, authors, null, null);
    }

    @Override
    public boolean receiveREFERENCEDocument(XWikiDocument document, ReplicationReceiverMessage message)
    {
        // Nothing to do
        return false;
    }
}
