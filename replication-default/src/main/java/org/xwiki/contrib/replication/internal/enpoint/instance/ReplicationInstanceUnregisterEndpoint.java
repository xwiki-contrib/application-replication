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

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.internal.enpoint.AbstractReplicationEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.ReplicationResourceReference;

/**
 * @version $Id$
 */
@Named("instance/unregister")
public class ReplicationInstanceUnregisterEndpoint extends AbstractReplicationEndpoint
{
    private static final String PARAMETER_ID = "id";

    private static final String PARAMETER_NAME = "name";

    private static final String PARAMETER_URI = "uri";

    // TODO: add support for public/private key
    private static final String PARAMETER_KEY = "key";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, ReplicationResourceReference reference)
        throws Exception
    {
        String id = reference.getParameterValue(PARAMETER_ID);
        String name = reference.getParameterValue(PARAMETER_NAME);
        String uri = reference.getParameterValue(PARAMETER_URI);

        ReplicationInstance instance = this.instances.getInstance(id);

        if (instance != null) {
            // TODO: validate key
            this.instances.removeInstance(instance);

            return;
        }

        instance = this.instances.getRequestingInstance(id);

        if (instance != null) {
            // TODO: validate key
            this.instances.removeRequestingInstance(instance);

            return;
        }

        // Unknown instance
        response.sendError(404, "Unknown instance");
    }
}
