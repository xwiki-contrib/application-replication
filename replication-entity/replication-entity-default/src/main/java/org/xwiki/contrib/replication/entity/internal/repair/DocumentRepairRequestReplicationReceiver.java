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
package org.xwiki.contrib.replication.entity.internal.repair;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationSenderMessageBuilder;
import org.xwiki.contrib.replication.entity.EntityReplicationBuilders;
import org.xwiki.contrib.replication.entity.EntityReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiver;
import org.xwiki.contrib.replication.entity.internal.DocumentReplicationUtils;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @since 1.5.0
 */
@Component
@Singleton
@Named(EntityReplicationMessage.TYPE_DOCUMENT_REPAIRREQUEST)
public class DocumentRepairRequestReplicationReceiver extends AbstractDocumentReplicationReceiver
{
    @Inject
    private DocumentReplicationUtils replicationUtils;

    @Inject
    private EntityReplicationBuilders builders;

    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        // Only the owner is allowed to send repair messages
        if (this.replicationUtils.isOwner(documentReference)) {
            DocumentReplicationSenderMessageBuilder builder = this.builders.documentMessageBuilder(documentReference);

            // Check if it should be a conflict message
            if (this.messageReader.getMetadata(message, EntityReplicationMessage.METADATA_DOCUMENT_CONFLICT,
                Boolean.FALSE)) {
                // As a conflict
                builder.conflict(true);
            }

            // Decide where to send the message (everywhere or to the requester only)
            if (this.messageReader.getMetadata(message, EntityReplicationMessage.METADATA_DOCUMENT_REPAIRREQUEST_SOURCE,
                Boolean.FALSE)) {
                builder.receivers(message.getSource());
            }

            // Send the message
            this.controller.send(builder);
        }
    }
}
