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

import java.util.Map;

import org.xwiki.crypto.pkix.params.CertifiedPublicKey;

/**
 * @version $Id$
 */
public interface ReplicationInstance
{
    /**
     * The status of the instance.
     * 
     * @version $Id$
     */
    enum Status
    {
        /**
         * This instance is registered on both ends.
         */
        REGISTERED,

        /**
         * The instance requested a link but we did not answered yet.
         */
        REQUESTING,

        /**
         * The instance was requested but did not confirm yet.
         */
        REQUESTED,

        /**
         * The instance was noticed as a relayed instance in the past.
         */
        RELAYED
    }

    /**
     * @return the display name of the instance
     */
    String getName();

    /**
     * @return the base URI of the instance (generally of the form https://www.xwiki.org/xwiki/)
     */
    String getURI();

    /**
     * @return the status of this instance
     */
    Status getStatus();

    /**
     * @return the key to use to verify messages sent by this instance
     */
    CertifiedPublicKey getReceiveKey();

    /**
     * @return the properties (all the keys in that map are lower cased)
     */
    Map<String, Object> getProperties();
}
