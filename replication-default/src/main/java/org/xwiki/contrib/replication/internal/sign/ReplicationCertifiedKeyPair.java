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

import java.util.Date;

import org.xwiki.crypto.pkix.params.CertifiedKeyPair;

/**
 * @version $Id$
 * @since 2.1.0
 */
public class ReplicationCertifiedKeyPair
{
    private final CertifiedKeyPair key;

    private final Date date;

    /**
     * @param key they key
     */
    public ReplicationCertifiedKeyPair(CertifiedKeyPair key)
    {
        this.key = key;
        this.date = new Date();
    }

    /**
     * @param key they key
     * @param date the date
     */
    public ReplicationCertifiedKeyPair(CertifiedKeyPair key, Date date)
    {
        this.key = key;
        this.date = date;
    }

    /**
     * @param keyPair1 the first key pair
     * @param keyPair2 the second key pair
     * @return true if both key pair have the same public key
     */
    public static boolean samePublicKey(ReplicationCertifiedKeyPair keyPair1, ReplicationCertifiedKeyPair keyPair2)
    {
        if (keyPair1 != keyPair2) {
            if (keyPair1 == null || keyPair2 == null) {
                return false;
            }

            return keyPair1.getKey().getPublicKey().equals(keyPair2.getKey().getPublicKey());
        }

        return true;
    }

    /**
     * @return the key
     */
    public CertifiedKeyPair getKey()
    {
        return this.key;
    }

    /**
     * @return the date
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * @param otherKeyPair the other key pair
     * @return true if this key pair was created before the passed one
     */
    public boolean before(ReplicationCertifiedKeyPair otherKeyPair)
    {
        return this.date == null || this.date.before(otherKeyPair.getDate());
    }

    /**
     * @param otherKeyPair the other key pair
     * @return true if the key pair was created after the passed one
     */
    public boolean after(ReplicationCertifiedKeyPair otherKeyPair)
    {
        return this.date != null && this.date.after(otherKeyPair.getDate());
    }
}
