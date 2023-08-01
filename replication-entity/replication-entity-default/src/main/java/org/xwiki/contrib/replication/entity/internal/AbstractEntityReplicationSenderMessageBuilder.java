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
package org.xwiki.contrib.replication.entity.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.EntityReplicationSenderMessageBuilder;
import org.xwiki.model.reference.EntityReference;

/**
 * Helper to asynchronously build a document related message to send.
 * 
 * @version $Id$
 * @since 2.0.0
 */
public abstract class AbstractEntityReplicationSenderMessageBuilder implements EntityReplicationSenderMessageBuilder
{
    protected final EntityReference entityReference;

    protected String id;

    protected Collection<String> receivers;

    protected DocumentReplicationLevel minimumLevel = DocumentReplicationLevel.REFERENCE;

    /**
     * @param entityReference the reference of the entity for which to send a message
     */
    AbstractEntityReplicationSenderMessageBuilder(EntityReference entityReference)
    {
        this.entityReference = entityReference;
    }

    @Override
    public AbstractEntityReplicationSenderMessageBuilder id(String id)
    {
        this.id = id;

        return this;
    }

    @Override
    public AbstractEntityReplicationSenderMessageBuilder receivers(Collection<String> receivers)
    {
        this.receivers = receivers;

        return this;
    }

    @Override
    public AbstractEntityReplicationSenderMessageBuilder receivers(String... receivers)
    {
        this.receivers = List.of(receivers);

        return this;
    }

    @Override
    public AbstractEntityReplicationSenderMessageBuilder minimumLevel(DocumentReplicationLevel minimumLevel)
    {
        this.minimumLevel = minimumLevel;

        return this;
    }

    @Override
    public String getId()
    {
        if (this.id == null) {
            // Default to random UUID
            this.id = UUID.randomUUID().toString();
        }

        return this.id;
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.entityReference;
    }

    @Override
    public Collection<String> getReceivers()
    {
        return this.receivers;
    }

    @Override
    public DocumentReplicationLevel getMinimumLevel()
    {
        return this.minimumLevel;
    }
}
