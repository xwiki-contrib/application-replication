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
package org.xwiki.contrib.replication.entity.internal.reference;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationReceiverMessageFilter;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 * @since 2.0.0
 */
@Component
@Singleton
@Named(DocumentReferenceReplicationMessage.TYPE_DOCUMENT_REFERENCE)
public class DocumentReferenceReplicationFilter extends AbstractDocumentReplicationReceiverMessageFilter
{
    @Override
    protected ReplicationReceiverMessage filter(ReplicationReceiverMessage message, DocumentReference documentReference,
        DocumentReplicationControllerInstance currentConfiguration) throws ReplicationException
    {
        if (this.replicationUtils.isOwner(documentReference)) {
            // It does not make sense for the owner to receive a REFERENCE message, send back a correction in the hope
            // to fix any inconsistency in the network
            this.controller.sendDocument(documentReference);

            // Reject the message
            throw new InvalidReplicationMessageException(
                "It's forbidden to send REFERENCE messages to the owner instance");
        }

        return message;
    }
}
