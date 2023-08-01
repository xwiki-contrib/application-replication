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
package org.xwiki.contrib.replication.entity.internal.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerConfiguration;
import org.xwiki.contrib.replication.entity.DocumentReplicationMessageReader;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named("standard")
public class StandardDocumentReplicationControllerConfiguration implements DocumentReplicationControllerConfiguration
{
    @Inject
    private ConfigurationSource configurationSource;

    @Inject
    private DocumentReplicationControllerConfigurationStore store;

    @Inject
    private EntityReferenceFactory referenceFactory;

    @Inject
    private ComponentManager componentManager;

    @Inject
    @Named("minimum")
    private DocumentReplicationController minimumController;

    @Inject
    private DocumentReplicationMessageReader documentMessageTool;

    private Map<String, DocumentReplicationController> controllers;

    private Optional<Map<String, DocumentReplicationController>> publicControllers;

    private DocumentReplicationController fallbackController;

    private final ReentrantLock lock = new ReentrantLock();

    private final Map<EntityReference, ControllerCacheEntry> cache = new ConcurrentHashMap<>();

    private class ControllerCacheEntry
    {
        private DocumentReplicationController controller;

        private boolean inherited;

        private Set<EntityReference> children;
    }

    @Override
    public Optional<Map<String, DocumentReplicationController>> getControllers() throws ReplicationException
    {
        if (this.publicControllers == null) {
            this.publicControllers = Optional.of(Collections.unmodifiableMap(getInternalControllers()));
        }

        return this.publicControllers;
    }

    private Map<String, DocumentReplicationController> getInternalControllers() throws ReplicationException
    {
        if (this.controllers == null) {
            try {
                Map<String, DocumentReplicationController> standardControllers =
                    new HashMap<>(this.componentManager.getInstanceMap(DocumentReplicationController.class));

                // Get rid of the default controller which is not a real one
                standardControllers.remove("default");

                // Remove the minimum controller which does nothing and is only here as a fallback
                standardControllers.remove("minimum");

                this.controllers = standardControllers;

                if (this.controllers.isEmpty()) {
                    // Fallback on the minimum controller is nothing else is available
                    this.fallbackController = this.minimumController;
                } else {
                    this.fallbackController = this.controllers.values().iterator().next();
                }
            } catch (ComponentLookupException e) {
                throw new ReplicationException("Failed to get controllers", e);
            }
        }

        return this.controllers;
    }

    /**
     * @param entityReference the reference of the entity to remove from the cache along with its children
     */
    public void invalidate(EntityReference entityReference)
    {
        this.lock.lock();

        try {
            ControllerCacheEntry entry = this.cache.remove(entityReference);

            if (entry != null && entry.children != null) {
                entry.children.forEach(this::invalidateChild);
            }
        } finally {
            this.lock.unlock();
        }
    }

    private void invalidateChild(EntityReference entityReference)
    {
        ControllerCacheEntry entry = this.cache.get(entityReference);
        if (entry != null && entry.inherited) {
            this.cache.remove(entityReference);

            if (entry.children != null) {
                entry.children.forEach(this::invalidate);
            }
        }
    }

    private DocumentReplicationController getFromCache(EntityReference reference)
    {
        ControllerCacheEntry entry = this.cache.get(reference);

        return entry != null ? entry.controller : null;
    }

    private ControllerCacheEntry addToCache(EntityReference reference)
    {
        return addToCache(reference, (EntityReference) null);
    }

    private ControllerCacheEntry addToCache(EntityReference reference, EntityReference child)
    {
        ControllerCacheEntry entry = this.cache.get(reference);

        // Create the entry if it does not exist
        if (entry == null) {
            entry = new ControllerCacheEntry();
            this.cache.put(reference, entry);
        }

        // Add child entity
        if (child != null) {
            if (entry.children == null) {
                entry.children = ConcurrentHashMap.newKeySet();
            }

            entry.children.add(child);
        }

        // Make sure parents exist
        if (reference.getParent() != null) {
            addToCache(reference.getParent(), reference);
        }

        return entry;
    }

