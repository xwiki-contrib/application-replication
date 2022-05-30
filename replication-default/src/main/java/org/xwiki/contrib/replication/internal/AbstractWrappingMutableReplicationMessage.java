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

import java.util.Date;

import org.xwiki.contrib.replication.MutableReplicationMessage;
import org.xwiki.contrib.replication.ReplicationMessage;

/**
 * @param <M> the type of {@link ReplicationMessage}
 * @version $Id$
 */
public abstract class AbstractWrappingMutableReplicationMessage<M extends ReplicationMessage>
    extends AbstractReplicationMessage implements MutableReplicationMessage
{
    protected M message;

    /**
     * @param message the message to wrap
     */
    public void initialize(M message)
    {
        this.message = message;
        this.modifiableMetadata.putAll(message.getCustomMetadata());
    }

    @Override
    public String getId()
    {
        return this.message.getId();
    }

    @Override
    public Date getDate()
    {
        return this.message.getDate();
    }

    @Override
    public String getSource()
    {
        return this.message.getSource();
    }

    @Override
    public String getType()
    {
        return this.message.getType();
    }

    @Override
    public void putCustomMetadata(String key, Object value)
    {
        super.putCustomMetadata(key, value);
    }
}
