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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.xwiki.contrib.replication.AbstractReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;

/**
 * @version $Id$
 * @since 1.2.0
 */
public abstract class AbstractReplicationInstanceRecoverMessage extends AbstractReplicationSenderMessage
{
    /**
     * @param minDate the minimum date for which to send back changes
     * @param maxDate the maximum date for which to send changes
     * @throws ReplicationException when failing to initialize the message
     */
    public void initialize(Date minDate, Date maxDate) throws ReplicationException
    {
        initialize();

        putCustomMetadata(METADATA_INSTANCE_RECOVER_REQUEST_DATE_MIN, minDate);
        putCustomMetadata(METADATA_INSTANCE_RECOVER_REQUEST_DATE_MAX, maxDate);
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        // No content
    }
}
