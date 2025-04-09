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
package org.xwiki.contrib.replication;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xwiki.filter.input.AbstractInputStreamInputSource;

/**
 * Allow exposing a message as input stream.
 * 
 * @version $Id$
 * @since 2.0.0
 */
public class ReplicationReceiverMessageInputSource extends AbstractInputStreamInputSource
{
    private final ReplicationReceiverMessage message;

    /**
     * @param message the message to wrap
     */
    public ReplicationReceiverMessageInputSource(ReplicationReceiverMessage message)
    {
        this.message = message;
    }

    @Override
    protected InputStream openStream() throws IOException
    {
        // Workaround for https://jira.xwiki.org/browse/XCOMMONS-3310
        // TODO: remove when moving to XWiki 16.10.6+ or 17.3.0+
        return new FilterInputStream(this.message.open())
        {
            @Override
            public void close() throws IOException
            {
                super.close();

                // Since the stream was closed, we need the InputStreamInputSourceto know about it
                inputStream = null;
            }
        };
    }
}
