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
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.xwiki.filter.input.InputStreamInputSource;

/**
 * @version $Id$
 * @since 2.0.0
 */
public class DefaultReplicationSenderMessage extends AbstractReplicationSenderMessage
{
    protected String type;

    protected InputStreamInputSource data;

    /**
     * Build a new {@link DefaultReplicationSenderMessage} instance.
     * 
     * @version $Id$
     */
    public static class Builder
    {
        private final DefaultReplicationSenderMessage message = new DefaultReplicationSenderMessage();

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
         * @param data custom metadata to associate with the message
         * @return this builder
         */
        public Builder data(InputStreamInputSource data)
        {
            this.message.data = data;

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
         * @param message the message to copy
         * @return this builder
         */
        public Builder message(ReplicationMessage message)
        {
            id(message.getId());
            date(message.getDate());
            type(message.getType());
            source(message.getSource());
            receivers(message.getReceivers());
            customMetadata(message.getCustomMetadata());

            return this;
        }

        /**
         * @return the {@link DefaultReplicationReceiverMessage} instance
         */
        public DefaultReplicationSenderMessage build()
        {
            // It's mandatory for a message to have a type
            if (this.message.type == null) {
                throw new IllegalStateException("It's mandatory for a message to have a type");
            }

            // Make sure that the message has an identifier
            this.message.initialize();

            return this.message;
        }
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
    public void write(OutputStream stream) throws IOException
    {
        if (this.data != null) {
            try (InputStreamInputSource inputSource = this.data) {
                IOUtils.copy(inputSource.getInputStream(), stream);
            }
        }
    }
}
