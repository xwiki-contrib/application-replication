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
package org.xwiki.contrib.replication.log;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.xwiki.eventstream.query.SimpleEventQuery;

/**
 * @version $Id$
 */
public class ReplicationMessageEventQuery extends SimpleEventQuery
{
    /**
     * The application indicated in the event for all replication messages.
     */
    public static final String VALUE_APPLICATION = "replication";

    /**
     * The status of a send message (stored, sent).
     */
    public static final String KEY_STATUS = "status";

    /**
     * Indicate that a message was stored and is waiting to be sent to the target instances.
     */
    public static final String VALUE_STATUS_STORED = "stored";

    /**
     * Indicate that the message was successfully sent to an instance.
     */
    public static final String VALUE_STATUS_SENT = "sent";

    /**
     * Indicate that the message was received from another instance and is waiting to be handled.
     */
    public static final String VALUE_STATUS_RECEIVED = "received";

    /**
     * Indicate that the received message was successfully handled.
     */
    public static final String VALUE_STATUS_HANDLED = "handled";

    /**
     * The instance to which the message was sent.
     */
    public static final String KEY_TARGET = "target";

    /**
     * The instances to which the message is supposed to be sent.
     */
    public static final String KEY_TARGETS = "targets";

    /**
     * The id of the message.
     */
    public static final String KEY_ID = "id";

    /**
     * The date of the message.
     */
    public static final String KEY_DATE = "date";

    /**
     * The source instance from which the message is coming from.
     */
    public static final String KEY_SOURCE = "source";

    /**
     * The type of the message.
     */
    public static final String KEY_TYPE = "type";

    /**
     * The prefix to use for custom message metadata.
     */
    public static final String PREFIX_CUSTOM_METADATA = "custom_";

    /**
     * The mapping between the custom replication event properties and the corresponding store types.
     */
    public static final Map<String, Type> CUSTOM_TYPES =
        Map.of(KEY_STATUS, String.class, KEY_TARGET, String.class, KEY_TARGETS, List.class, KEY_ID, String.class,
            KEY_DATE, Date.class, KEY_SOURCE, String.class, KEY_TYPE, String.class);

    /**
     * Helper to set the right type depending on the property name.
     * 
     * @param key the name of the replication event custom property
     * @return this {@link ReplicationMessageEventQuery}
     */
    public ReplicationMessageEventQuery custom(String key)
    {
        return (ReplicationMessageEventQuery) super.custom(CUSTOM_TYPES.get(key));
    }
}
