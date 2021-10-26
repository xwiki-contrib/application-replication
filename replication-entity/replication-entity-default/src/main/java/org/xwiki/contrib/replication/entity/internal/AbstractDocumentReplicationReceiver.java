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

import java.util.Locale;

import javax.inject.Inject;

import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 */
public abstract class AbstractDocumentReplicationReceiver extends AbstractEntityReplicationReceiver
{
    @Inject
    protected DocumentReplicationController controller;

    @Override
    protected void receiveEntity(ReplicationReceiverMessage message, EntityReference entityReference,
        XWikiContext xcontext) throws ReplicationException
    {
        DocumentReference documentReference = getDocumentReference(message, entityReference);

        // Check if this instance is allowed to replicate this document
        checkMessageInstance(message, documentReference);

        receiveDocument(message, documentReference, xcontext);
    }

    protected abstract void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException;

    protected DocumentReference getDocumentReference(ReplicationReceiverMessage message, EntityReference reference)
        throws InvalidReplicationMessageException
    {
        Locale locale = getMetadata(message, AbstractEntityReplicationMessage.METADATA_LOCALE, true, Locale.class);

        return new DocumentReference(reference, locale);
    }

    protected void checkMessageInstance(ReplicationReceiverMessage message, DocumentReference documentReference)
        throws ReplicationException
    {
        for (DocumentReplicationControllerInstance instance : this.controller.getDocumentInstances(documentReference)) {
            if (instance.getInstance() == message.getInstance() && instance.isReadonly()) {
                throw new InvalidReplicationMessageException("The instance [" + message.getInstance()
                    + "] is not allowed to send modifications for document [" + documentReference + "]");
            }
        }
    }
}
