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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.EntityReplicationBuilders;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Various helpers around {@link DocumentReplicationController}.
 * 
 * @version $Id$
 */
@Component(roles = DocumentReplicationUtils.class)
@Singleton
public class DocumentReplicationUtils
{
    @Inject
    private DocumentReplicationController controller;

    @Inject
    private EntityReplicationBuilders builders;

    @Inject
    private ReplicationDocumentStore store;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private Logger logger;

    /**
     * @param reference the reference of the entity
     * @return true if the current instance is configured to directly replicate changes made to the passed document
     * @throws ReplicationException when failing to get the configuration
     */
    public boolean isReplicated(EntityReference reference) throws ReplicationException
    {
        return !this.controller.getReplicationConfiguration(reference).isEmpty();
    }

    /**
     * @param reference the reference of the entity
     * @param instance the instance for which we want the configuration
     * @return the configuration of the instance
     * @throws ReplicationException when failing to get replication configuration for the passed entity
     */
    public DocumentReplicationControllerInstance getReplicationConfiguration(EntityReference reference,
        ReplicationInstance instance) throws ReplicationException
    {
        for (DocumentReplicationControllerInstance configuration : this.controller
            .getReplicationConfiguration(reference)) {
            if (configuration.getInstance() == instance) {
                return configuration;
            }
        }

        return null;
    }

    /**
     * @param reference the reference of the document
     * @return true if the current instance is the owner of the passed document
     * @throws ReplicationException when failing to get the configuration
     */
    public boolean isOwner(DocumentReference reference) throws ReplicationException
    {
        String owner = this.store.getOwner(reference);

        return isInstance(reference, owner);
    }

    /**
     * @param reference the reference of the document
     * @param uri the URI of the instance
     * @return true if the current instance is the owner of the passed document
     * @throws ReplicationException when failing to get the configuration
     */
    public boolean isInstance(DocumentReference reference, String uri) throws ReplicationException
    {
        ReplicationInstance ownerInstance = this.instances.getInstanceByURI(uri);

        return ownerInstance != null && ownerInstance.getStatus() == null;
    }

    /**
     * @param documentReference the reference of the document for which to share the conflict status
     * @param conflict true if the document has a replication conflict
     */
    public void sendDocumentConflict(DocumentReference documentReference, boolean conflict)
    {
        try {
            this.controller
                .send(this.builders.documentConflictUpdateMessageBuilder(documentReference).conflict(conflict), null);
        } catch (ReplicationException e) {
            this.logger.error("Failed to send a replication message for conflict [{}] on document [{}]", conflict,
                documentReference);
        }
    }
}
