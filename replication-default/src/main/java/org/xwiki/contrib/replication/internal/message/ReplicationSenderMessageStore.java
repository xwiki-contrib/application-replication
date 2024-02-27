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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.event.ReplicationMessageStoringEvent;
import org.xwiki.contrib.replication.internal.WrappingMutableReplicationSenderMessage;
import org.xwiki.observation.ObservationManager;

/**
 * @version $Id$
 */
@Component(roles = ReplicationSenderMessageStore.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ReplicationSenderMessageStore extends AbstractReplicationMessageStore<ReplicationSenderMessage>
    implements Initializable
{
    @Inject
    private ObservationManager observation;

    @Inject
    private Provider<WrappingMutableReplicationSenderMessage> wrappingMessageProvider;

    private ReplicationInstance instance;

    /**
     * The message to send and the instance to send it to stored on the filesystem.
     * 
     * @version $Id$
     */
    public final class FileReplicationSenderMessage extends AbstractFileReplicationMessage
        implements ReplicationSenderMessage
    {
        private FileReplicationSenderMessage(File messageFolder) throws ConfigurationException, ReplicationException
        {
            super(messageFolder);
        }

        @Override
        public void write(OutputStream stream) throws IOException
        {
            FileUtils.copyFile(this.dataFile, stream);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        setHome(new File(this.fileStore.getReplicationFolder(), "sender"));
    }

    /**
     * @param instance the instance to send messages to
     * @since 2.0.0
     */
    public void initialize(ReplicationInstance instance)
    {
        this.instance = instance;

        setHome(new File(this.home, clean(this.instance.getURI())));
    }

    private String clean(String uri)
    {
        return StringUtils.replaceChars(uri, "/:@", "-_.");
    }

    @Override
    protected FileReplicationSenderMessage createReplicationMessage(File messageFolder) throws ReplicationException
    {
        try {
            return new FileReplicationSenderMessage(messageFolder);
        } catch (Exception e) {
            throw new ReplicationException(
                "Failed to create a file based ReplicationReceiverMessage instance from folder [" + messageFolder + "]",
                e);
        }
    }

    @Override
    protected void storeData(ReplicationSenderMessage message, File dataFile) throws IOException
    {
        try (FileOutputStream stream = new FileOutputStream(dataFile)) {
            message.write(stream);
        }
    }

    /**
     * @param message the message to store
     * @return the new instance to manipulate the stored message
     * @throws ReplicationException when failing to store the message
     */
    public FileReplicationSenderMessage store(ReplicationSenderMessage message) throws ReplicationException
    {
        // Give a change to customize the message to store
        WrappingMutableReplicationSenderMessage customMessage = this.wrappingMessageProvider.get();
        customMessage.initialize(message);
        this.observation.notify(new ReplicationMessageStoringEvent(), customMessage);

        // Store the message
        File messageFolder = storeMessage(customMessage);

        return createReplicationMessage(messageFolder);
    }
}
