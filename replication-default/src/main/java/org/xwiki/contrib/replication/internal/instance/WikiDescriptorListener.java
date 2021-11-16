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
package org.xwiki.contrib.replication.internal.instance;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * @version $Id$
 */
@Component
@Named(WikiDescriptorListener.NAME)
@Singleton
public class WikiDescriptorListener extends AbstractEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "org.xwiki.contrib.replication.internal.WikiDescriptorListener";

    private static final DocumentReference MAINWIKI_DESCRIPTOR =
        new DocumentReference("xwiki", "XWiki", "XWikiServerXwiki");

    @Inject
    private Provider<ReplicationInstanceStore> client;

    /**
     * Default constructor.
     */
    public WikiDescriptorListener()
    {
        super(NAME, new DocumentCreatedEvent(MAINWIKI_DESCRIPTOR), new DocumentUpdatedEvent(MAINWIKI_DESCRIPTOR));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Reset the cache current instance in case the URL changed
        this.client.get().resetCurrentInstance();
    }
}
