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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component(roles = DocumentReplicationControllerConfigurationStore.class)
@Singleton
public class DocumentReplicationControllerConfigurationStore
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private DocumentReference getConfigurationReference(EntityReference entityReference)
    {
        if (entityReference != null) {
            if (entityReference.getType() == EntityType.WIKI) {
                return new DocumentReference(XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE,
                    entityReference instanceof WikiReference ? (WikiReference) entityReference
                        : new WikiReference(entityReference));
            } else {
                EntityReference spaceReference = entityReference.extractReference(EntityType.SPACE);
                if (spaceReference != null) {
                    return new DocumentReference("WebPreferences", entityReference instanceof SpaceReference
                        ? (SpaceReference) entityReference : new SpaceReference(entityReference));
                }
            }
        }

        return null;
    }

    private BaseObject getConfigurationObject(EntityReference entityRefeference, boolean create, XWikiContext xcontext)
        throws ReplicationException
    {
        DocumentReference configurationDocumentReference = getConfigurationReference(entityRefeference);

        if (configurationDocumentReference == null) {
            throw new ReplicationException("Unsupported reference [" + entityRefeference + "]");
        }

        XWikiDocument configurationDocument;
        try {
            configurationDocument = xcontext.getWiki().getDocument(configurationDocumentReference, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException(
                "Failed to access the entity controller configuration for entity [" + entityRefeference + "]", e);
        }

        return configurationDocument.getXObject(ReplicationEntityConfigurationClassInitializer.CLASS_REFERENCE, create,
            xcontext);
    }

    /**
     * @param entityReference the reference of the entity for which to get the configured controller name
     * @return the name of the configured controller
     * @throws ReplicationException when failing to get the configuration
     */
    public String getDocumentReplicationController(EntityReference entityReference) throws ReplicationException
    {
        XWikiContext xcontext = xcontextProvider.get();

        BaseObject configurationObject = getConfigurationObject(entityReference, false, xcontext);

        if (configurationObject != null) {
            return configurationObject.getStringValue(ReplicationEntityConfigurationClassInitializer.FIELD_CONTROLLER);
        }

        return null;
    }

    /**
     * Set the controller to use by default for the passed entity.
     * 
     * @param entityRefeference the reference of the entity
     * @param controller the controller to set
     * @throws ReplicationException when failing to set the controller for the passed wiki
     */
    public void setDocumentReplicationController(EntityReference entityRefeference, String controller)
        throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        BaseObject configurationObject = getConfigurationObject(entityRefeference, true, xcontext);

        configurationObject.setStringValue(ReplicationEntityConfigurationClassInitializer.FIELD_CONTROLLER, controller);

        try {
            xcontext.getWiki().saveDocument(configurationObject.getOwnerDocument(), xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException(
                "Failed to save the entity controller configuration for entity [" + entityRefeference + "]", e);
        }
    }
}
