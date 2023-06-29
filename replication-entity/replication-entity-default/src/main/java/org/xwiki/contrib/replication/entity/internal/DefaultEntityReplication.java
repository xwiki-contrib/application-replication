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

import java.util.List;

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
}
