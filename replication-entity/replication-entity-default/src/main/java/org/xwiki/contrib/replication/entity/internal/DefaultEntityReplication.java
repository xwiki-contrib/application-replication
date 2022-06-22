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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
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

    /**
     * @param documentReference the reference of the document
     * @return the owner instance of the document
     * @throws ReplicationException when failing to get the owner
     */
    @Override
    public String getOwner(DocumentReference documentReference) throws ReplicationException
    {
        return this.documentStore.getOwner(documentReference);
    }

    /**
     * @param documentReference the reference of the document
     * @return true if the document has a replication conflict
     * @throws ReplicationException when failing to get the owner
     */
    @Override
    public boolean getConflict(DocumentReference documentReference) throws ReplicationException
    {
        return this.documentStore.getConflict(documentReference);
    }

    /**
     * @param documentReference the identifier of the document
     * @param conflict true if the document has a replication conflict
     * @throws ReplicationException when failing to update the conflict marker
     */
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
}
