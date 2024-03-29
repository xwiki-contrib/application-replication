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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DocumentReplicationControllerInstanceConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
class DocumentReplicationControllerInstanceConverterTest
{
    @InjectMockComponents
    private DocumentReplicationControllerInstanceConverter converter;

    @MockComponent
    private ReplicationInstanceManager manager;

    private static final ReplicationInstance INSTANCE =
        new DefaultReplicationInstance("name", "uri", Status.REGISTERED, null, null);

    @BeforeEach
    void beforeEach() throws ReplicationException
    {
        when(this.manager.getInstanceByURI("uri")).thenReturn(INSTANCE);
    }

    @Test
    void convertFromString()
    {
        assertEquals(
            new DocumentReplicationControllerInstance(INSTANCE, DocumentReplicationLevel.ALL,
                DocumentReplicationDirection.BOTH),
            this.converter.convert(DocumentReplicationControllerInstanceConverter.class, "all:both:uri"));
        assertEquals(
            new DocumentReplicationControllerInstance(INSTANCE, DocumentReplicationLevel.REFERENCE,
                DocumentReplicationDirection.RECEIVE_ONLY),
            this.converter.convert(DocumentReplicationControllerInstanceConverter.class, "reference:receive_only:uri"));
        assertEquals(
            new DocumentReplicationControllerInstance(null, DocumentReplicationLevel.REFERENCE,
                DocumentReplicationDirection.SEND_ONLY),
            this.converter.convert(DocumentReplicationControllerInstanceConverter.class, "reference:send_only:"));
        assertEquals(new DocumentReplicationControllerInstance(null, DocumentReplicationLevel.REFERENCE, null),
            this.converter.convert(DocumentReplicationControllerInstanceConverter.class, "reference:"));
        assertEquals(new DocumentReplicationControllerInstance(null, null, null),
            this.converter.convert(DocumentReplicationControllerInstanceConverter.class, ""));
        assertEquals(new DocumentReplicationControllerInstance(INSTANCE, null, null),
            this.converter.convert(DocumentReplicationControllerInstanceConverter.class, "uri"));
    }

    @Test
    void convertToString()
    {
        assertEquals("ALL:BOTH:uri",
            this.converter.convert(String.class, new DocumentReplicationControllerInstance(INSTANCE,
                DocumentReplicationLevel.ALL, DocumentReplicationDirection.BOTH)));
        assertEquals("REFERENCE:SEND_ONLY:uri",
            this.converter.convert(String.class, new DocumentReplicationControllerInstance(INSTANCE,
                DocumentReplicationLevel.REFERENCE, DocumentReplicationDirection.SEND_ONLY)));
        assertEquals("REFERENCE:RECEIVE_ONLY:",
            this.converter.convert(String.class, new DocumentReplicationControllerInstance(null,
                DocumentReplicationLevel.REFERENCE, DocumentReplicationDirection.RECEIVE_ONLY)));
    }
}
