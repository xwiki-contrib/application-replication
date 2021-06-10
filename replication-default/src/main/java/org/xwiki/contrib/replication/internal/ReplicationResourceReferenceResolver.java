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

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;

/**
 * Transform Async renderer URL into a typed Resource Reference. The URL format handled is
 * {@code http://server/context/asyncrenderer/}.
 * 
 * @version $Id: 54dbba541e75c72917c30d7e49f6cdf2553781bc $
 * @since 10.10RC1
 */
@Component
@Named(ReplicationResourceReferenceHandler.HINT)
@Singleton
public class ReplicationResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    /**
     * The name of the parameter containing the source instance id.
     */
    public static final String PARAMETER_SOURCE = "source";

    /**
     * The name of the parameter containing the type of data.
     */
    public static final String PARAMETER_TYPE = "type";

    @Override
    public ResourceReference resolve(ExtendedURL representation, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        String dataType = getParameter(representation, PARAMETER_TYPE);
        String sourceId = getParameter(representation, PARAMETER_SOURCE);

        return new ReplicationResourceReference(resourceType, dataType, sourceId);
    }

    private String getParameter(ExtendedURL representation, String key)
    {
        List<String> parameters = representation.getParameters().get(key);
        if (CollectionUtils.isNotEmpty(parameters)) {
            return parameters.get(0);
        }

        return null;
    }
}
