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
package org.xwiki.contrib.replication.internal.enpoint.message;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.HTTPUtils;
import org.xwiki.contrib.replication.internal.ReplicationUtils;

/**
 * @version $Id$
 */
@Component(roles = HttpServletRequestReplicationReceiverMessage.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class HttpServletRequestReplicationReceiverMessage implements ReplicationReceiverMessage
{
    /**
     * The request parameter containing the message id.
     */
    public static final String PARAMETER_ID = "id";

    /**
     * The request parameter containing the message date.
     */
    public static final String PARAMETER_DATE = "date";

    /**
     * The request parameter containing the message source instance.
     */
    public static final String PARAMETER_SOURCE = "source";

    /**
     * The request parameter containing the instances which should handle the message.
     * 
     * @since 1.1
     */
    public static final String PARAMETER_RECEIVERS = "receivers";

    /**
     * The request parameter containing the message data type.
     */
    public static final String PARAMETER_TYPE = "type";

    /**
     * The prefix in frong of all the HTTP headers containing the custom metadata associated to the message.
     */
    public static final String HEADER_METADATA_PREFIX = "X-XWIKI-R-";

    // TODO: use it to decrypt values
    private ReplicationInstance instance;

    private HttpServletRequest request;

    /**
     * @param date the date to convert to {@link String}
     * @return the {@link String} version of the date in the context of a message request
     */
    public static String fromDate(Date date)
    {
        return date != null ? String.valueOf(date.getTime()) : null;
    }

    /**
     * @param date the String to convert to {@link Date}
     * @return the {@link Date} version of the date in the context of a message request
     */
    public static Date toDate(String date)
    {
        return date != null ? new Date(Long.valueOf(date)) : null;
    }

    /**
     * @param instance the last instance which sent the message
     * @param request the request to read
     */
    public void initialize(ReplicationInstance instance, HttpServletRequest request)
    {
        this.instance = instance;
        this.request = request;
    }

    /**
     * @return the instance the last instance which sent the message
     */
    @Override
    public ReplicationInstance getInstance()
    {
        return this.instance;
    }

    @Override
    public String getId()
    {
        return this.request.getParameter(PARAMETER_ID);
    }

    @Override
    public Date getDate()
    {
        String dateString = this.request.getParameter(PARAMETER_DATE);

        return toDate(dateString);
    }

    @Override
    public String getSource()
    {
        return this.request.getParameter(PARAMETER_SOURCE);
    }

    @Override
    public Collection<String> getReceivers()
    {
        String[] receivers = this.request.getParameterValues(PARAMETER_RECEIVERS);

        return receivers != null ? Arrays.asList(receivers) : null;
    }

    @Override
    public String getType()
    {
        return this.request.getParameter(PARAMETER_TYPE);
    }

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        Map<String, Collection<String>> metadatas = new LinkedHashMap<>();
        for (Enumeration<String> en = this.request.getHeaderNames(); en.hasMoreElements();) {
            String headerName = en.nextElement();

            String upperCaseHeaderName = headerName.toUpperCase();
            if (upperCaseHeaderName.startsWith(HEADER_METADATA_PREFIX)) {
                Collection<String> values = HTTPUtils.toList(this.request.getHeader(headerName));

                metadatas.put(upperCaseHeaderName.substring(HEADER_METADATA_PREFIX.length()), values);
            }
        }

        return metadatas;
    }

    @Override
    public InputStream open() throws IOException
    {
        return this.request.getInputStream();
    }

    @Override
    public String toString()
    {
        return ReplicationUtils.toString(this);
    }
}
