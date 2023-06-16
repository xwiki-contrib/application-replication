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
package org.xwiki.contrib.replication.entity.internal.probe;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @since 1.12.0
 */
@Component
@Singleton
@Named(DocumentUpdateProbeRequestReplicationMessage.TYPE)
public class DocumentUpdateProbeRequestReplicationReceiver extends AbstractDocumentUpdateProbeReplicationReceiver
{
    @Inject
    private Provider<DocumentUpdateProbeResponseReplicationMessage> messageProvider;

    @Override
    protected void receiveDocument(ReplicationReceiverMessage message, DocumentReference documentReference,
        XWikiContext xcontext) throws ReplicationException
    {
        // Send back a response to the source
        Collection<String> receivers = List.of(message.getSource());
        this.controller.send(m -> {
            DocumentUpdateProbeResponseReplicationMessage sendMessage = this.messageProvider.get();

            sendMessage.initialize(documentReference, receivers, m);

            return sendMessage;
        }, documentReference, DocumentReplicationLevel.ALL, receivers);
    }
}
