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

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Various common tool related to replication.
 * 
 * @version $Id$
 */
public final class ReplicationUtils
{
    private ReplicationUtils()
    {
    }

    /**
     * @param value the date as a String
     * @return the date as a {@link Date}
     */
    public static Date toDate(String value)
    {
        return new Date(Long.parseLong(value));
    }

    /**
     * @param date the date as a {@link Date}
     * @return the date as a String
     */
    public static String toString(Date date)
    {
        return String.valueOf(date.getTime());
    }

    /**
     * @param message the message to print
     * @return the String serialization of the replication message
     * @since 2.2.7
     */
    public static String toString(ReplicationMessage message)
    {
        ToStringBuilder builder = new XWikiToStringBuilder(message);

        builder.append("id", message.getId());
        builder.append("type", message.getType());
        builder.append("source", message.getSource());
        builder.append("date", message.getDate());
        builder.append("receivers", message.getReceivers());
        builder.append("metadata", message.getCustomMetadata());

        if (message instanceof ReplicationReceiverMessage) {
            builder.append("instance", ((ReplicationReceiverMessage) message).getInstance());
        }

        return builder.build();
    }
}
