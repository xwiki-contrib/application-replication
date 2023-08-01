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
import org.xwiki.contrib.replication.ReplicationSenderMessage;

/**
 * @param <B> the exact type of builder
 * @version $Id$
 * @since 2.0.0
 */
@FunctionalInterface
public interface EntityReplicationSenderMessageBuilderProducer<B extends EntityReplicationSenderMessageBuilder>
{
    /**
     * @param builder the identifier of the message
     * @param level the level for which to produce the message
     * @param readonly indicate if the document update is readonly
     * @param extraMetadata custom metadata to add to the message
     * @return the message to send
     * @throws ReplicationException when failing to produce the message to send
     */
    ReplicationSenderMessage produce(B builder, DocumentReplicationLevel level, Boolean readonly,
        Map<String, Collection<String>> extraMetadata) throws ReplicationException;
}
