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

import java.security.SecureRandom;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.internal.ReplicationFileStore;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.crypto.internal.asymmetric.generator.BcRSAKeyPairGenerator;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.internal.asymmetric.keyfactory.DefaultKeyFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.internal.encoder.HexBinaryStringEncoder;
import org.xwiki.crypto.password.internal.DefaultPrivateKeyPasswordBasedEncryptor;
import org.xwiki.crypto.pkix.internal.BcX509CertificateFactory;
import org.xwiki.crypto.pkix.internal.BcX509CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.internal.extension.DefaultX509ExtensionBuilder;
import org.xwiki.crypto.signer.internal.factory.BcSHA256withRsaSignerFactory;
import org.xwiki.crypto.signer.internal.factory.DefaultSignerFactory;
import org.xwiki.crypto.store.filesystem.internal.X509KeyFileSystemStore;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validate {@link SignatureManager}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({CertifiedKeyPairStore.class, ReplicationFileStore.class, TestEnvironment.class,
    BcRSAKeyPairGenerator.class, BcX509CertificateGeneratorFactory.class, DefaultSignerFactory.class,
    BcRSAKeyFactory.class, BcSHA256withRsaSignerFactory.class, X509KeyFileSystemStore.class,
    DefaultPrivateKeyPasswordBasedEncryptor.class, DefaultKeyFactory.class, Base64BinaryStringEncoder.class,
    HexBinaryStringEncoder.class, BcX509CertificateFactory.class, DefaultX509ExtensionBuilder.class})
class SignatureManagerTest
{
    @MockComponent
    private Provider<SecureRandom> secureRandomProvider;

    @InjectMockComponents
    private SignatureManager signatureManager;

    @BeforeEach
    void beforeEach()
    {
        SecureRandom random = new SecureRandom();
        when(this.secureRandomProvider.get()).thenReturn(random);
    }

    @Test
    void signVerify() throws ReplicationException
    {
        DefaultReplicationInstance instance1 = new DefaultReplicationInstance("name1", "uri1", Status.REGISTERED, null, null);
        DefaultReplicationInstance instance2 = new DefaultReplicationInstance("name2", "uri2", Status.REGISTERED, null, null);

        String signature1 = this.signatureManager.sign(instance1, "content");
        assertNotEquals("content", signature1);
        String signature2 = this.signatureManager.sign(instance2, "content");
        assertNotEquals("content", signature2);
        
        // Empty public key
        assertTrue(this.signatureManager.verify(instance1, "content", signature1));
        assertTrue(this.signatureManager.verify(instance2, "content", signature1));
        assertTrue(this.signatureManager.verify(instance1, "content", signature2));        
        assertTrue(this.signatureManager.verify(instance2, "content", signature2));        

        // Set public keys
        instance1.setReceiveKey(this.signatureManager.getSendKey(instance1));
        instance2.setReceiveKey(this.signatureManager.getSendKey(instance2));

        assertTrue(this.signatureManager.verify(instance1, "content", signature1));
        assertFalse(this.signatureManager.verify(instance1, "othercontent", signature1));

        assertTrue(this.signatureManager.verify(instance2, "content", signature2));
        assertFalse(this.signatureManager.verify(instance2, "othercontent", signature2));

        assertFalse(this.signatureManager.verify(instance1, "content", signature2));
        assertFalse(this.signatureManager.verify(instance2, "content", signature1));
    }
}
