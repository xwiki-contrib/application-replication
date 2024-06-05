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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;

/**
 * Deal with the concerns related to the management, storage and use of asymetric keys.
 *
 * @version $Id$
 */
@Component(roles = SignatureManager.class)
@Singleton
public class SignatureManager
{
    @Inject
    private ReplicationCertifiedKeyPairStore store;

    @Inject
    private CryptTools cryptTools;

    /**
     * @param instance the instance for which to sign the content
     * @param content the content to sign
     * @return the signed content
     * @throws ReplicationException when failing to sign the content
     */
    public String sign(ReplicationInstance instance, String content) throws ReplicationException
    {
        try {
            ReplicationCertifiedKeyPair keyPair = this.store.getCertifiedKeyPair(instance.getURI(), false);

            if (keyPair != null) {
                PrivateKeyParameters pk = keyPair.getKey().getPrivateKey();

                return sign(pk, content);
            }
        } catch (Exception e) {
            throw new ReplicationException(String.format("Error while signing [%s] for [%s]", content, instance), e);
        }

        return null;
    }

    /**
     * @param pk the key to use to sign the content
     * @param content the content to sign
     * @return the signed content
     * @throws ReplicationException when failing to sign the content
     */
    public String sign(PrivateKeyParameters pk, String content) throws ReplicationException
    {
        return this.cryptTools.sign(pk, content);
    }

    /**
     * @param instance the instance for which to validate the signature
     * @param content the unsigned content
     * @param signedContent the signed content
     * @return true if signatures are equals
     * @throws ReplicationException when failing to verify the signature
     */
    public boolean verify(ReplicationInstance instance, String content, String signedContent)
        throws ReplicationException
    {
        return this.cryptTools.verify(instance, content, signedContent);
    }

    /**
     * @param instance the instance associated to the public key
     * @param create if true and no key already exist, create a new one
     * @return the public key used to verify the signature of messages sent by this instance
     * @throws ReplicationException when failing to get the public key of the instance
     * @since 2.1.0
     */
    public CertifiedPublicKey getSendKey(ReplicationInstance instance, boolean create) throws ReplicationException
    {
        return getSendKey(instance.getURI(), create);
    }

    /**
     * @param instance the instance associated to the public key
     * @param create if true and no key already exist, create a new one
     * @return the public key used to verify the signature of messages sent by this instance
     * @throws ReplicationException when failing to get the public key of the instance
     * @since 2.1.0
     */
    public CertifiedPublicKey getSendKey(String instance, boolean create) throws ReplicationException
    {
        ReplicationCertifiedKeyPair keyPair = this.store.getCertifiedKeyPair(instance, create);

        return keyPair != null ? keyPair.getKey().getCertificate() : null;
    }

    /**
     * @param key the serialized public key
     * @return the unserialized public key
     * @throws IOException when failing to unserialize the public key
     */
    public CertifiedPublicKey unserializeKey(String key) throws IOException
    {
        return this.cryptTools.unserializePublicKey(key);
    }

    /**
     * @param key the unserialized public key
     * @return the serialized public key
     * @throws IOException when failing to serialize the public key
     */
    public String serializeKey(CertifiedPublicKey key) throws IOException
    {
        return cryptTools.serializePublicKey(key);
    }

    /**
     * @param key the key for which to create a fingerprint
     * @return the fingerprint for the passed key
     * @throws IOException when failing to create a fingerprint
     */
    public String getFingerprint(CertifiedPublicKey key) throws IOException
    {
        return key != null ? DigestUtils.sha1Hex(key.getEncoded()) : null;
    }
}
