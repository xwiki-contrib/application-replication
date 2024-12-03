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
package org.xwiki.contrib.replication.entity.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.replication.entity.EntityReplicationMessage;

/**
 * @version $Id$
 * @since 1.4.0
 */
@Component(roles = EntityReplicationConfiguration.class)
@Singleton
public class EntityReplicationConfiguration
{
    /**
     * Indicate who is allowed to do something.
     * 
     * @version $Id$
     * @since 2.2.0
     */
    public enum Who
    {
        /**
         * Noone.
         */
        NOONE,

        /**
         * Only the owner.
         */
        OWNER,

        /**
         * Everyone.
         */
        EVERYONE
    }

    /**
     * The prefix of replication entity related configurations.
     */
    public static final String PREFIX = "replication.entity.";

    // Limit unrecoverable changes to the owner
    private static final Map<String, Who> DEFAULT_TYPE_MAPPING =
        Map.of(EntityReplicationMessage.TYPE_DOCUMENT_UNREPLICATE, Who.OWNER,
            EntityReplicationMessage.TYPE_DOCUMENT_HISTORYDELETE, Who.OWNER);

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * @return the maximum number of ancestors to send with the update
     */
    public int getDocumentAncestorMaxCount()
    {
        return this.configuration.getProperty(PREFIX + "ancestorMaxCount", 50);
    }

    /**
     * @param type the message type
     * @return who is allowed to do send this type of messages
     * @since 2.2.0
     */
    public Who getMessageTypeAllowed(String type)
    {
        Who defaultWho = DEFAULT_TYPE_MAPPING.get(type);
        if (defaultWho == null) {
            defaultWho = Who.EVERYONE;
        }

        return this.configuration.getProperty(PREFIX + "who." + type, defaultWho);
    }
}
