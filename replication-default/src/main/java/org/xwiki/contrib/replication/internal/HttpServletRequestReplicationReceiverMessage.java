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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * @version $Id$
 */
public class HttpServletRequestReplicationReceiverMessage implements ReplicationReceiverMessage
{
    private final HttpServletRequest request;

    private ReplicationInstance source;

    /**
     * @param request the request to read
     */
    public HttpServletRequestReplicationReceiverMessage(HttpServletRequest request)
    {
        this.request = request;
    }

    @Override
    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ReplicationInstance getSource()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream open() throws IOException
    {
        return this.request.getInputStream();
    }
}
