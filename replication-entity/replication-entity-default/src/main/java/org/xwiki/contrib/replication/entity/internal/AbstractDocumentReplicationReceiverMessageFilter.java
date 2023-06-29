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

import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
import org.xwiki.contrib.replication.entity.DocumentReplicationMessageReader;
import org.xwiki.contrib.replication.entity.DocumentReplicationReceiverMessageFilter;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.model.reference.DocumentReference;

/**
 * Base class for all standard message filters.
 * 
 * @version $Id$
 * @since 1.13.0
 */
public abstract class AbstractDocumentReplicationReceiverMessageFilter
    implements DocumentReplicationReceiverMessageFilter
{
    @Inject
    protected DocumentReplicationMessageReader documentMessageReader;

    @Inject
    protected DocumentReplicationUtils replicationUtils;

    @Inject
    protected DocumentReplicationController controller;

    @Inject
    protected EntityReplication entityReplication;

    @Inject
    protected ReplicationInstanceManager instances;

    protected boolean ownerOnly;

    protected boolean writeMessage = true;

    @Override
    public ReplicationReceiverMessage filter(ReplicationReceiverMessage message) throws ReplicationException
    {
        return filter(message, this.documentMessageReader.getDocumentReference(message));
    }

    /**
     * @param message the received message
     * @param documentReference the document referenced in the message
     * @return the message to handle/relay
     * @throws InvalidReplicationMessageException when the message format is wrong
     * @throws ReplicationException when failing to filter the message
     * @since 1.13.0
     */
    protected ReplicationReceiverMessage filter(ReplicationReceiverMessage message, DocumentReference documentReference)
        throws ReplicationException
    {
        DocumentReplicationControllerInstance configuration = this.controller.getReceiveConfiguration(message);

        if (configuration != null) {
            filter(message, documentReference, configuration);
        }

        return message;
    }

    protected ReplicationReceiverMessage filter(ReplicationReceiverMessage message, DocumentReference documentReference,
        DocumentReplicationControllerInstance configuration) throws ReplicationException
    {
        // Refuse any modification message if the instance is supposed to be SEND ONLY
        if (this.writeMessage && configuration.getDirection() == DocumentReplicationDirection.SEND_ONLY) {
            throw new InvalidReplicationMessageException("The instance [" + message.getInstance()
                + "] is not allowed to send messages for document [" + documentReference + "]");
        }

        if (this.ownerOnly) {
            // Only the owner is allowed to send this type of messages
            checkOwnerOnlyMessageInstance(message, documentReference);
        }

        return message;
    }

    protected void checkOwnerOnlyMessageInstance(ReplicationReceiverMessage message,
        DocumentReference documentReference) throws ReplicationException
    {
        if (!this.replicationUtils.isOwner(documentReference)) {
            String owner = this.entityReplication.getOwner(documentReference);
            if (owner != null) {
                ReplicationInstance ownerInstance = this.instances.getInstanceByURI(owner);

                // Check if the current instance is the owner
                if (ownerInstance.getStatus() == null) {
                    throw new InvalidReplicationMessageException(
                        "It's forbidden to send messages of type [" + message.getType()
                            + "] messages to the owner instance of document [" + documentReference + "]");
                }

                // Check of the source is the owner
                String sourceInstance = message.getSource();

                if (!owner.equals(sourceInstance)) {
                    throw new InvalidReplicationMessageException("Instance [" + sourceInstance
                        + "] is not allowed to send messages of type [" + message.getType() + "] for the document ["
                        + documentReference + "] as it's owned by instance [" + owner + "]");
                }
            }
        }
    }
}
