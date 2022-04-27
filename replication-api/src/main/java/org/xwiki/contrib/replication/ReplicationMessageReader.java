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

import java.lang.reflect.Type;

import org.xwiki.component.annotation.Role;

/**
 * @version $Id$
 */
@Role
public interface ReplicationMessageReader
{
    /**
     * @param message the received message
     * @param key the key metadata in the message
     * @param mandatory true of the property is mandatory
     * @return the metadata value
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    String getMetadata(ReplicationMessage message, String key, boolean mandatory)
        throws InvalidReplicationMessageException;

    /**
     * @param <T> the type of the metadata
     * @param message the received message
     * @param key the key metadata in the message
     * @param mandatory true of the property is mandatory
     * @param type the type to convert the metadata to
     * @return the metadata value
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    <T> T getMetadata(ReplicationMessage message, String key, boolean mandatory, Type type)
        throws InvalidReplicationMessageException;

    /**
     * @param <T> the type of the metadata
     * @param message the received message
     * @param key the key metadata in the message
     * @param mandatory true of the property is mandatory
     * @param def the default value to return if none could be found
     * @return the metadata value
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    <T> T getMetadata(ReplicationMessage message, String key, boolean mandatory, T def)
        throws InvalidReplicationMessageException;

    /**
     * @param <T> the type of the metadata
     * @param message the received message
     * @param key the key metadata in the message
     * @param mandatory true of the property is mandatory
     * @param type the type to convert the metadata to
     * @param def the default value to return if none could be found
     * @return the metadata value
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    <T> T getMetadata(ReplicationMessage message, String key, boolean mandatory, Type type, T def)
        throws InvalidReplicationMessageException;

}
