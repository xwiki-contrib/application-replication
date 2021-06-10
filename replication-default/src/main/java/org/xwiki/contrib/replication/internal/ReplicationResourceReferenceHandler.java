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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiver;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;

/**
 * Async renderer resource handler.
 *
 * @version $Id: ba87ff284f14dfb454fc7623fa7f95ad8c54c370 $
 * @since 10.10RC1
 */
@Component
@Named(ReplicationResourceReferenceHandler.HINT)
@Singleton
public class ReplicationResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    /**
     * The role hint to use for job related resource handler.
     */
    public static final String HINT = "replication";

    /**
     * Represents replication Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType(HINT);

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Container container;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private ReplicationReceiverMessageQueue queue;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(TYPE);
    }

    @Override
    public void handle(ResourceReference resourceReference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        ReplicationResourceReference reference = (ReplicationResourceReference) resourceReference;

        // Make sure the source instance is accepted
        validateInstance(reference.getSource());

        // Make sure the data type is supported
        String dataType = reference.getDataType();
        if (this.componentManager.hasComponent(ReplicationReceiver.class, dataType)) {
            throw new ResourceReferenceHandlerException("Unsupported replication data type [" + dataType + "]");
        }

        // Add the data to the queue
        Request request = this.container.getRequest();
        if (request instanceof ServletRequest) {
            ServletRequest servletRequest = (ServletRequest) request;

            try {
                this.queue
                    .add(new HttpServletRequestReplicationReceiverMessage(servletRequest.getHttpServletRequest()));
            } catch (Exception e) {
                throw new ResourceReferenceHandlerException("Could not handle the replication data", e);
            }
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    void validateInstance(String instanceId) throws ResourceReferenceHandlerException
    {
        ReplicationInstance instance = this.instances.getInstance(instanceId);

        if (instance == null) {
            throw new ResourceReferenceHandlerException(
                "The intance with id [" + instanceId + "] is not authorized to send replication data");
        }
    }
}
