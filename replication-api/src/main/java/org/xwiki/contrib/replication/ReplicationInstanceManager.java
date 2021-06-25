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

import org.xwiki.component.annotation.Role;

/**
 * @version $Id$
 */
@Role
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
     * @param instance the instance to add
     * @return <tt>true</tt> if an instance was added as a result of this call
     * @throws ReplicationException
     */
    boolean addInstance(ReplicationInstance instance) throws ReplicationException;

    /**
     * @param uri the uri of the instance to remove
     * @return <tt>true</tt> if an instance was removed as a result of this call
     * @throws ReplicationException
     */
    boolean removeInstance(String uri) throws ReplicationException;

    /**
     * @param instanceURL the base URL of the instance
     * @throws ReplicationException when failing to send a request to the target instance
     */
    void requestInstance(String instanceURL) throws ReplicationException;

    /**
     * @return all requested instances which have not validated the link yet
     */
    Collection<ReplicationInstance> getRequestedInstances();

    /**
     * @param uri the base URI of the instance to remove from the list of instance which did not yet accepted the link
     * @return <tt>true</tt> if an instance was removed as a result of this call
     * @throws ReplicationException
     */
    boolean cancelRequestedInstance(String uri) throws ReplicationException;

    /**
     * @param instance the instance to add
     * @return <tt>true</tt> if an instance was confirmed as a result of this call
     * @throws ReplicationException
     */
    boolean confirmRequestedInstance(ReplicationInstance instance) throws ReplicationException;

    /**
     * @return all instance which requested a link with this instance
     */
    Collection<ReplicationInstance> getRequestingInstances();

    /**
     * @param uri the uri of the instance to accept
     * @return <tt>true</tt> if an instance was created/modified as a result of this call
     * @throws ReplicationException
     */
    boolean acceptRequestingInstance(String uri) throws ReplicationException;

    /**
     * @param uri the uri of the instance to decline
     * @return <tt>true</tt> if an instance was removed as a result of this call
     * @throws ReplicationException
     */
    boolean declineRequestingInstance(String uri) throws ReplicationException;

    /**
     * @param uri the uri of the instance to remove
     * @return <tt>true</tt> if an instance was removed as a result of this call
     * @throws ReplicationException
     */
    boolean removeRequestingInstance(String uri) throws ReplicationException;

    /**
     * Reload all the instances from the store.
     * 
     * @throws ReplicationException when failing to reload the instances from the store
     */
    void reload() throws ReplicationException;
}
