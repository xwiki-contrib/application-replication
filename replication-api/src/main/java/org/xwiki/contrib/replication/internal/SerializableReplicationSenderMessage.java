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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.xwiki.contrib.replication.ReplicationSenderMessage;

/**
 * A {@link Serializable} version of {@link ReplicationSenderMessage}.
 * 
 * @version $Id$
 * @since 2.3.0
 */
public class SerializableReplicationSenderMessage extends SerializableReplicationMessage
    implements ReplicationSenderMessage
{
    private static final long serialVersionUID = 1L;

    private final byte[] data;

    /**
     * @param replicationMessage the message to copy
     * @throws IOException when failing to read the message content
     */
    public SerializableReplicationSenderMessage(ReplicationSenderMessage replicationMessage) throws IOException
    {
        super(replicationMessage);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        replicationMessage.write(stream);
        this.data = stream.toByteArray();
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        IOUtils.write(this.data, stream);
    }
}
