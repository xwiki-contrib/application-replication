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
package org.xwiki.contrib.replication.internal.message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.AbstractReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;

/**
 * @version $Id$
 */
@Component(roles = ReplicationInstanceUpdateMessage.class)
public class ReplicationInstanceUpdateMessage extends AbstractReplicationSenderMessage
{
    /**
     * The message type for these messages.
     */
    public static final String TYPE = "instance_update";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the instance name.
     */
    public static final String METADATA_NAME = METADATA_PREFIX + "NAME";

    /**
     * The prefix used to generate the name of the custom instance properties.
     * 
     * @since 1.10.0
     */
    public static final String PREFIX_METADATE_CUSTOM = METADATA_PREFIX + "CUSTOM_";

    @Inject
    private ReplicationInstanceManager instances;

    @Override
    public String getType()
    {
        return TYPE;
    }

    /**
     * @throws ReplicationException when failing to initialize the message
     */
    public void initializeCurrent() throws ReplicationException
    {
        super.initialize();

        ReplicationInstance currentInstance = this.instances.getCurrentInstance();

        putCustomMetadata(METADATA_NAME, currentInstance.getName());

        for (Map.Entry<String, Object> entry : currentInstance.getProperties().entrySet()) {
            putCustomMetadata(PREFIX_METADATE_CUSTOM + entry.getKey().toUpperCase(), entry.getValue());
        }
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        // No content
    }
}
