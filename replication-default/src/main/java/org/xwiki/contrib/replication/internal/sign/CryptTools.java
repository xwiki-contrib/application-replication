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
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.EnumSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.KeyPairGenerator;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.CertificateGenerator;
import org.xwiki.crypto.pkix.CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.X509ExtensionBuilder;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateGenerationParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateParameters;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Crypt related tools used by several components in replication.
 * 
 * @version $Id$
 * @since 1.4.0
 */
@Component(roles = CryptTools.class)
@Singleton
public class CryptTools implements Initializable
{
    @Inject
    @Named("RSA")
    private KeyPairGenerator keyPairGenerator;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    @Inject
    @Named("X509")
    private CertificateGeneratorFactory certificateGeneratorFactory;

    @Inject
    private X509ExtensionBuilder extensionBuilder;

    @Inject
    @Named("X509")
    private CertificateFactory certificateFactory;

    @Inject
    private AsymmetricKeyFactory keyFactory;

    private X509CertificateGenerationParameters generationParameters;

    @Override
    public void initialize() throws InitializationException
    {
        this.generationParameters = new X509CertificateGenerationParameters(0, this.extensionBuilder
            .addBasicConstraints(true).addKeyUsage(true, EnumSet.of(KeyUsage.keyCertSign, KeyUsage.cRLSign)).build());
    }

    /**
     * @return a new {@link CertifiedKeyPair}
     * @throws IOException when failing to generate the key pair
     * @throws GeneralSecurityException when failing to generate the key pair
     */
    public CertifiedKeyPair createCertifiedKeyPair() throws IOException, GeneralSecurityException
    {
        AsymmetricKeyPair keys = this.keyPairGenerator.generate();
        Signer signer = this.signerFactory.getInstance(true, keys.getPrivate());
        CertificateGenerator certificateGenerator =
            this.certificateGeneratorFactory.getInstance(signer, this.generationParameters);

        // TODO: give more information about the owner instance and the target one
        CertifiedPublicKey certifiedPublicKey = certificateGenerator.generate(new DistinguishedName("O=XWiki"),
            keys.getPublic(), new X509CertificateParameters());

        return new CertifiedKeyPair(keys.getPrivate(), certifiedPublicKey);
    }

    /**
     * @param key the unserialized public key
     * @return the serialized public key
     * @throws IOException when failing to serialize the public key
     */
    public String serializePublicKey(CertifiedPublicKey key) throws IOException
    {
        if (key == null) {
            return null;
        }

        byte[] encoded = key.getEncoded();

        return Base64.getEncoder().encodeToString(encoded);
    }

    /**
     * @param key the serialized public key
     * @return the unserialized public key
     * @throws IOException when failing to unserialize the public key
     */
    public CertifiedPublicKey unserializePublicKey(String key) throws IOException
    {
        return StringUtils.isEmpty(key) ? null : this.certificateFactory.decode(Base64.getDecoder().decode(key));
    }

    /**
     * @param key the unserialized private key
     * @return the serialized private key
     */
    public byte[] serializePrivateKey(PrivateKeyParameters key)
    {
        return key.getEncoded();
    }

    /**
     * @param key the serialized private key
     * @return the unserialized private key
     * @throws IOException when failing to unserialize the public key
     */
    public PrivateKeyParameters unserializePrivateKey(byte[] key) throws IOException
    {
        return this.keyFactory.fromPKCS8(key);
    }

    /**
     * @param pk the key to use to sign the content
     * @param content the content to sign
     * @return the signed content
     * @throws ReplicationException when failing to sign the content
     */
    public String sign(PrivateKeyParameters pk, String content) throws ReplicationException
    {
        try {
            Signer signer = this.signerFactory.getInstance(true, pk);
            signer.update(content.getBytes(UTF_8));

            return Base64.getEncoder().encodeToString(signer.generate());
        } catch (Exception e) {
            throw new ReplicationException(String.format("Error while signing [%s]", content), e);
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
        if (instance.getReceiveKey() == null) {
            return true;
        }

        try {
            Signer signer = this.signerFactory.getInstance(false, instance.getReceiveKey().getPublicKeyParameters());
            signer.update(content.getBytes(UTF_8));

            return signer.verify(Base64.getDecoder().decode(signedContent));
        } catch (Exception e) {
            throw new ReplicationException(
                String.format("Error while verifying signature [%s] for [%s]", signedContent, instance), e);
        }
    }
}
