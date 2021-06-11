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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.inject.Inject;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.environment.Environment;

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

    protected abstract M createReplicationMessage(String id) throws ReplicationException;

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
        return new File(this.home, id);
    }

    private File getMetadataFile(String id)
    {
        File messageFolder = getMessageFolder(id);

        return getMetadataFile(messageFolder);
    }

    private File getMetadataFile(File dataFolder)
    {
        return new File(dataFolder, FILE_METADATA);
    }

    private File getCustomFile(String id)
    {
        File messageFolder = getMessageFolder(id);

        return getCustomFile(messageFolder);
    }

    private File getCustomFile(File dataFolder)
    {
        return new File(dataFolder, FILE_CUSTOM);
    }

    private File getDataFile(String id)
    {
        File messageFolder = getMessageFolder(id);

        return getDataFile(messageFolder);
    }

    private File getDataFile(File messageFolder)
    {
        return new File(messageFolder, FILE_DATA);
    }

    protected abstract class AbstractFileReplicationMessage
        implements ReplicationMessage, Comparable<ReplicationMessage>
    {
        protected final String id;

        protected final Date date;

        protected final String type;

        protected final ReplicationInstance source;

        protected Map<String, Collection<String>> custom;

        protected File dataFile;

        protected AbstractFileReplicationMessage(String id) throws ConfigurationException
        {
            this.id = id;

            // Standard metadata

            File metadataFile = getMetadataFile(getId());
            PropertiesConfiguration metadata = new Configurations().properties(metadataFile);

            this.type = (String) metadata.getProperty(PROPERTY_TYPE);

            String sourceId = (String) metadata.getProperty(PROPERTY_SOURCE);
            this.source = instances.getInstance(sourceId);

            String dateString = (String) metadata.getProperty(PROPERTY_DATE);
            this.date = new Date(Long.parseLong(dateString));

            // Custom metadata

            this.custom = new HashMap<>();
            File customFile = getCustomFile(getId());
            PropertiesConfiguration customProperties = new Configurations().properties(customFile);
            for (Iterator<String> it = customProperties.getKeys(id); it.hasNext();) {
                String key = it.next();

                Object propertyValue = customProperties.getProperty(key);

                Collection<String> values;
                if (propertyValue instanceof Collection) {
                    values = Collections.unmodifiableCollection((Collection<String>) propertyValue);
                } else {
                    values = Collections.singletonList((String) propertyValue);
                }

                this.custom.put(key, values);
            }
            this.custom = Collections.unmodifiableMap(this.custom);

            // Data

            this.dataFile = getDataFile(getId());
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
        public ReplicationInstance getSource()
        {
            return this.source;
        }

        @Override
        public String getType()
        {
            return this.type;
        }

        @Override
        public Map<String, Collection<String>> getCustomMetadata()
        {
            return this.custom;
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

        for (File file : this.home.listFiles()) {
            if (file.isDirectory()) {
                String id = file.getName();

                try {
                    list.offer(createReplicationMessage(id));
                } catch (ReplicationException e) {
                    this.logger.error("Failed to load replication message from folder [{}]", file.getAbsolutePath(), e);
                }
            }
        }

        return list;
    }

    /**
     * @param message the message to store
     * @return the new instance to manipulate the stored message
     * @throws ReplicationException when failing to store the message
     */
    protected void storeMessage(M message) throws ReplicationException
    {
        File messageFolder = getMessageFolder(message.getId());

        if (messageFolder.exists()) {
            throw new ReplicationException("The data with id [" + message.getId() + "] already exist in the queue");
        }

        boolean clean = true;

        try {
            // Data
            try {
                storeData(message, getDataFile(messageFolder));
            } catch (IOException e) {
                throw new ReplicationException(
                    "Failed to write on disk the data of the message with id [" + message.getId() + "]", e);
            }

            // Standard metadata
            try {
                FileBasedConfigurationBuilder<PropertiesConfiguration> metadata =
                    new Configurations().propertiesBuilder(getMetadataFile(messageFolder));
                metadata.getConfiguration().setProperty(PROPERTY_TYPE, message.getType());
                metadata.getConfiguration().setProperty(PROPERTY_SOURCE, message.getSource().getId());
                metadata.getConfiguration().setProperty(PROPERTY_DATE, String.valueOf(message.getDate().getTime()));
                metadata.save();
            } catch (ConfigurationException e) {
                throw new ReplicationException(
                    "Failed to write on disk the standard metadata of the message with id [" + message.getId() + "]",
                    e);
            }

            // Custom metadata
            try {
                FileBasedConfigurationBuilder<PropertiesConfiguration> custom =
                    new Configurations().propertiesBuilder(getCustomFile(messageFolder));
                message.getCustomMetadata().forEach(custom.getConfiguration()::setProperty);
                custom.save();
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
