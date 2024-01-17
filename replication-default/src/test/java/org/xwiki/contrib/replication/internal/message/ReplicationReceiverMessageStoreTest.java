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
package org.xwiki.contrib.replication.internal.message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.replication.DefaultReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.ReplicationFileStore;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.filter.input.DefaultByteArrayInputSource;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
@ComponentList({TestEnvironment.class, ReplicationFileStore.class})
class ReplicationReceiverMessageStoreTest
{
    @InjectMockComponents
    private ReplicationReceiverMessageStore store;

    @MockComponent
    private ReplicationInstanceManager instances;

    @Test
    void storeAndLoad() throws ReplicationException, IOException
    {
        DefaultReplicationReceiverMessage.Builder message = new DefaultReplicationReceiverMessage.Builder();

        message.id("id");
        Date date = new Date();
        message.date(date);
        DefaultReplicationInstance instance = new DefaultReplicationInstance("name", "uri", Status.REGISTERED, null,
            Map.of("key1", "value1", "key2", "value2"));
        when(this.instances.getInstanceByURI(instance.getURI())).thenReturn(instance);
        message.instance(instance);
        message.type("type");
        message.source("source");
        message.receivers(List.of("receiver1", "receiver2"));
        Map<String, Collection<String>> customMetadata =
            Map.of("KEY1", List.of("value11", "value12"), "KEY2", List.of("value21", "value22"));
        message.customMetadata(customMetadata);
        message.data(new DefaultByteArrayInputSource("data".getBytes()));

        ReplicationReceiverMessage storedMessage = this.store.store(message.build());

        assertEquals("id", storedMessage.getId());
        assertEquals(date, storedMessage.getDate());
        assertSame(instance, storedMessage.getInstance());
        assertEquals("type", storedMessage.getType());
        assertEquals("source", storedMessage.getSource());
        assertEquals(List.of("receiver1", "receiver2"), List.copyOf(storedMessage.getReceivers()));
        assertEquals(2, storedMessage.getCustomMetadata().size());
        assertEquals(customMetadata.get("KEY1"), List.copyOf(storedMessage.getCustomMetadata().get("KEY1")));
        assertEquals(customMetadata.get("KEY2"), List.copyOf(storedMessage.getCustomMetadata().get("KEY2")));
        assertEquals("data", IOUtils.toString(storedMessage.open(), StandardCharsets.UTF_8));
    }
}
