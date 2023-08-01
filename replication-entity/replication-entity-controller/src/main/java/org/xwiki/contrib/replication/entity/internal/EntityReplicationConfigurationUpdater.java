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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.contrib.replication.entity.EntityReplicationBuilders;
import org.xwiki.contrib.replication.entity.internal.message.EntityReplicationControllerSender;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Update the stored configuration and generate required messages.
 * 
 * @version $Id$
 */
@Component(roles = EntityReplicationConfigurationUpdater.class)
@Singleton
public class EntityReplicationConfigurationUpdater
{
    @Inject
    private EntityReplicationStore store;

    @Inject
    private EntityReplicationBuilders builders;

    @Inject
    private DocumentReplicationController controller;

    @Inject
    private EntityReplicationControllerSender configurationSender;

    @Inject
    private EntityReplication entityReplication;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private Logger logger;

    private class Change
    {
        private final DocumentReplicationControllerInstance before;

        private final DocumentReplicationControllerInstance after;

        Change(DocumentReplicationControllerInstance before, DocumentReplicationControllerInstance after)
        {
            this.before = before;
            this.after = after;
        }
    }

    /**
     * @param reference the reference of the entity
     * @param configurations the instance to send the entity to
     * @throws XWikiException when failing to store entity replication configuration
     * @throws ReplicationException when failing to update the configuration
     * @throws QueryException when failing to gather documents to update
     */
    public void save(EntityReference reference, List<DocumentReplicationControllerInstance> configurations)
        throws XWikiException, ReplicationException, QueryException
    {
        // Resolve pre-save configuration
        Map<String, DocumentReplicationControllerInstance> preConfigurations =
            this.store.resolveControllerInstancesMap(this.store.getHibernateEntityReplication(reference), false);

        // Resolve post-save configuration
        Map<String, DocumentReplicationControllerInstance> postConfigurations =
            this.store.resolveControllerInstancesMap(configurations, false);

        // Calculate the configuration diff
        List<DocumentReplicationControllerInstance> newInstances = new ArrayList<>();
        List<DocumentReplicationControllerInstance> removedInstances = new ArrayList<>();
        List<Change> changes = new ArrayList<>();
        diff(preConfigurations, postConfigurations, newInstances, removedInstances, changes);

        // Gather affected documents
        XWikiContext xcontext = this.provider.get();
        List<DocumentReference> documentsToUpdate = newInstances.isEmpty() && removedInstances.isEmpty()
            ? Collections.emptyList() : resolveDocuments(reference, xcontext);

        // Make sure current instance is allowed to do that change
        checkChanges(documentsToUpdate, removedInstances, changes);

        // Save new configuration
        this.store.storeHibernateEntityReplication(reference, configurations);

        // If the document is not replicated at all anymore, remove the document from the replication index
        if (!documentsToUpdate.isEmpty() && configurations.isEmpty()) {
            for (DocumentReference documentToUpdate : documentsToUpdate) {
                this.entityReplication.remove(documentToUpdate);
            }
        }

        // Send unreplicate messages according to configuration diff
        sendUnreplicateMessages(documentsToUpdate, removedInstances, xcontext);

        // Synchronize configuration with other instances
        try {
            this.configurationSender.send(reference, configurations);
        } catch (Exception e) {
            // TODO: put this in a retry queue
            this.logger.error("Failed to notify other instances about the replication configuration change", e);
        }

        // Send add messages according to configuration diff
        sendAddMessages(documentsToUpdate, newInstances, preConfigurations == null || preConfigurations.isEmpty(),
            xcontext);
    }

    private void checkChanges(List<DocumentReference> documentsToUpdate,
        List<DocumentReplicationControllerInstance> removedInstances, List<Change> changes) throws ReplicationException
    {
        for (String owner : this.entityReplication.getOwners(documentsToUpdate)) {
            for (DocumentReplicationControllerInstance removedInstance : removedInstances) {
                if (removedInstance.getInstance().getURI().equals(owner)) {
                    throw new ReplicationException(
                        "Removing owner instance of a document from the replication is not allowed");
                }
            }
            for (Change change : changes) {
                if (change.before.getInstance().getURI().equals(owner)
                    && change.after.getLevel() != DocumentReplicationLevel.ALL) {
                    throw new ReplicationException(
                        "Reducing the replication level of the owner instance of a document is not allowed");
                }
            }
        }
    }

    private DocumentReplicationControllerInstance get(Map<String, DocumentReplicationControllerInstance> configurations,
        String uri)
    {
        if (configurations != null) {
            DocumentReplicationControllerInstance configuration = configurations.get(uri);

            if (configuration != null && configuration.getLevel() != null) {
                return configuration;
            }
        }

        return null;
    }

    private void diff(Map<String, DocumentReplicationControllerInstance> preConfigurations,
        Map<String, DocumentReplicationControllerInstance> postConfigurations,
        List<DocumentReplicationControllerInstance> newInstances,
        List<DocumentReplicationControllerInstance> removedInstances, List<Change> changes)
    {
        if (preConfigurations != null) {
            diffPre(preConfigurations, postConfigurations, newInstances, removedInstances, changes);
        }

        if (postConfigurations != null) {
            diffPost(preConfigurations, postConfigurations, newInstances);
        }
    }

