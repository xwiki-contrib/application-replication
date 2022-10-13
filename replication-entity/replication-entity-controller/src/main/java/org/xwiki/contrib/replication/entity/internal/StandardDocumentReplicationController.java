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
import org.xwiki.contrib.replication.entity.AbstractDocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiException;

/**
 * Default implementation of {@link DocumentReplicationController}.
 * 
 * @version $Id$
 */
@Component
@Named("standard")
@Singleton
public class StandardDocumentReplicationController extends AbstractDocumentReplicationController
{
    @Inject
    private EntityReplicationStore store;

    @Override
    public List<DocumentReplicationControllerInstance> getReplicationConfiguration(EntityReference entityReference)
        throws ReplicationException
    {
        return getConfiguration(entityReference, false);
    }

    @Override
    public List<DocumentReplicationControllerInstance> getRelayConfiguration(EntityReference entityReference)
        throws ReplicationException
    {
        return getConfiguration(entityReference, true);
    }

    private DocumentReplicationControllerInstance getConfiguration(EntityReference entityReference,
        ReplicationInstance instance) throws ReplicationException
    {
        try {
            return this.store.resolveHibernateEntityReplication(entityReference, instance);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to retrieve configuration for instance [" + instance.getURI() + "]",
                e);
        }
    }

    private List<DocumentReplicationControllerInstance> getConfiguration(EntityReference entityReference, boolean relay)
        throws ReplicationException
    {
        // Get current instance configuration
        DocumentReplicationControllerInstance currentInstance =
            getConfiguration(entityReference, this.instanceManager.getCurrentInstance());

        // Don't replicate anything if the instance is not a relay and is forbidden from modifying the
        // document
        if (!relay
            && (currentInstance.isReadonly() || currentInstance.getLevel() == DocumentReplicationLevel.REFERENCE)) {
            return Collections.emptyList();
        }

        // Get full configuration
        Collection<DocumentReplicationControllerInstance> instances;
        try {
            instances = this.store.resolveHibernateEntityReplication(entityReference);
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
}
