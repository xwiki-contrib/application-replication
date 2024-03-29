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
package org.xwiki.contrib.replication.entity.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.internal.DocumentReplicationControllerInstanceConverter;
import org.xwiki.contrib.replication.entity.internal.EntityReplicationConfigurationUpdater;
import org.xwiki.contrib.replication.entity.internal.EntityReplicationStore;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;

/**
 * Allow manipulating the configuration of replicated entities.
 * 
 * @version $Id$
 */
@Component
@Named("replication.controller")
@Singleton
public class ControllerReplicationScriptService implements ScriptService
{
    @Inject
    private EntityReplicationStore store;

    @Inject
    private EntityReplicationConfigurationUpdater updater;

    @Inject
    private ReplicationInstanceManager instanceManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * @param reference the reference of the entity
     * @return the instances directly configured at this entity level
     * @throws XWikiException when failing to get the instances
     * @throws AccessDeniedException if the current script author does not have the right to use this API
     */
    public Collection<DocumentReplicationControllerInstance> getHibernateEntityReplication(EntityReference reference)
        throws XWikiException, AccessDeniedException
    {
        return this.store.getHibernateEntityReplication(reference);
    }

    /**
     * @param reference the reference of the entity
     * @return the instances to send the entity to
     * @throws XWikiException when failing to get the instances
     * @throws AccessDeniedException if the current script author does not have the right to use this API
     * @throws ReplicationException when failing to access instances
     */
    public Collection<DocumentReplicationControllerInstance> resolveHibernateEntityReplication(
        EntityReference reference) throws XWikiException, AccessDeniedException, ReplicationException
    {
        return this.store.resolveHibernateEntityReplication(reference, false);
    }

    /**
     * @param reference the reference of the entity
     * @param instance the configured instance
     * @return the configuration of the instance
     * @throws XWikiException when failing to get the instances
     * @throws AccessDeniedException if the current script author does not have the right to use this API
     * @throws ReplicationException when failing to access instances
     */
    public DocumentReplicationControllerInstance resolveHibernateEntityReplication(EntityReference reference,
        ReplicationInstance instance) throws XWikiException, AccessDeniedException, ReplicationException
    {
        return this.store.resolveHibernateEntityReplication(reference, instance, false);
    }

    /**
     * @param reference the reference of the entity associated with the configuration
     * @param instances the instance configuration to update
     * @throws AccessDeniedException if the current script author does not have the right to use this API
     * @throws XWikiException when failing to save the configuration
     * @throws ReplicationException when failing to get current instance
     * @throws QueryException when failing to save the configuration
     */
    public void save(EntityReference reference, List<Map<String, Object>> instances)
        throws AccessDeniedException, XWikiException, ReplicationException, QueryException
    {
        this.authorization.checkAccess(Right.PROGRAM);

        List<DocumentReplicationControllerInstance> configurations;
        if (instances == null) {
            configurations = null;
        } else {
            configurations = new ArrayList<>(instances.size());
            ReplicationInstance currentInstance = this.instanceManager.getCurrentInstance();
            boolean currentInstanceFound = false;
            for (Map<String, Object> entry : instances) {
                DocumentReplicationControllerInstance configuration =
                    DocumentReplicationControllerInstanceConverter.toControllerInstance(entry, this.instanceManager);
                if (configuration != null && (configuration.getInstance() == null
                    || configuration.getInstance().getStatus() == Status.REGISTERED)) {
                    if (configuration.getLevel() == DocumentReplicationLevel.REFERENCE
                        && configuration.getDirection() != DocumentReplicationDirection.SEND_ONLY) {
                        // A REFERENCE replication also imply that it's a send only
                        configuration = new DocumentReplicationControllerInstance(configuration.getInstance(),
                            DocumentReplicationLevel.REFERENCE, DocumentReplicationDirection.SEND_ONLY);
                    }

                    configurations.add(configuration);

                    if (configuration.getInstance() == this.instanceManager.getCurrentInstance()) {
                        currentInstanceFound = true;
                    }
                }
            }

            // Add current instance if not already there
            if (!currentInstanceFound) {
                configurations.add(
                    new DocumentReplicationControllerInstance(currentInstance, DocumentReplicationLevel.ALL, null));
            }
        }

        // Save new configuration
        this.updater.save(reference, configurations);
    }
}