    private void diffPre(Map<String, DocumentReplicationControllerInstance> preConfigurations,
        Map<String, DocumentReplicationControllerInstance> postConfigurations,
        List<DocumentReplicationControllerInstance> newInstances,
        List<DocumentReplicationControllerInstance> removedInstances, List<Change> changes)
    {
        for (DocumentReplicationControllerInstance preConfiguration : preConfigurations.values()) {
            // Skip current instance
            if (preConfiguration.getInstance().getStatus() != null && preConfiguration.getLevel() != null) {
                DocumentReplicationControllerInstance postConfiguration =
                    get(postConfigurations, preConfiguration.getInstance().getURI());

                if (postConfiguration == null) {
                    // An instance configuration was removed
                    removedInstances.add(preConfiguration);
                } else if (postConfiguration.getLevel() != preConfiguration.getLevel()) {
                    // An instance configuration level changed
                    newInstances.add(postConfiguration);
                    changes.add(new Change(preConfiguration, postConfiguration));
                }
            }
        }
    }

    private void diffPost(Map<String, DocumentReplicationControllerInstance> preConfigurations,
        Map<String, DocumentReplicationControllerInstance> postConfigurations,
        List<DocumentReplicationControllerInstance> newInstances)
    {
        for (DocumentReplicationControllerInstance postConfiguration : postConfigurations.values()) {
            // Skip current instance
            if (postConfiguration.getInstance().getStatus() != null && postConfiguration.getLevel() != null) {
                DocumentReplicationControllerInstance preConfiguration =
                    get(preConfigurations, postConfiguration.getInstance().getURI());

                if (preConfiguration == null) {
                    // A new instance configuration was added
                    newInstances.add(postConfiguration);
                }
            }
        }
    }

    private void sendUnreplicateMessages(List<DocumentReference> documentsToUpdate,
        List<DocumentReplicationControllerInstance> removedInstances, XWikiContext xcontext)
        throws XWikiException, ReplicationException
    {
        for (DocumentReference documentReference : documentsToUpdate) {
            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

            if (!document.isNew()) {
                this.controller.send(this.builders.documentUnreplicateMessageBuilder(document), removedInstances);
            }
        }
    }

    private void sendAddMessages(List<DocumentReference> documentsToUpdate,
        List<DocumentReplicationControllerInstance> newInstances, boolean create, XWikiContext xcontext)
        throws XWikiException, ReplicationException
    {
        for (DocumentReference documentReference : documentsToUpdate) {
            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

            if (!document.isNew()) {
                if (create) {
                    this.controller.send(this.builders.documentCreateMessageBuilder(document), newInstances);
                } else {
                    this.controller.send(this.builders.documentCompleteUpdateMessageBuilder(document), newInstances);
                }
            }
        }
    }

    private List<DocumentReference> resolveDocuments(EntityReference reference, XWikiContext xcontext)
        throws XWikiException, QueryException
    {
        List<DocumentReference> documentsToUpdate = new ArrayList<>();

        if (reference.getType() == EntityType.DOCUMENT) {
            DocumentReference documentReference = reference instanceof DocumentReference ? (DocumentReference) reference
                : new DocumentReference(reference);
            if (xcontext.getWiki().exists(documentReference, xcontext)) {
                documentsToUpdate.add(documentReference);
            }
        } else if (reference.getType() == EntityType.SPACE) {
            resolveDocuments(
                reference instanceof SpaceReference ? (SpaceReference) reference : new SpaceReference(reference),
                documentsToUpdate);
        } else if (reference.getType() == EntityType.WIKI) {
            resolveDocuments(null, documentsToUpdate);
        }

        return documentsToUpdate;
    }

    private void resolveDocuments(SpaceReference reference, List<DocumentReference> documentsToUpdate)
        throws XWikiException, QueryException
    {
        // Resolve sub spaces
        List<SpaceReference> spaces = getSpaceReferences(reference);
        for (SpaceReference space : spaces) {
            if (this.store.getHibernateEntityReplication(space) == null) {
                // The space does not have any specific configuration
                resolveDocuments(space, documentsToUpdate);
            }
        }

        // Resolve documents
        List<DocumentReference> documents = getDocumentReferences(reference);
        for (DocumentReference document : documents) {
            if (this.store.getHibernateEntityReplication(document) == null) {
                // The document does not have any specific configuration
                documentsToUpdate.add(document);
            }
        }
    }

    private List<SpaceReference> getSpaceReferences(EntityReference spaceReference) throws QueryException
    {
        // Get the spaces
        Query query;
        if (spaceReference.getType() == EntityType.WIKI) {
            query = this.queryManager.createQuery(
                "select distinct space.name from Space space where space.parent IS NULL order by space.name asc",
                Query.XWQL);
        } else {
            query = this.queryManager.createQuery(
                "select distinct space.name from Space space where space.parent = :parent order by space.name asc",
                Query.XWQL);
            query.bindValue("parent", this.localSerializer.serialize(spaceReference));
        }
        query.setWiki(spaceReference.extractReference(EntityType.WIKI).getName());

        List<String> spaceReferenceStrings = query.execute();

        // Get references
        List<SpaceReference> spaceReferences = new ArrayList<>(spaceReferenceStrings.size());
        for (String spaceReferenceString : spaceReferenceStrings) {
            spaceReferences.add(new SpaceReference(spaceReferenceString, spaceReference));
        }

        return spaceReferences;
    }

    private List<DocumentReference> getDocumentReferences(SpaceReference spaceReference) throws QueryException
    {
        Query query = this.queryManager.createQuery(
            "select distinct doc.name from Document doc where doc.space = :space order by doc.name asc", Query.XWQL);
        query.bindValue("space", this.localSerializer.serialize(spaceReference));
        query.setWiki(spaceReference.getWikiReference().getName());

        List<String> documentNames = query.execute();

        List<DocumentReference> documentReferences = new ArrayList<>(documentNames.size());
        for (String documentName : documentNames) {
            documentReferences.add(new DocumentReference(documentName, spaceReference));
        }

        return documentReferences;
    }
}
