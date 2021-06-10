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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultReplicationInstanceManager implements ReplicationInstanceManager
{
    @Inject
    private ReplicationClient client;

    private final Map<String, ReplicationInstance> instances = new ConcurrentHashMap<>();

    private final Set<String> pendingTargetInstances = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<String, ReplicationInstance> pendingSourceInstances = new ConcurrentHashMap<>();

    @Override
    public Collection<ReplicationInstance> getInstances()
    {
        return Collections.unmodifiableCollection(this.instances.values());
    }

    @Override
    public ReplicationInstance getInstance(String id)
    {
        return this.instances.get(id);
    }

    @Override
    public boolean removeInstance(ReplicationInstance instance)
    {
        // Forget the instance
        ReplicationInstance storedInstance = this.instances.remove(instance.getId());

        if (storedInstance == null) {
            return false;
        }

        // TODO: Notify the target instance that it's not linked anymore

        return true;
    }

    @Override
    public void requestInstance(String instanceURL) throws ReplicationException
    {
        // TODO: send a request to the target instance

        this.pendingTargetInstances.add(instanceURL);
    }

    @Override
    public Collection<String> getRequestedInstances()
    {
        return Collections.unmodifiableCollection(this.pendingTargetInstances);
    }

    @Override
    public boolean cancelRequestedInstance(String url)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Collection<ReplicationInstance> getRequestingInstances()
    {
        return Collections.unmodifiableCollection(this.pendingSourceInstances.values());
    }

    @Override
    public boolean declineRequestingInstance(ReplicationInstance instance)
    {
        // TODO Auto-generated method stub
        return false;
    }
}
