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
import org.xwiki.contrib.replication.UnauthorizedReplicationInstanceException;
import org.xwiki.contrib.replication.internal.sign.SignatureManager;

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
     * The key to verify.
     */
    public static final String PARAMETER_KEY = "key";

    /**
     * The signature to verify with the key.
     */
    public static final String PARAMETER_SIGNEDKEY = "signedKey";

    @Inject
    protected ReplicationInstanceManager instances;

    @Inject
    protected SignatureManager signatureManager;

    protected ReplicationInstance validateInstance(ReplicationResourceReference reference) throws ReplicationException
    {
        String uri = reference.getParameterValue(PARAMETER_URI);
        ReplicationInstance instance = this.instances.getInstanceByURI(uri);

        if (instance == null) {
            throw new UnauthorizedReplicationInstanceException(
                String.format("The instance [%s] is unknown", uri));
        }

        if (instance.getStatus() != Status.REGISTERED) {
            throw new UnauthorizedReplicationInstanceException(
                String.format("The instance [%s] is not registered (status=[%s])", uri, instance.getStatus()));
        }

        validateInstance(instance, reference);

        return instance;
    }

    protected void validateInstance(ReplicationInstance instance, ReplicationResourceReference reference)
        throws ReplicationException
    {
        String key = reference.getParameterValue(PARAMETER_KEY);

        // Validate the key
        if (key == null) {
            throw new UnauthorizedReplicationInstanceException(
                "No key to verify for instance [" + instance.getURI() + "]");
        }

        String signedKey = reference.getParameterValue(PARAMETER_SIGNEDKEY);

        if (!this.signatureManager.verify(instance, key, signedKey)) {
            throw new UnauthorizedReplicationInstanceException(
                "Failed to validate the signature from instance [" + instance.getURI() + "]");
        }
    }
}
