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
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

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
    private CertifiedKeyPairStore store;

    @Inject
    @Named("X509")
    private CertificateFactory certificateFactory;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    /**
     * @param instance the instance for which to sign the content
     * @param content the content to sign
     * @return the signed content
     * @throws ReplicationException when failing to sign the content
     */
    public String sign(ReplicationInstance instance, String content) throws ReplicationException
    {
        try {
            PrivateKeyParameters pk = this.store.getCertifiedKeyPair(instance.getURI()).getPrivateKey();

            Signer signer = this.signerFactory.getInstance(true, pk);
            signer.update(content.getBytes(UTF_8));

            return Base64.getEncoder().encodeToString(signer.generate());
        } catch (Exception e) {
            throw new ReplicationException(String.format("Error while signing [%s] for [%s]", content, instance), e);
        }
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
        // If no signature associated accept anything
        if (instance.getPublicKey() == null) {
            return true;
        }

        try {
            Signer signer = this.signerFactory.getInstance(false, instance.getPublicKey().getPublicKeyParameters());
            signer.update(content.getBytes(UTF_8));

            return signer.verify(Base64.getDecoder().decode(signedContent));
        } catch (Exception e) {
            throw new ReplicationException(
                String.format("Error while verifying signature [%s] for [%s]", signedContent, instance), e);
        }
    }

    /**
     * @param instance the instance associated to the public key
     * @return the public key used to verify the signature of messages sent by this instance
     * @throws ReplicationException when failing to get the public key of the instance
     */
    public CertifiedPublicKey getSendPublicKey(ReplicationInstance instance) throws ReplicationException
    {
        return getSendPublicKey(instance.getURI());
    }

    /**
     * @param instance the instance associated to the public key
     * @return the public key used to verify the signature of messages sent by this instance
     * @throws ReplicationException when failing to get the public key of the instance
     */
    public CertifiedPublicKey getSendPublicKey(String instance) throws ReplicationException
    {
        return this.store.getCertifiedKeyPair(instance).getCertificate();
    }

    /**
     * @param publicKey the serialized public key
     * @return the unserialized public key
     * @throws IOException when failing to unserialize the public key
     */
    public CertifiedPublicKey unserializePublicKey(String publicKey) throws IOException
    {
        return StringUtils.isEmpty(publicKey) ? null
            : this.certificateFactory.decode(Base64.getDecoder().decode(publicKey));
    }

    /**
     * @param publicKey the unserialized public key
     * @return the serialized public key
     * @throws IOException when failing to serialize the public key
     */
    public String serializePublicKey(CertifiedPublicKey publicKey) throws IOException
    {
        if (publicKey == null) {
            return null;
        }

        byte[] encoded = publicKey.getEncoded();

        return Base64.getEncoder().encodeToString(encoded);
    }

    /**
     * @param key the key for which to create a fingerprint
     * @return the fingerprint for the passed key
     * @throws IOException when failing to create a fingerprint
     */
    public String getPublicFingerprint(CertifiedPublicKey key) throws IOException
    {
        return key != null ? DigestUtils.sha256Hex(key.getEncoded()) : null;
    }
}
