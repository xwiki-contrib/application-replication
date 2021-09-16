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

import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance.Level;

/**
 * @version $Id$
 */
public class HibernateEntityReplicationInstance
{
    private long entity;

    private String instance;

    private Level level;

    /**
     * Default constructor.
     */
    public HibernateEntityReplicationInstance()
    {

    }

    /**
     * @param entity the identifier of the entity
     * @param instance the configured target instance
     */
    public HibernateEntityReplicationInstance(long entity, DocumentReplicationControllerInstance instance)
    {
        this.entity = entity;
        this.instance = instance.getInstance().getURI();
        this.level = instance.getLevel();
    }

    /**
     * @return the reference hash of the entity associated with this configuration
     */
    public long getEntity()
    {
        return this.entity;
    }

    /**
     * @param entity the reference hash of the entity associated with this configuration
     */
    public void setEntity(long entity)
    {
        this.entity = entity;
    }

    /**
     * @return the instance to replicate the document with
     */
    public String getInstance()
    {
        return this.instance;
    }

    /**
     * @param instance the instance to replicate the document with
     */
    public void setInstance(String instance)
    {
        this.instance = instance;
    }

    /**
     * @return how much of the document should be replicated
     */
    public Level getLevel()
    {
        return this.level;
    }

    /**
     * @param level how much of the document should be replicated
     */
    public void setLevel(Level level)
    {
        this.level = level;
    }
}
