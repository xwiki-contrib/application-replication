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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.contrib.replication.ReplicationMessage;

/**
 * A {@link Serializable} version of {@link ReplicationMessage}.
 * 
 * @version $Id$
 * @since 2.3.0
 */
public class SerializableReplicationMessage implements ReplicationMessage, Serializable
{
    private static final long serialVersionUID = 1L;

    private final String id;

    private final Date date;

    private final String type;

    private final String source;

    private final Collection<String> receivers;

    private final Map<String, Collection<String>> customMetadata;

    /**
     * @param replicationMessage the replication message to copy
     */
    public SerializableReplicationMessage(ReplicationMessage replicationMessage)
    {
        this.id = replicationMessage.getId();
        this.date = replicationMessage.getDate();
        this.type = replicationMessage.getType();
        this.source = replicationMessage.getSource();
        this.receivers = new ArrayList<>(replicationMessage.getReceivers());

        this.customMetadata = new HashMap<>();
        replicationMessage.getCustomMetadata().entrySet()
            .forEach(e -> this.customMetadata.put(e.getKey(), new ArrayList<>(e.getValue())));
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
    public Collection<String> getReceivers()
    {
        return this.receivers;
    }

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        return this.customMetadata;
    }
}
