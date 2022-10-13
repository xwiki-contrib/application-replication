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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.AbstractReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;

/**
 * @version $Id$
 * @since 1.1
 */
@Component(roles = ReplicationInstanceRecoverMessage.class)
public class ReplicationInstanceRecoverMessage extends AbstractReplicationSenderMessage
{
    /**
     * The message type for these messages.
     */
    public static final String TYPE = "instance_outdated";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the minimum date for which to send back changes.
     */
    public static final String METADATA_DATE_MIN = METADATA_PREFIX + "DATE_MIN";

    /**
     * The name of the metadata containing the maximum date for which to send changes.
     */
    public static final String METADATA_DATE_MAX = METADATA_PREFIX + "DATE_MAX";

    @Override
    public String getType()
    {
        return TYPE;
    }

    /**
     * @param minDate the minimum date for which to send back changes
     * @param maxDate the maximum date for which to send changes
     * @throws ReplicationException when failing to initialize the message
     */
    public void initializeCurrent(Date minDate, Date maxDate) throws ReplicationException
    {
        super.initialize();

        putCustomMetadata(METADATA_DATE_MIN, minDate);
        putCustomMetadata(METADATA_DATE_MAX, maxDate);
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        // No content
    }
}
