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
package org.xwiki.contrib.replication.internal.message.log;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.internal.WikiDeletedListener;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.internal.DefaultReplicationSenderMessage;
import org.xwiki.environment.Environment;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEventFactory;
import org.xwiki.eventstream.internal.DefaultEventStore;
import org.xwiki.eventstream.internal.EventStreamConfiguration;
import org.xwiki.eventstream.store.solr.internal.EventsSolrCoreInitializer;
import org.xwiki.eventstream.store.solr.internal.SolrEventStore;
import org.xwiki.model.internal.reference.converter.EntityReferenceConverter;
import org.xwiki.model.internal.reference.converter.WikiReferenceConverter;
import org.xwiki.observation.ObservationManager;
import org.xwiki.search.solr.test.SolrComponentList;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.internal.model.reference.SpaceReferenceConverter;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validate {@link ReplicationMessageLogStore}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({EventsSolrCoreInitializer.class, WikiDeletedListener.class, WikiReferenceConverter.class,
    SpaceReferenceConverter.class, DocumentReferenceConverter.class, EntityReferenceConverter.class,
    DefaultEventFactory.class, SolrEventStore.class, DefaultEventStore.class,
    XWikiPropertiesMemoryConfigurationSource.class, ContextComponentManagerProvider.class})
@ReferenceComponentList
@SolrComponentList
class ReplicationMessageLogStoreTest
{
    @XWikiTempDir
    private File permanentDirectory;

    private Environment mockEnvironment;

    @InjectMockComponents
    private ReplicationMessageLogStore logStore;

    @MockComponent
    private DocumentAccessBridge bridge;

    @MockComponent
    private WikiDescriptorManager wikis;

    @MockComponent
    private EventStreamConfiguration configuration;

    @MockComponent
    private ObservationManager observation;

    @MockComponent
    private ContextStoreManager contextStore;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        this.mockEnvironment = this.componentManager.registerMockComponent(Environment.class);
        when(this.mockEnvironment.getPermanentDirectory()).thenReturn(this.permanentDirectory);
        FileUtils.deleteDirectory(this.permanentDirectory);
        this.permanentDirectory.mkdirs();

        when(this.configuration.isEventStoreEnabled()).thenReturn(true);
        when(this.configuration.getEventStore()).thenReturn("solr");
    }

    @Test
    void exist() throws EventStreamException, InterruptedException, ExecutionException
    {
        DefaultReplicationSenderMessage message =
            new DefaultReplicationSenderMessage("id", new Date(), "type", "source", Set.of("receiver1", "receiver2"),
                Map.of("key1", List.of("value1"), "key2", List.of("value2")), null);

        assertFalse(this.logStore.exist(message.getId()));

        this.logStore.saveSync(message, null);

        Optional<String> eventId = this.logStore.getEventId(message.getId());

        assertTrue(eventId.isPresent());
        assertNotEquals(message.getId(), eventId.get());

        assertTrue(this.logStore.exist(message.getId()));

        this.logStore.deleteAsync(message.getId()).get();

        assertFalse(this.logStore.exist(message.getId()));
        assertTrue(this.logStore.getEventId(message.getId()).isEmpty());
    }

    @Test
    void loadMessage() throws EventStreamException, InterruptedException
    {
        DefaultReplicationSenderMessage message =
            new DefaultReplicationSenderMessage("id", new Date(), "type", "source", Set.of("receiver1", "receiver2"),
                Map.of("key1", List.of("value1"), "key2", List.of("value2")), null);

        Event event = this.logStore.saveSync(message, null);

        ReplicationSenderMessage loadedMessage = this.logStore.loadMessage(event.getId());

        assertEquals(message.getId(), loadedMessage.getId());
        assertEquals(message.getCustomMetadata(), loadedMessage.getCustomMetadata());
        assertEquals(message.getDate(), loadedMessage.getDate());
        assertEquals(message.getSource(), loadedMessage.getSource());
        assertEquals(message.getType(), loadedMessage.getType());
        assertEquals(Set.copyOf(message.getReceivers()), Set.copyOf(loadedMessage.getReceivers()));

        loadedMessage = this.logStore.loadMessage(event.getId(), Set.of("receiver3", "receiver4"));

        assertEquals(message.getId(), loadedMessage.getId());
        assertEquals(message.getCustomMetadata(), loadedMessage.getCustomMetadata());
        assertEquals(message.getDate(), loadedMessage.getDate());
        assertEquals(message.getSource(), loadedMessage.getSource());
        assertEquals(message.getType(), loadedMessage.getType());
        assertEquals(Set.of("receiver3", "receiver4"), Set.copyOf(loadedMessage.getReceivers()));
    }
}
