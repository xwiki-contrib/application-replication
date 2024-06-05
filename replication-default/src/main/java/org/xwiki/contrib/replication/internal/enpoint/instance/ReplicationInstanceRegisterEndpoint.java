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
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.UnauthorizedReplicationInstanceException;
import org.xwiki.contrib.replication.internal.enpoint.AbstractReplicationEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.ReplicationResourceReference;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.contrib.replication.internal.message.ReplicationInstanceMessageSender;

/**
 * @version $Id$
 */
@Component
@Named(ReplicationInstanceRegisterEndpoint.PATH)
@Singleton
public class ReplicationInstanceRegisterEndpoint extends AbstractReplicationEndpoint
{
    /**
     * The path to use to access this endpoint.
     */
    public static final String PATH = "instance/register";

    /**
     * The name of the parameter which contain the display name of the instance which sent the request.
     */
    public static final String PARAMETER_NAME = "name";

    /**
     * The name of the parameter which contain the public key to use to verify messages sent by the instance.
     */
    public static final String PARAMETER_RECEIVEKEY = "receiveKey";

    /**
     * The name of the parameter which contain the public key to use to verify the instance was requested.
     */
    public static final String PARAMETER_REQUESTKEY = "requestKey";

    @Inject
    private Provider<ReplicationInstanceMessageSender> senderProvider;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, ReplicationResourceReference reference)
        throws Exception
    {
        String name = reference.getParameterValue(PARAMETER_NAME);
        String uri = reference.getParameterValue(PARAMETER_URI);
        String receiveKey = reference.getParameterValue(PARAMETER_RECEIVEKEY);
        String requestKey = reference.getParameterValue(PARAMETER_REQUESTKEY);

        ReplicationInstance instance = this.instances.getInstanceByURI(uri);

        if (instance != null) {
            if (instance.getStatus() == null) {
                response.sendError(400, "Client and target instances have the same URI: " + uri);
            } else if (instance.getStatus() == Status.REQUESTED) {
                // Make sure the instance was requested
                if (!requestKey
                    .equals(this.signatureManager.serializeKey(this.signatureManager.getSendKey(instance, false)))) {
                    throw new UnauthorizedReplicationInstanceException("The instance was not requested");
                }

                // Confirm the registration
                ReplicationInstance newInstance = new DefaultReplicationInstance(name, instance.getURI(),
                    Status.REGISTERED, this.signatureManager.unserializeKey(receiveKey), instance.getProperties());
                this.instances.confirmRequestedInstance(newInstance);

                // The instance is now registered
                response.setStatus(200);

                // Make sure to flush the response before sending updates about the instance as otherwise the update
                // message might be refused (before the instance is not REGISTERED yet)
                response.flushBuffer();

                // Send details about the current instance to the new linked one
                // TODO: send custom properties as part of the ACCESS request along with the name ?
                this.senderProvider.get().updateCurrentInstance(newInstance);
            } else if (instance.getStatus() == Status.REGISTERED) {
                // Already registered
                response.sendError(409, "An instance is already registered with URI: " + uri);
            } else {
                // Already requested
                response.setStatus(202);
            }
        } else {
            // Creating a new requesting instance
            this.instances.addInstance(new DefaultReplicationInstance(name, uri, Status.REQUESTING,
                this.signatureManager.unserializeKey(receiveKey), null));

            response.setStatus(201);
        }
    }
}
