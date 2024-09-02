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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.NotFoundResourceHandlerException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;

/**
 * Replication resource handler.
 *
 * @version $Id: ba87ff284f14dfb454fc7623fa7f95ad8c54c370 $
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
    private Logger logger;

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

        ReplicationEndpoint enpoint;
        try {
            enpoint = this.componentManager.getInstance(ReplicationEndpoint.class, reference.getPath());
        } catch (ComponentLookupException e) {
            throw new NotFoundResourceHandlerException(resourceReference, e);
        }

        Request request = this.container.getRequest();
        if (request instanceof ServletRequest) {
            ServletRequest servletRequest = (ServletRequest) request;
            ServletResponse servletResponse = (ServletResponse) this.container.getResponse();

            try {
                enpoint.handle(servletRequest.getHttpServletRequest(), servletResponse.getHttpServletResponse(),
                    reference);
            } catch (Exception e) {
                this.logger.debug("The Replication request [{}] failed", reference, e);

                throw new ResourceReferenceHandlerException("The Replication request failed", e);
            }
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

}
