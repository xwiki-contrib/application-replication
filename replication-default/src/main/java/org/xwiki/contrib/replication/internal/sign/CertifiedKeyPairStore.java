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

import java.io.File;
import java.net.URLEncoder;
import java.util.EnumSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.internal.ReplicationFileStore;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.crypto.KeyPairGenerator;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
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
import org.xwiki.crypto.store.FileStoreReference;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.KeyStoreException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate and provide {@link CertifiedKeyPair}.
 * 
 * @version $Id$
 */
@Component(roles = CertifiedKeyPairStore.class)
@Singleton
public class CertifiedKeyPairStore implements Initializable
{
    @Inject
    @Named("X509file")
    private KeyStore keyStore;

    @Inject
    @Named("RSA")
    private KeyPairGenerator keyPairGenerator;

    @Inject
    @Named("X509")
    private CertificateGeneratorFactory certificateGeneratorFactory;

    @Inject
    private X509ExtensionBuilder extensionBuilder;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    @Inject
    private ReplicationFileStore fileStore;

    private X509CertificateGenerationParameters generationParameters;

    @Override
    public void initialize() throws InitializationException
    {
        this.generationParameters = new X509CertificateGenerationParameters(0, this.extensionBuilder
            .addBasicConstraints(true).addKeyUsage(true, EnumSet.of(KeyUsage.keyCertSign, KeyUsage.cRLSign)).build());
    }

    /**
     * @param instance the instance for which to get the {@link CertifiedKeyPair}
     * @return the {@link CertifiedKeyPair} associated with the passed instance or a new one if it's the first time this
     *         method is called
     * @throws ReplicationException when failing to get or create the {@link CertifiedKeyPair}
     */
    public CertifiedKeyPair getCertifiedKeyPair(String instance) throws ReplicationException
    {
        CertifiedKeyPair stored;
        try {
            stored = this.keyStore.retrieve(buildFileStoreReference(instance));
        } catch (KeyStoreException e) {
            stored = null;
        }

        if (stored != null) {
            return stored;
        }

        return createCertifiedKeyPair(instance);
    }

    private CertifiedKeyPair createCertifiedKeyPair(String instance) throws ReplicationException
    {
        AsymmetricKeyPair keys = this.keyPairGenerator.generate();
        Signer signer = this.signerFactory.getInstance(true, keys.getPrivate());
        CertificateGenerator certificateGenerator =
            this.certificateGeneratorFactory.getInstance(signer, this.generationParameters);

        try {
            // TODO: give more information about the owner instance and the target one
            CertifiedPublicKey certifiedPublicKey = certificateGenerator.generate(new DistinguishedName("O=XWiki"),
                keys.getPublic(), new X509CertificateParameters());
            CertifiedKeyPair keyPair = new CertifiedKeyPair(keys.getPrivate(), certifiedPublicKey);
            this.keyStore.store(buildFileStoreReference(instance), keyPair);

            return keyPair;
        } catch (Exception e) {
            throw new ReplicationException("Error during the asymetric key creation.", e);
        }
    }

    private FileStoreReference buildFileStoreReference(String instance)
    {
        File permDirectory = this.fileStore.getReplicationFolder();
        File keysDirectory = new File(permDirectory, "keys");
        keysDirectory.mkdirs();
        File file = new File(keysDirectory,
            String.format("%s.key", URLEncoder.encode(DefaultReplicationInstance.cleanURI(instance), UTF_8)));

        return new FileStoreReference(file);
    }
}
