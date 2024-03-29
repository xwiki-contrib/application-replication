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
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerConfiguration;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * @version $Id$
 */
@Component
@Named(DocumentReplicationControllerConfigurationInvalidator.NAME)
@Singleton
public class DocumentReplicationControllerConfigurationInvalidator extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "DocumentReplicationControllerConfigurationInvalidator";

    @Inject
    @Named("standard")
    private Provider<DocumentReplicationControllerConfiguration> configurationProvider;

    /**
     * Setup the listener.
     */
    public DocumentReplicationControllerConfigurationInvalidator()
    {
        super(NAME, BaseObjectReference.anyEvents(ReplicationEntityConfigurationClassInitializer.CLASS_FULLNAME));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentReplicationControllerConfiguration controllerConfiguration = this.configurationProvider.get();

        if (controllerConfiguration instanceof StandardDocumentReplicationControllerConfiguration) {
            ((StandardDocumentReplicationControllerConfiguration) controllerConfiguration)
                .invalidate(((XWikiDocument) source).getDocumentReference().getParent());
        }
    }
}