    private void addToCache(EntityReference reference, DocumentReplicationController controller, boolean inherited)
    {
        ControllerCacheEntry entry = addToCache(reference);

        entry.controller = controller;
        entry.inherited = inherited;
    }

    @Override
    public DocumentReplicationController resolveDocumentReplicationController(EntityReference entityReference)
        throws ReplicationException
    {
        // If there is only one controller , skip the configuration entirely
        if (getInternalControllers().size() == 1) {
            return this.fallbackController;
        }

        // null reference means global configuration
        if (entityReference == null) {
            return getGlobalDocumentReplicationController();
        }

        // Try the cache
        DocumentReplicationController controller = getFromCache(entityReference);
        if (controller != null) {
            return controller;
        }

        this.lock.lock();
        try {
            // Load the controller
            if (entityReference.getType() == EntityType.WIKI) {
                controller = loadDocumentReplicationController(
                    this.referenceFactory.getReference(entityReference instanceof WikiReference
                        ? (WikiReference) entityReference : new WikiReference(entityReference)));
            } else {
                EntityReference spaceReference = entityReference.extractReference(EntityType.SPACE);
                if (spaceReference != null) {
                    controller = loadDocumentReplicationController(
                        this.referenceFactory.getReference(spaceReference instanceof SpaceReference
                            ? (SpaceReference) spaceReference : new SpaceReference(spaceReference)));
                }
            }
        } finally {
            this.lock.unlock();
        }

        return controller != null ? controller : this.fallbackController;
    }

    @Override
    public DocumentReplicationController resolveDocumentReplicationController(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        EntityReference reference = this.documentMessageTool.getEntityReference(message);

        return resolveDocumentReplicationController(reference);
    }

    @Override
    public DocumentReplicationController resolveDocumentReplicationController(XWikiDocument document)
        throws ReplicationException
    {
        return resolveDocumentReplicationController(document.getDocumentReference());
    }

    private synchronized DocumentReplicationController loadDocumentReplicationController(SpaceReference spaceReference)
        throws ReplicationException
    {
        DocumentReplicationController controller = null;
        boolean inherited = false;

        // Look at space configuration
        String controllerName = this.store.getDocumentReplicationController(spaceReference);
        if (controllerName != null) {
            controller = getInternalControllers().get(controllerName);
        }

        // Inherit
        if (controller == null) {
            if (spaceReference.getParent() instanceof SpaceReference) {
                controller = loadDocumentReplicationController((SpaceReference) spaceReference.getParent());
            } else if (spaceReference.getParent() instanceof WikiReference) {
                controller = loadDocumentReplicationController((WikiReference) spaceReference.getParent());
            } else {
                throw new ReplicationException(
                    "Unsupported entity reference type [" + spaceReference.getParent().getType() + "]");
            }

            inherited = true;
        }

        // Add the controller to the cache
        addToCache(spaceReference, controller, inherited);

        return controller;
    }

    private DocumentReplicationController loadDocumentReplicationController(WikiReference wikiReference)
        throws ReplicationException
    {
        DocumentReplicationController controller = null;
        boolean inherited = false;

        // Look at space configuration
        String controllerName = this.store.getDocumentReplicationController(wikiReference);
        if (controllerName != null) {
            controller = getInternalControllers().get(controllerName);
        }

        // Try the general configuration
        if (controller == null) {
            controller = getGlobalDocumentReplicationController();

            inherited = true;
        }

        // Add the controller to the cache
        addToCache(wikiReference, controller, inherited);

        return controller;
    }

    private DocumentReplicationController getGlobalDocumentReplicationController() throws ReplicationException
    {
        DocumentReplicationController controller = null;

        String controllerName = this.configurationSource.getProperty("replication.entity.controller");
        if (controllerName != null) {
            controller = getInternalControllers().get(controllerName);
        }

        // Fallback on the first found controller in the list
        return controller != null ? controller : this.fallbackController;
    }
}
