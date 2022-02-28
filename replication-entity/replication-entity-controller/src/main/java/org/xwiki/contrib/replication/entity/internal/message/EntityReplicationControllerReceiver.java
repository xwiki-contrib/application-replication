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
package org.xwiki.contrib.replication.entity.internal.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.internal.AbstractEntityReplicationReceiver;
import org.xwiki.contrib.replication.entity.internal.DocumentReplicationControllerInstanceConverter;
import org.xwiki.contrib.replication.entity.internal.EntityReplicationStore;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(EntityReplicationControllerMessage.TYPE)
public class EntityReplicationControllerReceiver extends AbstractEntityReplicationReceiver
{
    @Inject
    private EntityReplicationStore store;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private EntityReplicationControllerSender controlSender;

    @Override
    protected void receiveEntity(ReplicationReceiverMessage message, EntityReference entityReference,
        XWikiContext xcontext) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> configurations = optimizeConfiguration(message);

        try {
            this.store.storeHibernateEntityReplication(entityReference, configurations);
        } catch (XWikiException e) {
            throw new ReplicationException(
                "Failed to store replication configuration for entity [" + entityReference + "]", e);
        }
    }

    private List<DocumentReplicationControllerInstance> optimizeConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        Collection<String> values =
            message.getCustomMetadata().get(EntityReplicationControllerMessage.METADATA_CONFIGURATION);
        List<DocumentReplicationControllerInstance> configurations =
            DocumentReplicationControllerInstanceConverter.toControllerInstances(values, this.instances);

        // Convert the configuration to current instance point of view
        if (configurations != null) {
            configurations = optimizeConfiguration(configurations);
        }

        return configurations;
    }

    private List<DocumentReplicationControllerInstance> optimizeConfiguration(
        List<DocumentReplicationControllerInstance> configurations) throws ReplicationException
    {
        List<DocumentReplicationControllerInstance> optimizedConfigurations = new ArrayList<>(configurations.size());

        DocumentReplicationControllerInstance allConfiguration = getAllConfiguration(configurations);
        DocumentReplicationControllerInstance currentConfiguration = getCurrentConfiguration(configurations);

        // Add current instance if not already there
        if (currentConfiguration == null) {
            if (allConfiguration == null) {
                return Collections.emptyList();
            }

            optimizedConfigurations.add(new DocumentReplicationControllerInstance(this.instances.getCurrentInstance(),
                allConfiguration.getLevel(), allConfiguration.isReadonly()));
        }

        // Optimize the list
        optimizeConfiguration(configurations, allConfiguration, optimizedConfigurations);

        return optimizedConfigurations;
    }

    private void optimizeConfiguration(List<DocumentReplicationControllerInstance> configurations,
        DocumentReplicationControllerInstance allConfiguration,
        List<DocumentReplicationControllerInstance> optimizedConfigurations)
    {
        for (DocumentReplicationControllerInstance configuration : configurations) {
            // Keep wildcard configuration
            // Keep current instance configuration
            if (configuration.getInstance() != null && configuration.getInstance().getStatus() != null) {
                // Don't add not replicated instances
                // No need to keep a specific configuration for an instance having exactly the same as all
                if (configuration.getLevel() == null
                    || (allConfiguration != null && configuration.getLevel() == allConfiguration.getLevel()
                        && configuration.isReadonly() == allConfiguration.isReadonly())) {
                    // Don't add not replicated instances
                    continue;
                }
            }

            optimizedConfigurations.add(configuration);
        }
    }

    private DocumentReplicationControllerInstance getAllConfiguration(
        List<DocumentReplicationControllerInstance> resolvedConfiguration)
    {
        for (DocumentReplicationControllerInstance configuredInstance : resolvedConfiguration) {
            if (configuredInstance.getInstance() == null) {
                return configuredInstance;
            }
        }

        return null;
    }

    private DocumentReplicationControllerInstance getCurrentConfiguration(
        List<DocumentReplicationControllerInstance> resolvedConfiguration)
    {
        // Try to find the current instance
        for (DocumentReplicationControllerInstance configuredInstance : resolvedConfiguration) {
            if (configuredInstance.getInstance() != null && configuredInstance.getInstance().getStatus() == null) {
                return configuredInstance;
            }
        }

        return null;
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        // We relay the optimized configuration instead of the source configuration
        List<DocumentReplicationControllerInstance> configurations = optimizeConfiguration(message);

        // Relay the configuration
        return this.controlSender.relay(message, configurations);
    }
}
