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
     * @return the current instance representation
     * @throws ReplicationException when failing to resolve the create the current instance
     */
    ReplicationInstance getCurrentInstance() throws ReplicationException;

    /**
     * @return all the instances
     * @throws ReplicationException when failing to load the instances
     */
    Collection<ReplicationInstance> getInstances() throws ReplicationException;

    /**
     * @param uri the uri of the instance
     * @return the instance
     * @throws ReplicationException when failing to load the instances
     */
    ReplicationInstance getInstanceByURI(String uri) throws ReplicationException;

    /**
     * @param name the instance name
     * @return the instance
     * @throws ReplicationException when failing to load the instances
     */
    ReplicationInstance getInstanceByName(String name) throws ReplicationException;

    /**
     * @param key the name of the custom property
     * @param value the value of the custom property
     * @return the instance
     * @throws ReplicationException when failing to load the instances
     */
    ReplicationInstance getInstanceByProperty(String key, Object value) throws ReplicationException;

    /**
     * @param instance the instance to add
     * @return true if an instance was added as a result of this call
     * @throws ReplicationException
     */
    boolean addInstance(ReplicationInstance instance) throws ReplicationException;

    /**
     * @param uri the uri of the instance to remove
     * @return the instance that was removed or null if none could be found matching the URI
     * @throws ReplicationException
     */
    ReplicationInstance removeInstance(String uri) throws ReplicationException;

    /**
     * @return all instances which been validated on both ends
     * @throws ReplicationException when failing to load the instances
     */
    Collection<ReplicationInstance> getRegisteredInstances() throws ReplicationException;

    /**
     * @param uri the base URI of the instance to remove from the list of registered instances
     * @return true if an instance was removed as a result of this call
     * @throws ReplicationException when failing to remove the instance
     */
    boolean removeRegisteredInstance(String uri) throws ReplicationException;

    /**
     * @param instanceURL the base URL of the instance
     * @throws ReplicationException when failing to send a request to the target instance
     */
    void requestInstance(String instanceURL) throws ReplicationException;

    /**
     * @return all requested instances which have not validated the link yet
     * @throws ReplicationException when failing to load the instances
     */
    Collection<ReplicationInstance> getRequestedInstances() throws ReplicationException;

    /**
     * @param uri the base URI of the instance to remove from the list of instance which did not yet accepted the link
     * @return true if an instance was removed as a result of this call
     * @throws ReplicationException when failing to cancel the instance
     */
    boolean cancelRequestedInstance(String uri) throws ReplicationException;

    /**
     * @param instance the instance to add
     * @return true if an instance was confirmed as a result of this call
     * @throws ReplicationException when failing to confirm the instance
     */
    boolean confirmRequestedInstance(ReplicationInstance instance) throws ReplicationException;

    /**
     * @return all instance which requested a link with this instance
     * @throws ReplicationException when failing to load the instances
     */
    Collection<ReplicationInstance> getRequestingInstances() throws ReplicationException;

    /**
     * @param uri the uri of the instance to accept
     * @return true if an instance was created/modified as a result of this call
     * @throws ReplicationException when failing to accept the instance
     */
    boolean acceptRequestingInstance(String uri) throws ReplicationException;

    /**
     * @param uri the uri of the instance to decline
     * @return true if an instance was removed as a result of this call
     * @throws ReplicationException when failing to remove the instance
     */
    boolean declineRequestingInstance(String uri) throws ReplicationException;

    /**
     * @param uri the uri of the instance to remove
     * @return the instance that was removed or null if none could be found matching the URI
     * @throws ReplicationException when failing to remove the instance
     */
    ReplicationInstance removeRequestingInstance(String uri) throws ReplicationException;

    /**
     * Reload all the instances from the store.
     * 
     * @throws ReplicationException when failing to reload the instances from the store
     */
    void reload() throws ReplicationException;

    /**
     * @param name the custom name of the current instance
     * @param uri the custom uri of the custom instance
     * @throws ReplicationException when failing to update the current instance
     */
    void saveCurrentInstance(String name, String uri) throws ReplicationException;

    /**
     * Create a new key and replace the one current used to send message to the passed instance.
     * 
     * @param uri the uri of the instance for which to reset the send key
     * @throws ReplicationException when failing to reset the instance send key
     */
    void resetSendKey(String uri) throws ReplicationException;
}
