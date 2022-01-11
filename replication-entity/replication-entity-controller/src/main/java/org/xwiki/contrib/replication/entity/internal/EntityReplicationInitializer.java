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

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.util.Util;

/**
 * Register the entity replication mapping.
 *
 * @version $Id: f9ed06c92322e4b3ba6505fc7b87ea7a142d246d $
 */
@Component
@Named("EntityReplicationInitializer")
@Singleton
public class EntityReplicationInitializer extends AbstractEventListener implements Initializable
{
    private static final Version VERSION_131003 = new DefaultVersion("13.10.3");

    @Inject
    @Named("readonly")
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    // FIXME: don't use private component
    private HibernateStore store;

    @Inject
    private CoreExtensionRepository coreExtensions;

    @Inject
    private Logger logger;

    /**
     * Setup the listener.
     */
    public EntityReplicationInitializer()
    {
        super("EntityReplicationInitializer", new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Inject the configuration before Hibernate initialization
        configure();
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Check if XWiki is currently initializing (in which case the configuration injection will take place when
        // receiving ApplicationStartedEvent)
        if (this.xcontextProvider.get() == null) {
            return;
        }

        // Check if the Hibernate configuration already been injected
        if (this.store.getConfigurationMetadata()
            .getEntityBinding(HibernateEntityReplicationInstance.class.getName()) == null) {
            // Inject the configuration
            configure();
        }

        // TODO: remove when upgrading to 13.10.3 or 14.0
        if (this.coreExtensions.getCoreExtension("org.xwiki.platform:xwiki-platform-oldcore").getId().getVersion()
            .compareTo(VERSION_131003) >= 0) {
            // Force reload Hibernate configuration
            // Even if the configuration did not changed the registered class did so it needs to be reloaded
            try {
                this.store.build();
            } catch (Exception e) {
                this.logger.error("Failed to reload the Hibernate configuration", e);
            }
        }
    }

    private void configure()
    {
        try (InputStream stream = getMappingFile("replication/entityreplication.hbm.xml")) {
            this.store.getConfiguration().addInputStream(stream);
        } catch (IOException e) {
            this.logger.warn("Failed to close the stream: {}", ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private InputStream getMappingFile(String mappingFileName)
    {
        InputStream resource = Util.getResourceAsStream(mappingFileName);

        // It could happen that the resource is not found in the file system, in the Servlet Context or in the current
        // Thread Context Classloader. In this case try to get it from the CL used to load this class since the default
        // mapping file is located in the same JAR that contains this code.
        // This can happen in the case when this JAR is installed as a root extension (and thus in a ClassLoader not
        // visible from the current Thread Context Classloader at the time when the ApplicationStartedEvent event
        // is sent (the thread context CL is set to the current wiki CL later on).
        if (resource == null) {
            resource = getClass().getClassLoader().getResourceAsStream(mappingFileName);
        }

        return resource;
    }
}
