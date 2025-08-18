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
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * @version $Id$
 */
public class RelayedReplicationSenderMessage implements ReplicationSenderMessage
{
    private final ReplicationReceiverMessage source;

    private final Map<String, Collection<String>> metadata;

    /**
     * @param source the message to relay
     */
    public RelayedReplicationSenderMessage(ReplicationReceiverMessage source)
    {
        this(source, null);
    }

    /**
     * @param source the message to relay
     * @param custom a custom metadata map or null to relay the source metadata
     */
    public RelayedReplicationSenderMessage(ReplicationReceiverMessage source, Map<String, Collection<String>> custom)
    {
        this.source = source;
        this.metadata = custom;
    }

    @Override
    public String getId()
    {
        return this.source.getId();
    }

    @Override
    public Date getDate()
    {
        return this.source.getDate();
    }

    @Override
    public String getSource()
    {
        return this.source.getSource();
    }

    @Override
    public Collection<String> getReceivers()
    {
        return this.source.getReceivers();
    }

    @Override
    public String getType()
    {
        return this.source.getType();
    }

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        return this.metadata != null ? this.metadata : this.source.getCustomMetadata();
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        try (InputStream input = this.source.open()) {
            IOUtils.copy(input, stream);
        }
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.append("id", getId());
        builder.append("type", getType());
        builder.append("source", getSource());
        builder.append("date", getDate());
        builder.append("receivers", getReceivers());
        builder.append("metadata", getCustomMetadata());

        return builder.build();
    }
}
