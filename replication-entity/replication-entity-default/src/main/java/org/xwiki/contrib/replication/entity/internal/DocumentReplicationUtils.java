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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.internal.conflict.DocumentReplicationConflictMessage;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
import org.xwiki.model.reference.DocumentReference;

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
    private ReplicationDocumentStore documentStore;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private Provider<DocumentReplicationConflictMessage> conflictMessageProvider;

    @Inject
    private Logger logger;

    /**
     * @param reference the reference of the document
     * @return true if the current instance is configured to directly replicate changes made to the passed document
     * @throws ReplicationException when failing to get the configuration
     */
    public boolean isReplicated(DocumentReference reference) throws ReplicationException
    {
        return !this.controller.getReplicationConfiguration(reference).isEmpty();
    }

    /**
     * @param reference the reference of the document
     * @return true if the current instance is the owner of the passed document
     * @throws ReplicationException when failing to get the configuration
     */
    public boolean isOwner(DocumentReference reference) throws ReplicationException
    {
        String owner = this.documentStore.getOwner(reference);

        ReplicationInstance ownerInstance = this.instances.getInstanceByURI(owner);

        return ownerInstance != null && ownerInstance.getStatus() == null;
    }

    /**
     * @param documentReference the reference of the document for which to share the conflict status
     * @param conflict true if the document has a replication conflict
     */
    public void sendDocumentConflict(DocumentReference documentReference, boolean conflict)
    {
        try {
            this.controller.send(m -> {
                DocumentReplicationConflictMessage message = this.conflictMessageProvider.get();

                message.initialize(documentReference, conflict, m);

                return message;
            }, documentReference, DocumentReplicationLevel.ALL);
        } catch (ReplicationException e) {
            this.logger.error("Failed to send a replication message for conflict [{}] on document [{}]", conflict,
                documentReference);
        }
    }
}
