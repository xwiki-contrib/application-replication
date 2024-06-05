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

/**
 * An event sent when a new {@link ReplicationCertifiedKeyPair} is created.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the {@link ReplicationCertifiedKeyPair}</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.1.0
 */
public class ReplicationCertifiedKeyPairCreatedEvent extends AbstractReplicationCertifiedKeyPairEvent
{
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor.
     */
    public ReplicationCertifiedKeyPairCreatedEvent()
    {

    }

    /**
     * @param instance the instance associated to the {@link org.xwiki.crypto.pkix.params.CertifiedKeyPair}
     */
    public ReplicationCertifiedKeyPairCreatedEvent(String instance)
    {
        super(instance);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ReplicationCertifiedKeyPairCreatedEvent;
    }
}
