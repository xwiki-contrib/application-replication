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

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * @version $Id$
 */
public interface ReplicationMessage
{
    /**
     * The prefix of all instance recovering types.
     * 
     * @since 1.13.0
     */
    String PREFIX_TYPE_INSTANCE_RECOVER = "instance_recover_";

    /**
     * The type of the messages in charge of indicating to other instance that recovery is finished.
     * 
     * @since 1.13.0
     */
    String TYPE_INSTANCE_RECOVER_FINISHED = PREFIX_TYPE_INSTANCE_RECOVER + "finished";

    /**
     * The type of the messages in charge of requesting other instances to send back recovery messages.
     * 
     * @since 1.13.0
     */
    String TYPE_INSTANCE_RECOVER_REQUEST = PREFIX_TYPE_INSTANCE_RECOVER + "requested";

    /**
     * The prefix in front of all entity metadata properties.
     * 
     * @since 1.13.0
     */
    String PREFIX_METADATA_INSTANCE_RECOVER_REQUEST = PREFIX_TYPE_INSTANCE_RECOVER.toUpperCase();

    /**
     * The name of the metadata containing the minimum date for which to send back changes.
     * 
     * @since 1.13.0
     */
    String METADATA_INSTANCE_RECOVER_REQUEST_DATE_MIN = PREFIX_METADATA_INSTANCE_RECOVER_REQUEST + "DATE_MIN";

    /**
     * The name of the metadata containing the maximum date for which to send changes.
     * 
     * @since 1.13.0
     */
    String METADATA_INSTANCE_RECOVER_REQUEST_DATE_MAX = PREFIX_METADATA_INSTANCE_RECOVER_REQUEST + "DATE_MAX";

    /**
     * The prefix in front of all entity metadata properties.
     */
    String PREFIX_METADATA_INSTANCE_RECOVER_FINISHED = TYPE_INSTANCE_RECOVER_FINISHED.toUpperCase() + '_';

    /**
     * The name of the metadata containing the identifier of the corresponding request.
     */
    String METADATA_INSTANCE_RECOVER_FINISHED_REQUEST_ID = PREFIX_METADATA_INSTANCE_RECOVER_FINISHED + "REQUEST_ID";

    /**
     * The type messages in charge of sending instance metadata to other instances.
     * 
     * @since 1.13.0
     */
    String TYPE_INSTANCE_UPDATE = "instance_update";

    /**
     * The prefix in front of all entity metadata properties of instance update messages.
     * 
     * @since 1.13.0
     */
    String PREFIX_METADATA_INSTANCE_UPDATE = TYPE_INSTANCE_UPDATE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the instance name.
     * 
     * @since 1.13.0
     */
    String METADATA_INSTANCE_UPDATE_NAME = PREFIX_METADATA_INSTANCE_UPDATE + "NAME";

    /**
     * The prefix used to generate the name of the custom instance properties.
     * 
     * @since 1.13.0
     */
    String PREFIX_INSTANCE_UPDATE_CUSTOM = PREFIX_METADATA_INSTANCE_UPDATE + "CUSTOM_";

    /**
     * @return the unique identifier of the message
     */
    String getId();

    /**
     * @return the date and time at which this message was produced
     */
    Date getDate();

    /**
     * @return the instance from which the message is originally coming
     */
    String getSource();

    /**
     * @return the identifier of the handler associated with the message
     */
    String getType();

    /**
     * @return the specific instances to send the message to, null for all instances
     * @since 1.1
     */
    default Collection<String> getReceivers()
    {
        return null;
    }

    /**
     * @return custom metadata to associate with the message
     */
    Map<String, Collection<String>> getCustomMetadata();
}
