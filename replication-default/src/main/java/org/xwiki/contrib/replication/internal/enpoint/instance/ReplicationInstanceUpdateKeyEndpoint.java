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
package org.xwiki.contrib.replication.internal.enpoint.instance;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.internal.enpoint.AbstractReplicationEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.ReplicationResourceReference;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.contrib.replication.internal.instance.ReplicationInstanceStore;
import org.xwiki.contrib.replication.internal.sign.SignatureManager;

/**
 * @version $Id$
 */
@Component
@Named(ReplicationInstanceUpdateKeyEndpoint.PATH)
@Singleton
public class ReplicationInstanceUpdateKeyEndpoint extends AbstractReplicationEndpoint
{
    /**
     * The path to use to access this endpoint.
     */
    public static final String PATH = "instance/updatekey";

    /**
     * The name of the parameter which contain the new value of the receive key for the source instance.
     */
    public static final String PARAMETER_NEWKEY = "newKey";

    @Inject
    private ReplicationInstanceStore store;

    @Inject
    private SignatureManager signatureManager;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, ReplicationResourceReference reference)
        throws Exception
    {
        // Make sure the instance is allwoed to do this change
        ReplicationInstance instance = validateInstance(reference);

        // Get the new key
        String newKey = reference.getParameterValue(PARAMETER_NEWKEY);

        // Store the new key
        try {
            this.store.updateInstance(new DefaultReplicationInstance(instance.getName(), instance.getURI(),
                instance.getStatus(), this.signatureManager.unserializeKey(newKey), instance.getProperties()));
        } catch (ReplicationException e) {
            throw new ReplicationException("Failed to update the replication instance [" + instance + "]", e);
        }
    }
}
