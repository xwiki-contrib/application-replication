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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
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
        Map<String, DocumentReplicationControllerInstance> configurations = optimizeConfiguration(message);

        try {
            this.store.storeHibernateEntityReplication(entityReference,
                configurations != null ? configurations.values() : null);
        } catch (XWikiException e) {
            throw new ReplicationException(
                "Failed to store replication configuration for entity [" + entityReference + "]", e);
        }

        // TOTO 1.13.0: Update the readonly status of the document if the configuration changed
        //this.entityReplication.updateDocumentReadonly(documentReference);
    }

    private Map<String, DocumentReplicationControllerInstance> optimizeConfiguration(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        Collection<String> values =
            message.getCustomMetadata().get(EntityReplicationControllerMessage.METADATA_CONFIGURATION);
        Map<String, DocumentReplicationControllerInstance> configurations =
            DocumentReplicationControllerInstanceConverter.toControllerInstanceMap(values, this.instances);

        // Convert the configuration to current instance point of view
        if (configurations != null) {
            configurations = optimizeConfiguration(message, configurations);
        }

        return configurations;
    }

    private Map<String, DocumentReplicationControllerInstance> optimizeConfiguration(ReplicationReceiverMessage message,
        Map<String, DocumentReplicationControllerInstance> configurations) throws ReplicationException
    {
        Map<String, DocumentReplicationControllerInstance> optimizedConfigurations =
            new LinkedHashMap<>(configurations.size());

        // Add wildcard configuration
        DocumentReplicationControllerInstance allConfiguration = getAllConfiguration(configurations);
        if (allConfiguration != null) {
            optimizedConfigurations.put(null, allConfiguration);
        }

        // Add current instance if not already there
        DocumentReplicationControllerInstance currentConfiguration =
            configurations.get(this.instances.getCurrentInstance().getURI());
        if (currentConfiguration == null) {
            if (allConfiguration == null) {
                // The entity is not replicated at all with the current instance
                return Collections.emptyMap();
            }

            ReplicationInstance currentInstance = this.instances.getCurrentInstance();

            currentConfiguration = new DocumentReplicationControllerInstance(currentInstance,
                allConfiguration.getLevel(), allConfiguration.getDirection());

            optimizedConfigurations.put(currentInstance.getURI(), currentConfiguration);
        }

        // Invert sending on the source if needed and if the source is known
        ReplicationInstance sourceInstance = this.instances.getRegisteredInstanceByURI(message.getSource());
        if (sourceInstance != null) {
            // If current instance is receive_only then the source is too (from current instance point of view)
            if (currentConfiguration.getDirection() == DocumentReplicationDirection.RECEIVE_ONLY) {
                // Keep a dedicated configuration for the source since it won't behave the same as wildcard in case of
                // relay
                optimizedConfigurations.put(sourceInstance.getURI(), new DocumentReplicationControllerInstance(
                    sourceInstance, DocumentReplicationLevel.ALL, DocumentReplicationDirection.RECEIVE_ONLY));
            }
        }

        for (DocumentReplicationControllerInstance configuration : configurations.values()) {
            // Skip configurations already handled above
            if (configuration.getInstance() != null
                && !optimizedConfigurations.containsKey(configuration.getInstance().getURI())
                && !isPartOfWildcard(currentConfiguration, allConfiguration)) {
                // Skip not registered instances
                optimizedConfigurations.put(configuration.getInstance().getURI(), configuration);
            }
        }

        return optimizedConfigurations;
    }

    private boolean isPartOfWildcard(DocumentReplicationControllerInstance configuration,
        DocumentReplicationControllerInstance allConfiguration)
    {
        // Check if the configurations is identical to the wildcard configuration
        return allConfiguration != null && configuration.getLevel() == allConfiguration.getLevel()
            && configuration.getDirection() == allConfiguration.getDirection();
    }

    private DocumentReplicationControllerInstance getAllConfiguration(
        Map<String, DocumentReplicationControllerInstance> resolvedConfiguration)
    {
        DocumentReplicationControllerInstance allConfiguration = resolvedConfiguration.get(null);

        if (allConfiguration != null && allConfiguration.getDirection() == DocumentReplicationDirection.SEND_ONLY) {
            // Since it's a send only replication from the point of view of the source instance it means that every
            // other instances is in receive only mode
            return new DocumentReplicationControllerInstance(null, allConfiguration.getLevel(),
                DocumentReplicationDirection.RECEIVE_ONLY);
        }

        return allConfiguration;
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> relay(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        // We relay the optimized configuration instead of the source configuration
        Map<String, DocumentReplicationControllerInstance> configurations = optimizeConfiguration(message);

        // Relay the configuration
        return this.controlSender.relay(message, configurations != null ? configurations.values() : null);
    }
}
