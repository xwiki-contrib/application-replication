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

import java.util.Collection;

/**
 * @version $Id$
 */
public interface ReplicationInstanceManager
{
    /**
     * @return all the registered instances
     */
    Collection<ReplicationInstance> getInstances();

    /**
     * @param id the identifier of the instance
     * @return the instance
     */
    ReplicationInstance getInstance(String id);

    /**
     * @param instance the instance to remove from the registered list
     * @return <tt>true</tt> if an instance was removed as a result of this call
     */
    boolean removeInstance(ReplicationInstance instance);

    /**
     * @param instanceURL the base URL of the instance
     * @throws ReplicationException when failing to send a request to the target instance
     */
    void requestInstance(String instanceURL) throws ReplicationException;

    /**
     * @return all requested instances which have not validated the link yet
     */
    Collection<String> getRequestedInstances();

    /**
     * @param url the base URL of the instance to remove from the list of instance which did not yet accepted the link
     * @return <tt>true</tt> if an instance was removed as a result of this call
     */
    boolean cancelRequestedInstance(String url);

    /**
     * @return all instance which requested a link with this instance
     */
    Collection<ReplicationInstance> getRequestingInstances();

    /**
     * @param instance the instance to remove from the registered list
     * @return <tt>true</tt> if an instance was removed as a result of this call
     */
    boolean declineRequestingInstance(ReplicationInstance instance);
}
