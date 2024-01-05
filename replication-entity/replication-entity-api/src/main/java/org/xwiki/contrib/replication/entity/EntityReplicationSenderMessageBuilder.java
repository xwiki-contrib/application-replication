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
package org.xwiki.contrib.replication.entity;

import java.util.Collection;
import java.util.Map;

import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.model.reference.EntityReference;

/**
 * Helper to asynchronously build a document related message to send.
 * 
 * @version $Id$
 * @since 2.0.0
 */
public interface EntityReplicationSenderMessageBuilder
{
    /**
     * @param id the unique identifier to use for the produced message
     * @return this builder
     */
    EntityReplicationSenderMessageBuilder id(String id);

    /**
     * @param source the instance from which the message is originally coming
     * @return this builder
     */
    EntityReplicationSenderMessageBuilder source(String source);

    /**
     * @param receivers the instances which are supposed to handler the message
     * @return this builder
     */
    EntityReplicationSenderMessageBuilder receivers(Collection<String> receivers);

    /**
     * @param receivers the instances which are supposed to handler the message
     * @return this builder
     */
    EntityReplicationSenderMessageBuilder receivers(String... receivers);

    /**
     * @param customMetadata custom metadata to associate with the message
     * @return this builder
     */
    EntityReplicationSenderMessageBuilder customMetadata(Map<String, Collection<String>> customMetadata);

    /**
     * @param message the message to copy
     * @return this builder
     */
    EntityReplicationSenderMessageBuilder message(ReplicationMessage message);

    /**
     * @param minimumLevel the minimum level required from an instance replication configuration to receive the entity,
     *            null means the message is sent to all instances
     * @return this builder
     */
    EntityReplicationSenderMessageBuilder minimumLevel(DocumentReplicationLevel minimumLevel);

    /**
     * @return the unique identifier to use for the produced message
     */
    String getId();

    /**
     * @return the instance from which the message is originally coming
     */
    String getSource();

    /**
     * @return the reference of the entity associated with the message
     */
    EntityReference getEntityReference();

    /**
     * @return the instances which are supposed to handler the message
     */
    Collection<String> getReceivers();

    /**
     * @return custom metadata to associate with the message
     */
    Map<String, Collection<String>> getCustomMetadata();

    /**
     * @return the minimum level required from an instance configuration to receive the entity
     */
    DocumentReplicationLevel getMinimumLevel();

    /**
     * @param level the level for which to produce the message
     * @param readonly indicate if the document update is readonly
     * @param metadata custom metadata to add to the message
     * @return the message to send
     * @throws ReplicationException when failing to produce the message to send
     */
    ReplicationSenderMessage build(DocumentReplicationLevel level, Boolean readonly,
        Map<String, Collection<String>> metadata) throws ReplicationException;
}
