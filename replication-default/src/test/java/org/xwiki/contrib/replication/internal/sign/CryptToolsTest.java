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
import java.security.SecureRandom;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.replication.internal.ReplicationFileStore;
import org.xwiki.crypto.internal.asymmetric.generator.BcRSAKeyPairGenerator;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.internal.asymmetric.keyfactory.DefaultKeyFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.internal.encoder.HexBinaryStringEncoder;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.password.internal.DefaultPrivateKeyPasswordBasedEncryptor;
import org.xwiki.crypto.pkix.internal.BcX509CertificateFactory;
import org.xwiki.crypto.pkix.internal.BcX509CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.internal.extension.DefaultX509ExtensionBuilder;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.signer.internal.factory.BcSHA256withRsaSignerFactory;
import org.xwiki.crypto.signer.internal.factory.DefaultSignerFactory;
import org.xwiki.crypto.store.filesystem.internal.X509KeyFileSystemStore;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CryptTools}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({CertifiedKeyPairStore.class, ReplicationFileStore.class, TestEnvironment.class,
    BcRSAKeyPairGenerator.class, BcX509CertificateGeneratorFactory.class, DefaultSignerFactory.class,
    BcRSAKeyFactory.class, BcSHA256withRsaSignerFactory.class, X509KeyFileSystemStore.class,
    DefaultPrivateKeyPasswordBasedEncryptor.class, DefaultKeyFactory.class, Base64BinaryStringEncoder.class,
    HexBinaryStringEncoder.class, BcX509CertificateFactory.class, DefaultX509ExtensionBuilder.class})
class CryptToolsTest
{
    @MockComponent
    private Provider<SecureRandom> secureRandomProvider;

    @InjectMockComponents
    private CryptTools cryptTools;

    @BeforeEach
    void beforeEach()
    {
        SecureRandom random = new SecureRandom();
        when(this.secureRandomProvider.get()).thenReturn(random);
    }

    @Test
    void keySerialization() throws IOException, GeneralSecurityException
    {
        CertifiedKeyPair keyPair = this.cryptTools.createCertifiedKeyPair();

        String publicKeyString = this.cryptTools.serializePublicKey(keyPair.getCertificate());
        CertifiedPublicKey publicKey = this.cryptTools.unserializePublicKey(publicKeyString);

        assertEquals(keyPair.getCertificate(), publicKey);

        byte[] privateKeyBytes = this.cryptTools.serializePrivateKey(keyPair.getPrivateKey());
        PrivateKeyParameters privateKey = this.cryptTools.unserializePrivateKey(privateKeyBytes);

        assertEquals(keyPair.getPrivateKey(), privateKey);
    }
}
