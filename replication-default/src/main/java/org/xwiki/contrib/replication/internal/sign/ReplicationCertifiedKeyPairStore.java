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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.internal.ReplicationFileStore;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.store.FileStoreReference;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.observation.ObservationManager;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate and provide {@link ReplicationCertifiedKeyPair}.
 * 
 * @version $Id$
 * @since 2.1.0
 */
@Component(roles = ReplicationCertifiedKeyPairStore.class)
@Singleton
public class ReplicationCertifiedKeyPairStore
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

    @Inject
    private Logger logger;

    /**
     * @param instance the instance for which to get the {@link CertifiedKeyPair}
     * @param create if true and no key already exist, create a new one
     * @return the new {@link CertifiedKeyPair} associated with the passed instance or a new one if it's the first time
     *         this method is called
     * @throws ReplicationException when failing to get or create the {@link CertifiedKeyPair}
     * @since 2.1.0
     */
    public ReplicationCertifiedKeyPair getCertifiedKeyPair(String instance, boolean create) throws ReplicationException
    {
        FileStoreReference file = buildFileStoreReference(instance);

        if (file.getFile().exists()) {
            ReplicationCertifiedKeyPair stored = null;
            try {
                BasicFileAttributes attr = Files.readAttributes(file.getFile().toPath(), BasicFileAttributes.class);
                FileTime fileTime = attr.lastModifiedTime();
                stored = new ReplicationCertifiedKeyPair(this.keyStore.retrieve(file), new Date(fileTime.toMillis()));
            } catch (Exception e) {
                if (create) {
                    this.logger.error("Failed to read the stored certified key pair. Trying to create a new one.", e);
                } else {
                    throw new ReplicationException("Failed to read the stored certified key pair", e);
                }
            }

            if (stored != null) {
                return stored;
            }
        }

        return create ? createCertifiedKeyPair(instance) : null;
    }

    /**
     * @param instance the instance for which to create a new {@link CertifiedKeyPair}
     * @return the new {@link CertifiedKeyPair} associated with the passed instance or a new one if it's the first time
     *         this method is called
     * @throws ReplicationException when failing to get or create the {@link CertifiedKeyPair}
     */
    public ReplicationCertifiedKeyPair createCertifiedKeyPair(String instance) throws ReplicationException
    {
        try {
            ReplicationCertifiedKeyPair keyPair =
                new ReplicationCertifiedKeyPair(this.cryptTools.createCertifiedKeyPair());

            // Store the key pair
            storeCertifiedKeyPair(instance, keyPair.getKey());

            // Notify about the new key
            this.observation.notify(new ReplicationCertifiedKeyPairCreatedEvent(instance), keyPair);

            return keyPair;
        } catch (Exception e) {
            throw new ReplicationException("Error during the asymetric key creation.", e);
        }
    }

    /**
     * @param instance the instance associated with the {@link CertifiedKeyPair} to store
     * @param keyPair the {@link CertifiedKeyPair} associated with the passed instance
     * @throws KeyStoreException when failing to store the {@link CertifiedKeyPair}
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

    /**
     * @param instance the instance for which to refresh the key
     * @throws ReplicationException when failing to get the current key pair
     */
    public void refresh(ReplicationInstance instance) throws ReplicationException
    {
        ReplicationCertifiedKeyPair keyPair = getCertifiedKeyPair(instance.getURI(), false);

        this.observation.notify(new ReplicationCertifiedKeyPairRefreshEvent(instance.getURI()), keyPair);
    }
}
