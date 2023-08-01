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
package org.xwiki.contrib.replication;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.xwiki.filter.input.InputStreamInputSource;

/**
 * @version $Id$
 * @since 2.0.0
 */
public class DefaultReplicationReceiverMessage extends AbstractReplicationMessage implements ReplicationReceiverMessage
{
    protected ReplicationInstance instance;

    protected String id;

    protected Date date;

    protected String type;

    protected String source;

    protected Collection<String> receivers;

    protected InputStreamInputSource data;

    /**
     * Build a new {@link DefaultReplicationReceiverMessage} instance.
     * 
     * @version $Id$
     */
    public static class Builder
    {
        private final DefaultReplicationReceiverMessage message = new DefaultReplicationReceiverMessage();

        /**
         * @param instance the last instance which sent the message
         * @return this builder
         */
        public Builder instance(ReplicationInstance instance)
        {
            this.message.instance = instance;

            return this;
        }

        /**
         * @param id the unique identifier of the message
         * @return this builder
         */
        public Builder id(String id)
        {
            this.message.id = id;

            return this;
        }

        /**
         * @param date the date and time at which this message was produced
         * @return this builder
         */
        public Builder date(Date date)
        {
            this.message.date = date;

            return this;
        }

        /**
         * @param type the instance from which the message is originally coming
         * @return this builder
         */
        public Builder type(String type)
        {
            this.message.type = type;

            return this;
        }

        /**
         * @param source the identifier of the handler associated with the message
         * @return this builder
         */
        public Builder source(String source)
        {
            this.message.source = source;

            return this;
        }

        /**
         * @param receivers the specific instances to send the message to, null for all instances
         * @return this builder
         */
        public Builder receivers(Collection<String> receivers)
        {
            this.message.receivers = receivers;

            return this;
        }

        /**
         * @param metadata
         * @return this builder
         */
        public Builder customMetadata(Map<String, Collection<String>> metadata)
        {
            this.message.modifiableMetadata.putAll(metadata);

            return this;
        }

        /**
         * @param key the name of the metadata
         * @param value the value of the metadata
         * @return this builder
         */
        public Builder customMetadata(String key, Collection<String> value)
        {
            this.message.modifiableMetadata.put(key, value);

            return this;
        }

        /**
         * @param data custom metadata to associate with the message
         * @return this builder
         */
        public Builder data(InputStreamInputSource data)
        {
            this.message.data = data;

            return this;
        }

        /**
         * @param message the message to copy
         * @return this builder
         */
        public Builder message(ReplicationReceiverMessage message)
        {
            id(message.getId());
            date(message.getDate());
            type(message.getType());
            source(message.getSource());
            receivers(message.getReceivers());
            customMetadata(message.getCustomMetadata());
            data(new ReplicationReceiverMessageInputSource(message));

            return this;
        }

        /**
         * @return the {@link DefaultReplicationReceiverMessage} instance
         */
        public DefaultReplicationReceiverMessage build()
        {
            return this.message;
        }
    }

    @Override
    public ReplicationInstance getInstance()
    {
        return this.instance;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getType()
    {
        return this.type;
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
    public Collection<String> getReceivers()
    {
        return this.receivers;
    }

    @Override
    public InputStream open() throws IOException
    {
        return this.data.getInputStream();
    }
}
