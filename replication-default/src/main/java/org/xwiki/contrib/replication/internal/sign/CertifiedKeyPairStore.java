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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.internal.ReplicationFileStore;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.store.FileStoreReference;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.observation.ObservationManager;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate and provide {@link CertifiedKeyPair}.
 * 
 * @version $Id$
 */
@Component(roles = CertifiedKeyPairStore.class)
@Singleton
public class CertifiedKeyPairStore
{
    @Inject
    private CryptTools cryptTools;

    @Inject
    @Named("X509file")
    private KeyStore keyStore;

    @Inject
    private ReplicationFileStore fileStore;

    @Inject
    private ObservationManager observation;

    /**
     * @param instance the instance for which to get the {@link CertifiedKeyPair}
     * @return the new {@link CertifiedKeyPair} associated with the passed instance or a new one if it's the first time
     *         this method is called
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

    /**
     * @param instance the instance for which to create a new {@link CertifiedKeyPair}
     * @return the new {@link CertifiedKeyPair} associated with the passed instance or a new one if it's the first time
     *         this method is called
     * @throws ReplicationException when failing to get or create the {@link CertifiedKeyPair}
     */
    public CertifiedKeyPair createCertifiedKeyPair(String instance) throws ReplicationException
    {
        try {
            CertifiedKeyPair keyPair = cryptTools.createCertifiedKeyPair();

            // Store the key pair
            storeCertifiedKeyPair(instance, keyPair);

            // Notify about the new key
            this.observation.notify(new CertifiedKeyPairCreatedEvent(instance), keyPair);

            return keyPair;
        } catch (Exception e) {
            throw new ReplicationException("Error during the asymetric key creation.", e);
        }
    }

    /**
     * @param instance the instance associated with the {@link CertifiedKeyPair} to store
     * @param keyPair the {@link CertifiedKeyPair} associated with the passed instance
     * @throws KeyStoreException when failing to store the {@link CertifiedKeyPair}
     * @since 1.4.0
     */
    public void storeCertifiedKeyPair(String instance, CertifiedKeyPair keyPair) throws KeyStoreException
    {
        this.keyStore.store(buildFileStoreReference(instance), keyPair);
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
