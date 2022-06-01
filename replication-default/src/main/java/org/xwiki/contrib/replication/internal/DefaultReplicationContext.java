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
package org.xwiki.contrib.replication.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationMessage;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultReplicationContext implements ReplicationContext
{
    private static final String REPLICATION_MESSAGE = "replication.message";

    @Inject
    private Execution execution;

    /**
     * @return true if the execution results from a received replication message, false otherwise
     */
    @Override
    public boolean isReplicationMessage()
    {
        return getReplicationMessage() != null;
    }

    @Override
    public ReplicationMessage getReplicationMessage()
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null) {
            return (ReplicationMessage) context.getProperty(REPLICATION_MESSAGE);
        }

        return null;
    }

    /**
     * @param replicationMessage true if the execution results from a received replication message, false otherwise
     */
    public void setReplicationMessage(ReplicationMessage replicationMessage)
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null && getReplicationMessage() != replicationMessage) {
            if (replicationMessage != null) {
                context.newProperty(REPLICATION_MESSAGE).inherited().initial(replicationMessage).makeFinal().declare();
            } else {
                context.removeProperty(REPLICATION_MESSAGE);
            }
        }
    }
}
