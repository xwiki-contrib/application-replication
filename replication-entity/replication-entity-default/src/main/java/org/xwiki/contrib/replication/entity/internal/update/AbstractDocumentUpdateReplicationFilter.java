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
package org.xwiki.contrib.replication.entity.internal.update;

import org.xwiki.contrib.replication.DefaultReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.EntityReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiverMessageFilter;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 * @since 1.13.0
 */
public abstract class AbstractDocumentUpdateReplicationFilter extends AbstractDocumentReplicationReceiverMessageFilter
{
    @Override
    protected ReplicationReceiverMessage filter(ReplicationReceiverMessage message, DocumentReference documentReference,
        DocumentReplicationControllerInstance configuration) throws ReplicationException
    {
        ReplicationReceiverMessage filteredMessage = super.filter(message, documentReference, configuration);

        // If the instance is supposed to send REFERENCE messages only, convert any FULL message coming from it as
        // REFERENCE ones
        if (configuration.getLevel() == DocumentReplicationLevel.REFERENCE) {
            filteredMessage = new DefaultReplicationReceiverMessage.Builder().message(filteredMessage)
                .type(EntityReplicationMessage.TYPE_DOCUMENT_REFERENCE).build();
        }

        return filteredMessage;
    }
}
