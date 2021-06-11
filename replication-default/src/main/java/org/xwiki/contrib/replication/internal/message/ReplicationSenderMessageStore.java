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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSenderMessage;

/**
 * @version $Id$
 */
@Component(roles = ReplicationSenderMessageStore.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ReplicationSenderMessageStore extends AbstractReplicationMessageStore<ReplicationSenderMessage>
    implements Initializable
{
    private static final String FILE_TARGETS = "targets.txt";

    /**
     * The message to send and the instance to send it to stored on the filesystem.
     * 
     * @version $Id$
     */
    public final class FileReplicationSenderMessage extends AbstractFileReplicationMessage
        implements ReplicationSenderMessage
    {
        private final Collection<ReplicationInstance> targets;

        private FileReplicationSenderMessage(String id) throws ConfigurationException, IOException
        {
            super(id);

            this.targets = Collections.unmodifiableCollection(loadTargets(getId()));
        }

        /**
         * @return the targets the instances where to send the message
         */
        public Collection<ReplicationInstance> getTargets()
        {
            return Collections.unmodifiableCollection(this.targets);
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
        setHome(new File(getReplicationFolder(), "sender"));
    }

    @Override
    protected FileReplicationSenderMessage createReplicationMessage(String id) throws ReplicationException
    {
        try {
            return new FileReplicationSenderMessage(id);
        } catch (Exception e) {
            throw new ReplicationException(
                "Failed to create a file based ReplicationReceiverMessage instance for the message with id [" + id
                    + "]",
                e);
        }
    }

    private File getTargetsFile(String id)
    {
        File messageFolder = getMessageFolder(id);

        return getTargetsFile(messageFolder);
    }

    private File getTargetsFile(File dataFolder)
    {
        return new File(dataFolder, FILE_TARGETS);
    }

    private Set<ReplicationInstance> loadTargets(String id) throws IOException
    {
        File targetsFile = getTargetsFile(id);

        return loadTargets(targetsFile);
    }

    private Set<ReplicationInstance> loadTargets(File targetsFile) throws IOException
    {
        Set<ReplicationInstance> targets = new HashSet<>();

        try (FileInputStream stream = new FileInputStream(targetsFile)) {
            for (LineIterator it = IOUtils.lineIterator(stream, StandardCharsets.UTF_8); it.hasNext();) {
                String line = it.next();
                if (StringUtils.isNotBlank(line)) {
                    targets.add(instances.getInstance(line));
                }
            }
        }

        return targets;
    }

    /**
     * @param message the message to send
     * @param targetToRemove the instance for which the message does not need to be sent anymore
     * @throws ReplicationException when failing to update the list of targets
     */
    public void removeTarget(ReplicationSenderMessage message, ReplicationInstance targetToRemove)
        throws ReplicationException
    {
        File targetsFile = getTargetsFile(message.getId());

        // Get existing targets
        Set<ReplicationInstance> targets;
        try {
            targets = loadTargets(targetsFile);
        } catch (IOException e) {
            throw new ReplicationException("Failed to load targets for the message with id [" + message.getId() + "]",
                e);
        }

        // Remove the target from the existing targets
        targets.remove(targetToRemove);

        if (targets.isEmpty()) {
            // If there is no target left to send the message to, get rid of the entire message
            delete(message);
        } else {
            // Save the new list of targets
            try {
                storeTargets(targetsFile, targets);
            } catch (IOException e) {
                throw new ReplicationException(
                    "Failed to store targets for the message with id [" + message.getId() + "]", e);
            }
        }
    }

    private void storeTargets(String id, Collection<ReplicationInstance> targets) throws IOException
    {
        File targetsFile = getTargetsFile(id);

        storeTargets(targetsFile, targets);
    }

    private void storeTargets(File targetsFile, Collection<ReplicationInstance> targets) throws IOException
    {
        try (FileOutputStream stream = new FileOutputStream(targetsFile)) {
            for (ReplicationInstance target : targets) {
                stream.write(target.getId().getBytes(StandardCharsets.UTF_8));
            }
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
     * @param targets the target instances to associate with the message
     * @return the new instance to manipulate the stored message
     * @throws ReplicationException when failing to store the message
     */
    public FileReplicationSenderMessage store(ReplicationSenderMessage message, Collection<ReplicationInstance> targets)
        throws ReplicationException
    {
        storeMessage(message);

        try {
            storeTargets(message.getId(), targets);
        } catch (IOException e) {
            // Clean
            delete(message);

            throw new ReplicationException("Failed to store targets for message with id [" + message.getId() + "]", e);
        }

        return createReplicationMessage(message.getId());
    }
}
