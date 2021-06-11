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
package org.xwiki.contrib.replication.internal.instance;

import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;

/**
 * @version $Id$
 */
@Component(roles = ReplicationInstanceStore.class)
@Singleton
public class ReplicationInstanceStore
{
    public List<ReplicationInstance> loadInstances() throws ReplicationException
    {

    }

    public void saveInstance(ReplicationInstance instance) throws ReplicationException
    {

    }

    public void deleteInstance(ReplicationInstance instance) throws ReplicationException
    {

    }

    public List<ReplicationInstance> loadRequestingInstances() throws ReplicationException
    {

    }

    public void saveRequestingInstance(ReplicationInstance instance) throws ReplicationException
    {

    }

    public void deleteRequestingInstance(ReplicationInstance instance) throws ReplicationException
    {

    }

    public List<String> loadRequestedInstances() throws ReplicationException
    {

    }

    public void saveRequestedInstance(String instanceURL) throws ReplicationException
    {

    }

    public void deleteRequestedInstance(String instanceURL) throws ReplicationException
    {

    }
}
