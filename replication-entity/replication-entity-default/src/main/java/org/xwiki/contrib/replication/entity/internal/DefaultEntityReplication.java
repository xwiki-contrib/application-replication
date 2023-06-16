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

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
import org.xwiki.contrib.replication.entity.internal.probe.DocumentUpdateProbeRequestReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultEntityReplication implements EntityReplication
{
    @Inject
    private ReplicationDocumentStore documentStore;

    @Inject
    private DocumentReplicationUtils replicationUtils;

    @Inject
    private DocumentReplicationController controller;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private Provider<DocumentUpdateProbeRequestReplicationMessage> probeRequestMessageProvider;

    @Override
    public String getOwner(DocumentReference documentReference) throws ReplicationException
    {
        return this.documentStore.getOwner(documentReference);
    }

    @Override
    public List<String> getOwners(List<DocumentReference> documents) throws ReplicationException
    {
        return this.documentStore.getOwners(documents);
    }

    @Override
    public boolean getConflict(DocumentReference documentReference) throws ReplicationException
    {
        return this.documentStore.getConflict(documentReference);
    }

    @Override
    public void setConflict(DocumentReference documentReference, boolean conflict) throws ReplicationException
    {
        if (this.documentStore.getConflict(documentReference) != conflict) {
            // Update the conflict status
            this.documentStore.setConflict(documentReference, conflict);

            // Indicate the change to other instances
            this.replicationUtils.sendDocumentConflict(documentReference, conflict);
        }
    }

    @Override
    public boolean isReadonly(DocumentReference documentReference) throws ReplicationException
    {
        return this.documentStore.isReadonly(documentReference);
    }

    @Override
    public void setReadonly(DocumentReference documentReference, boolean readonly) throws ReplicationException
    {
        this.documentStore.setReadonly(documentReference, readonly);
    }

    @Override
    public void remove(DocumentReference documentReference) throws ReplicationException
    {
        this.documentStore.remove(documentReference);
    }

    @Override
    public void updateDocumentReadonly(DocumentReference documentReference) throws ReplicationException
    {
        // Get the owner of the document
        String owner = getOwner(documentReference);

        // Check if we can directly figure out if the instance can send updates to the owner
        Boolean readonly = isReadonly(documentReference, owner);
        if (readonly != null) {
            this.documentStore.setReadonly(documentReference, readonly);

            return;
        }

        // Set the document as readonly for now
        this.documentStore.setReadonly(documentReference, true);
        // And a probe to find out if a document update route exist to the owner
        Collection<String> receivers = List.of(owner);
        this.controller.send(m -> {
            DocumentUpdateProbeRequestReplicationMessage sendMessage = this.probeRequestMessageProvider.get();

            sendMessage.initialize(documentReference, receivers, m);

            return sendMessage;
        }, documentReference, DocumentReplicationLevel.ALL, receivers);
    }

    private Boolean isReadonly(DocumentReference documentReference, String owner) throws ReplicationException
    {
        ReplicationInstance ownerInstance = this.instances.getInstanceByURI(owner);

        if (ownerInstance != null) {
            // Check if the current instance is the owner
            if (ownerInstance.getStatus() == null) {
                return true;
            }

            // Check if the instance is allowed to directly send updated to the owner instance
            if (ownerInstance.getStatus() == Status.REGISTERED) {
                DocumentReplicationControllerInstance configuration =
                    this.replicationUtils.getReplicationConfiguration(documentReference, ownerInstance);

                return configuration != null && configuration.getLevel() == DocumentReplicationLevel.ALL
                    && configuration.getDirection() != DocumentReplicationDirection.RECEIVE_ONLY;
            }
        }

        // Check if the instance is allowed to send update to at least one registered instance
        List<DocumentReplicationControllerInstance> configurations =
            this.controller.getReplicationConfiguration(documentReference);

        for (DocumentReplicationControllerInstance configuration : configurations) {
            if (configuration != null && configuration.getLevel() == DocumentReplicationLevel.ALL
                && configuration.getDirection() != DocumentReplicationDirection.RECEIVE_ONLY) {
                // We cannot really know if the instance has a root to the owner since it's allowed to send update
                // message to an instance (which itself may be allowed to send update message to to the owner)
                return null;
            }
        }

        // We could not find any instance to which we can send update message so we know we cannot reach the owner
        return true;
    }
}
