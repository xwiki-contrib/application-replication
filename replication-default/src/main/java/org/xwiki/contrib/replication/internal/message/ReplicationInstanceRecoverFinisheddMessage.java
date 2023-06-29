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
package org.xwiki.contrib.replication.internal.message;

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * @version $Id$
 * @since 1.2.0
 */
@Component(roles = ReplicationInstanceRecoverFinisheddMessage.class)
public class ReplicationInstanceRecoverFinisheddMessage extends AbstractReplicationInstanceRecoverMessage
{
    @Override
    public String getType()
    {
        return TYPE_INSTANCE_RECOVER_FINISHED;
    }

    /**
     * @param minDate the minimum date for which to send back changes
     * @param maxDate the maximum date for which to send changes
     * @param recoverMessage the corresponding recover request message
     * @throws ReplicationException when failing to initialize the message
     */
    public void initialize(Date minDate, Date maxDate, ReplicationReceiverMessage recoverMessage)
        throws ReplicationException
    {
        initialize(minDate, maxDate);

        // Indicate the id of the corresponding request message
        putCustomMetadata(METADATA_INSTANCE_RECOVER_FINISHED_REQUEST_ID, recoverMessage.getId());

        // Only send back this message to the requesting instance
        this.receivers = List.of(recoverMessage.getSource());
    }
}
