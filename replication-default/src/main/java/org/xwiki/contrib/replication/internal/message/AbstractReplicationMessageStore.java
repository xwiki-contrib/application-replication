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
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.inject.Inject;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.internal.AbstractReplicationMessage;
import org.xwiki.contrib.replication.internal.ReplicationUtils;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.util.Util;

/**
 * @param <M>
 * @version $Id$
 */
public abstract class AbstractReplicationMessageStore<M extends ReplicationMessage>
{
    private static final String DIRECTORY_REPLICATION = "replication";

    private static final String FILE_METADATA = "metadata.properties";

    private static final String FILE_CUSTOM = "custom.properties";

    private static final String FILE_DATA = "data";

    private static final String PROPERTY_ID = "id";

    private static final String PROPERTY_TYPE = "type";

    private static final String PROPERTY_SOURCE = "source";

    private static final String PROPERTY_DATE = "date";

    @Inject
    protected ReplicationInstanceManager instances;

    @Inject
    protected Environment environment;

    @Inject
    protected Logger logger;

    private File home;

    protected abstract M createReplicationMessage(File messageFolder) throws ReplicationException;

    protected void setHome(File home)
    {
        this.home = home;
    }

    protected File getReplicationFolder()
    {
        return new File(this.environment.getPermanentDirectory(), DIRECTORY_REPLICATION);
    }

    protected File getMessageFolder(String id)
    {
        return new File(this.home, String.valueOf(Util.getHash(id)));
    }

    private File getMetadataFile(File messageFolder)
    {
        return new File(messageFolder, FILE_METADATA);
    }

    private File getCustomFile(File messageFolder)
    {
        return new File(messageFolder, FILE_CUSTOM);
    }

    private File getDataFile(File messageFolder)
    {
        return new File(messageFolder, FILE_DATA);
    }

    protected abstract class AbstractFileReplicationMessage extends AbstractReplicationMessage
        implements ReplicationMessage, Comparable<ReplicationMessage>
    {
        protected String id;

        protected Date date;

        protected String type;

        protected String source;

        protected File dataFile;

        protected AbstractFileReplicationMessage(File messageFolder) throws ConfigurationException, ReplicationException
        {
            // Standard metadata

            File metadataFile = getMetadataFile(messageFolder);

            loadMetadata(new Configurations().properties(metadataFile));

            // Custom metadata

            File customFile = getCustomFile(messageFolder);
            PropertiesConfiguration customProperties = new Configurations().properties(customFile);
            for (Iterator<String> it = customProperties.getKeys(); it.hasNext();) {
                String key = it.next();

                Object propertyValue = customProperties.getProperty(key);

                Collection<String> values;
                if (propertyValue instanceof Collection) {
                    values = Collections.unmodifiableCollection((Collection<String>) propertyValue);
                } else {
                    values = Collections.singletonList((String) propertyValue);
                }

                this.modifiableMetadata.put(key, values);
            }

            // Data

            this.dataFile = getDataFile(messageFolder);
        }

        protected void loadMetadata(PropertiesConfiguration metadata) throws ReplicationException
        {
            this.id = (String) metadata.getProperty(PROPERTY_ID);
            this.type = (String) metadata.getProperty(PROPERTY_TYPE);
            this.source = (String) metadata.getProperty(PROPERTY_SOURCE);

            String dateString = (String) metadata.getProperty(PROPERTY_DATE);
            this.date = new Date(Long.parseLong(dateString));

        }

        @Override
        public String getId()
        {
            return this.id;
        }

        @Override
        public Date getDate()
        {
            return this.date;
        }

        @Override
        public String getSource()
        {
            return this.source;
        }

        @Override
        public String getType()
        {
            return this.type;
        }

        @Override
        public int compareTo(ReplicationMessage o)
        {
            return getDate().compareTo(o.getDate());
        }
    }

    /**
     * @return all the replication messages stored on the filesystem stored by date
     */
    public Queue<M> load()
    {
        Queue<M> list = new PriorityQueue<>();

        if (this.home.exists()) {
            for (File file : this.home.listFiles()) {
                if (file.isDirectory()) {
                    try {
                        list.offer(createReplicationMessage(file));
                    } catch (ReplicationException e) {
                        this.logger.error("Failed to load replication message from folder [{}]", file.getAbsolutePath(),
                            e);
                    }
                }
            }
        }

        return list;
    }

    /**
     * @param message the message to store
     * @throws ReplicationException when failing to store the message
     */
    protected File storeMessage(M message) throws ReplicationException
    {
        File messageFolder = getMessageFolder(message.getId());

        if (messageFolder.exists()) {
            throw new ReplicationException("The message with id [" + message.getId() + "] already exist in the store");
        }

        boolean clean = true;

        try {
            // Make sure the folder exist on filesystem
            messageFolder.mkdirs();

            // Data
            try {
                storeData(message, getDataFile(messageFolder));
            } catch (IOException e) {
                throw new ReplicationException(
                    "Failed to write on disk the data of the message with id [" + message.getId() + "]", e);
            }

            // Standard metadata
            try {
                FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class, null, true)
                        .configure(new Parameters().properties().setFile(getMetadataFile(messageFolder)));

                PropertiesConfiguration configuration = builder.getConfiguration();
                setMessageMetadata(message, configuration);

                builder.save();
            } catch (ConfigurationException e) {
                throw new ReplicationException(
                    "Failed to write on disk the standard metadata of the message with id [" + message.getId() + "]",
                    e);
            }

            // Custom metadata
            try {
                FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class, null, true)
                        .configure(new Parameters().properties().setFile(getCustomFile(messageFolder)));

                PropertiesConfiguration configuration = builder.getConfiguration();
                message.getCustomMetadata().forEach(configuration::setProperty);

                builder.save();
            } catch (ConfigurationException e) {
                throw new ReplicationException(
                    "Failed to write on disk the custom metadata of the message with id [" + message.getId() + "]", e);
            }

            // All went well
            clean = false;
        } finally {
            if (clean) {
                delete(message);
            }
        }

        return messageFolder;
    }

    protected void setMessageMetadata(M message, PropertiesConfiguration configuration) throws ReplicationException
    {
        configuration.setProperty(PROPERTY_ID, message.getId());
        configuration.setProperty(PROPERTY_TYPE, message.getType());
        configuration.setProperty(PROPERTY_DATE, ReplicationUtils.toString(message.getDate()));
        configuration.setProperty(PROPERTY_SOURCE, message.getSource());
    }

    protected abstract void storeData(M message, File file) throws IOException;

    /**
     * @param message the message to delete
     * @throws ReplicationException when failing to delete the message
     */
    public void delete(ReplicationMessage message) throws ReplicationException
    {
        File messageFolder = getMessageFolder(message.getId());

        if (messageFolder.exists()) {
            try {
                FileUtils.deleteDirectory(messageFolder);
            } catch (IOException e) {
                throw new ReplicationException(
                    "Failed to delete message with id [" + message.getId() + "] from the filesystem", e);
            }
        }
    }
}
