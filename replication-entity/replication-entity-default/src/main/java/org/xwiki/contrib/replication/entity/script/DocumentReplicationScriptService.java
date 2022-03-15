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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.internal.controller.DocumentReplicationControllerConfiguration;
import org.xwiki.contrib.replication.entity.internal.controller.DocumentReplicationControllerConfigurationStore;
import org.xwiki.contrib.replication.entity.internal.index.ReplicationDocumentStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Expose some document replication related APIs.
 * 
 * @version $Id$
 */
@Component
@Named("replication.document")
@Singleton
public class DocumentReplicationScriptService implements ScriptService
{
    @Inject
    private DocumentReplicationController defaultController;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReplicationControllerConfiguration controllerConfiguration;

    @Inject
    private DocumentReplicationControllerConfigurationStore controllerStore;

    @Inject
    private ReplicationDocumentStore documentStore;

    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * Indicate the list of registered instances this document should be replicated to.
     * 
     * @param documentReference the reference of the document about to be replicated
     * @return the registered instances on which to replicate the document
     * @throws ReplicationException when failing to get the instances
     */
    public List<DocumentReplicationControllerInstance> getDocumentInstances(DocumentReference documentReference)
        throws ReplicationException
    {
        return this.defaultController.getReplicationConfiguration(documentReference);
    }

    /**
     * Force doing a full replication of a given document.
     * 
     * @param documentReference the reference of the document to replicate
     * @throws XWikiException when failing to load the document
     * @throws ReplicationException when failing to send the document
     * @throws AccessDeniedException when the current author is not allowed to use this API
     */
    public void replicateCompleteDocument(DocumentReference documentReference)
        throws XWikiException, ReplicationException, AccessDeniedException
    {
        this.authorization.checkAccess(Right.PROGRAM);

        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

        this.defaultController.sendCompleteDocument(document);
    }

    /**
     * @return the available entity replication controllers
     * @throws ReplicationException when failing to get the controllers
     */
    public Set<String> getDocumentReplicationControllers() throws ReplicationException
    {
        return this.controllerConfiguration.getControllers().keySet();
    }

    /**
     * @param entityReference the reference of the entity to replicate
     * @return the entity replication controller in charge of controlling the replication for the passed entity
     * @throws ReplicationException when failing to get the controller
     */
    public String resolveDocumentReplicationController(EntityReference entityReference) throws ReplicationException
    {
        DocumentReplicationController entityController =
            this.controllerConfiguration.resolveDocumentReplicationController(entityReference);

        for (Map.Entry<String, DocumentReplicationController> entry : this.controllerConfiguration.getControllers()
            .entrySet()) {
            if (entry.getValue() == entityController) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * @param entityReference the reference of the entity for which to get the configured controller name
     * @return the name of the configured controller
     * @throws ReplicationException when failing to get the configuration
     */
    public String getDocumentReplicationController(EntityReference entityReference) throws ReplicationException
    {
        return entityReference != null ? this.controllerStore.getDocumentReplicationController(entityReference) : null;
    }

    /**
     * Update the controller to use for the passed entity.
     * 
     * @param entityReference the reference of the entity
     * @param controller the controller to set
     * @throws ReplicationException when the update of the document configuration fail
     * @throws AccessDeniedException when the current author is not allowed to use this API
     */
    public void setDocumentReplicationController(EntityReference entityReference, String controller)
        throws ReplicationException, AccessDeniedException
    {
        this.authorization.checkAccess(Right.PROGRAM);

        this.controllerStore.setDocumentReplicationController(entityReference, controller);
    }

    /**
     * @param documentReference the reference of the document
     * @return the owner instance of the document
     * @throws ReplicationException when failing to get the owner
     */
    public String getOwner(DocumentReference documentReference) throws ReplicationException
    {
        return this.documentStore.getOwner(documentReference);
    }

    /**
     * @param documentReference the reference of the document
     * @param owner the owner instance of the document
     * @throws ReplicationException when the update of the owner fail
     * @throws AccessDeniedException when the current author is not allowed to use this API
     */
    public void setOwner(DocumentReference documentReference, ReplicationInstance owner)
        throws ReplicationException, AccessDeniedException
    {
        this.authorization.checkAccess(Right.PROGRAM);

        this.documentStore.setOwner(documentReference, owner.getURI());
    }
}
