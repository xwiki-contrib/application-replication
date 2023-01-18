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
package org.xwiki.contrib.replication.internal.sign;

import java.io.Serializable;

import org.xwiki.observation.event.Event;

/**
 * An event sent when a new replication {@link org.xwiki.crypto.pkix.params.CertifiedKeyPair.CertifiedKeyPair} is
 * created.
 * 
 * @version $Id$
 * @since 1.4.0
 */
public class CertifiedKeyPairCreatedEvent implements Event, Serializable
{
    private static final long serialVersionUID = 1L;

    private final String instance;

    /**
     * The default constructor.
     */
    public CertifiedKeyPairCreatedEvent()
    {
        this.instance = null;
    }

    /**
     * @param instance the instance associated to the
     *            {@link org.xwiki.crypto.pkix.params.CertifiedKeyPair.CertifiedKeyPair}
     */
    public CertifiedKeyPairCreatedEvent(String instance)
    {
        this.instance = instance;
    }

    /**
     * @return the instance associated to the {@link org.xwiki.crypto.pkix.params.CertifiedKeyPair.CertifiedKeyPair}.
     */
    public String getInstance()
    {
        return this.instance;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof CertifiedKeyPairCreatedEvent;
    }
}
