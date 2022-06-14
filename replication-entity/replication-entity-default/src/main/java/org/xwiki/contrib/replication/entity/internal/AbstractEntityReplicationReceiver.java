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
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.contrib.replication.AbstractReplicationReceiver;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationMessageReader;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 */
public abstract class AbstractEntityReplicationReceiver extends AbstractReplicationReceiver
{
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected DocumentReplicationMessageReader documentMessageReader;

    @Inject
    protected Logger logger;

    @Override
    public void receive(ReplicationReceiverMessage message) throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        xcontext.setUserReference(getContextUserReference(message));

        receiveEntity(message, this.documentMessageReader.getEntityReference(message), xcontext);
    }

    protected abstract void receiveEntity(ReplicationReceiverMessage message, EntityReference entityReference,
        XWikiContext xcontext) throws ReplicationException;

    protected DocumentReference getContextUserReference(ReplicationReceiverMessage message)
        throws InvalidReplicationMessageException
    {
        return this.documentMessageReader.getContextUser(message);
    }
}
