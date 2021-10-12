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
package org.xwiki.contrib.replication.internal.enpoint;

import javax.inject.Inject;

import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.resource.ResourceReferenceHandlerException;

/**
 * @version $Id$
 */
public abstract class AbstractReplicationEndpoint implements ReplicationEndpoint
{
    /**
     * The name of the parameter containing the URI of the instance which sent the request.
     */
    public static final String PARAMETER_URI = "uri";

    /**
     * The name of the parameter containing the key of the instance which sent the request.
     */
    // TODO: add support for public/private key
    public static final String PARAMETER_KEY = "key";

    @Inject
    protected ReplicationInstanceManager instances;

    protected ReplicationInstance validateInstance(String instanceId)
        throws ResourceReferenceHandlerException, ReplicationException
    {
        ReplicationInstance instance = this.instances.getInstance(instanceId);
        
        if (instance == null || instance.getStatus() != Status.REGISTERED) {
            throw new ResourceReferenceHandlerException(
                "The instance with id [" + instanceId + "] is not authorized to send replication data");
        }

        // Validate the key

        return instance;
    }
}
