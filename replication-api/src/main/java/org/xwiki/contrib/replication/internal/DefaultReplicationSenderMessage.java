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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.xwiki.contrib.replication.AbstractReplicationMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.filter.input.InputStreamInputSource;

/**
 * @version $Id$
 * @since 1.1
 */
public class DefaultReplicationSenderMessage extends AbstractReplicationMessage implements ReplicationSenderMessage
{
    protected final String id;

    protected final Date date;

    protected final String type;

    protected final String source;

    protected final Collection<String> receivers;

    protected final InputStreamInputSource data;

    /**
     * @param id the unique identifier of the message
     * @param date the date and time at which this message was produced
     * @param type the identifier of the handler associated with the message
     * @param source the instance from which the message is originally coming
     * @param receivers the instances which should handle the message
     * @param metadata custom metadata to add to the message
     * @param data the data to send with the message
     */
    public DefaultReplicationSenderMessage(String id, Date date, String type, String source,
        Collection<String> receivers, Map<String, Collection<String>> metadata, InputStreamInputSource data)
    {
        this.id = id;
        this.date = date;
        this.type = type;
        this.source = source;
        this.receivers = receivers;
        this.data = data;

        if (metadata != null) {
            this.modifiableMetadata.putAll(metadata);
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
