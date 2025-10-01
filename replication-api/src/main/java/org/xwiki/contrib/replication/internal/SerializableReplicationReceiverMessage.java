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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * A {@link Serializable} version of {@link SerializableReplicationMessage}.
 * 
 * @version $Id$
 * @since 2.3.0
 */
public class SerializableReplicationReceiverMessage extends SerializableReplicationMessage
    implements ReplicationReceiverMessage
{
    private static final long serialVersionUID = 1L;

    private final String instanceURI;

    private ReplicationInstance instance;

    private final byte[] data;

    /**
     * @param replicationMessage the message to copy
     * @throws IOException when failing to read the message content
     */
    public SerializableReplicationReceiverMessage(ReplicationReceiverMessage replicationMessage) throws IOException
    {
        super(replicationMessage);

        this.instanceURI = replicationMessage.getInstance().getURI();

        try (InputStream stream = replicationMessage.open()) {
            this.data = IOUtils.toByteArray(stream);
        }
    }

    @Override
    public ReplicationInstance getInstance()
    {
        return this.instance;
    }

    /**
     * @param instances the {@link ReplicationInstanceManager} to use to resolve the instance
     * @throws ReplicationException when failing to resolve the instance
     */
    public void resolveInstance(ReplicationInstanceManager instances) throws ReplicationException
    {
        this.instance = instances.getInstanceByURI(this.instanceURI);
    }

    @Override
    public InputStream open() throws IOException
    {
        return new ByteArrayInputStream(this.data);
    }

}
