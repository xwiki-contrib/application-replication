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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Singleton;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * @version $Id$
 */
@Component(roles = ReplicationReceiverMessageStore.class)
@Singleton
public class ReplicationReceiverMessageStore extends AbstractReplicationMessageStore<ReplicationReceiverMessage>
    implements Initializable
{
    private final class FileReplicationReceiverMessage extends AbstractFileReplicationMessage
        implements ReplicationReceiverMessage
    {
        private FileReplicationReceiverMessage(String id) throws ConfigurationException
        {
            super(id);
        }

        @Override
        public InputStream open() throws IOException
        {
            return new FileInputStream(this.dataFile);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        setHome(new File(getReplicationFolder(), "receiver"));
    }

    /**
     * @param message the message to store
     * @return the new instance to manipulate the stored message
     * @throws ReplicationException when failing to store the message
     */
    public ReplicationReceiverMessage store(ReplicationReceiverMessage message) throws ReplicationException
    {
        store(message);

        return createReplicationMessage(message.getId());
    }

    @Override
    protected void storeData(ReplicationReceiverMessage message, File dataFile) throws IOException
    {
        try (InputStream stream = message.open()) {
            FileUtils.copyInputStreamToFile(stream, dataFile);
        }
    }

    @Override
    protected FileReplicationReceiverMessage createReplicationMessage(String id) throws ReplicationException
    {
        try {
            return new FileReplicationReceiverMessage(id);
        } catch (ConfigurationException e) {
            throw new ReplicationException(
                "Failed to create a file based ReplicationReceiverMessage instance for the message with id [" + id
                    + "]",
                e);
        }
    }
}
