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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationMessageReader;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationReceiverMessageFilter;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.EntityReplicationMessage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Default implementation of {@link ReplicationReceiverMessageFilter}.
 * 
 * @version $Id$
 * @since 1.13.0
 */
@Component
@Singleton
public class DefaultReplicationReceiverMessageFilter implements ReplicationReceiverMessageFilter
{
    @Inject
    private ReplicationMessageReader reader;

    @Inject
    private DocumentReplicationController controller;

    @Override
    public ReplicationReceiverMessage filter(ReplicationReceiverMessage message) throws ReplicationException
    {
        EntityReference reference = this.reader.getMetadata(message, EntityReplicationMessage.METADATA_ENTITY_REFERENCE,
            false, EntityReference.class);

        if (reference != null && reference.getType() == EntityType.DOCUMENT) {
            // It's an document message
            return this.controller.filter(message);
        }

        return message;
    }
}
