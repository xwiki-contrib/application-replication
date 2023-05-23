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

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerConfiguration;
import org.xwiki.contrib.replication.entity.internal.EntityReplicationConfiguration;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Call the right {@link DocumentReplicationControllerConfiguration} depending on the configuration.
 * 
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
public class DefaultDocumentReplicationControllerConfiguration implements DocumentReplicationControllerConfiguration
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private DocumentReplicationControllerConfiguration controllerConfiguration;

    private DocumentReplicationControllerConfiguration getDocumentReplicationControllerConfiguration()
        throws ReplicationException
    {
        if (this.controllerConfiguration == null) {
            String hint = this.configuration
                .getProperty(EntityReplicationConfiguration.PREFIX + "controller.configuration", "standard");

            try {
                this.controllerConfiguration = this.componentManagerProvider.get()
                    .getInstance(DocumentReplicationControllerConfiguration.class, hint);
            } catch (ComponentLookupException e) {
                throw new ReplicationException(
                    "Failed to lookup the document replication configuation for the configured hint [" + hint + "]", e);
            }
        }

        return this.controllerConfiguration;
    }

    @Override
    public Optional<Map<String, DocumentReplicationController>> getControllers() throws ReplicationException
    {
        return getDocumentReplicationControllerConfiguration().getControllers();
    }

    @Override
    public DocumentReplicationController resolveDocumentReplicationController(EntityReference entityReference)
        throws ReplicationException
    {
        return getDocumentReplicationControllerConfiguration().resolveDocumentReplicationController(entityReference);
    }

    @Override
    public DocumentReplicationController resolveDocumentReplicationController(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        return getDocumentReplicationControllerConfiguration().resolveDocumentReplicationController(message);
    }

    @Override
    public DocumentReplicationController resolveDocumentDeleteReplicationController(XWikiDocument document)
        throws ReplicationException
    {
        return getDocumentReplicationControllerConfiguration().resolveDocumentDeleteReplicationController(document);
    }
}
