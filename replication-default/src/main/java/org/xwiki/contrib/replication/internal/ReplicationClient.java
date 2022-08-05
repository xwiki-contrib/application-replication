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
package org.xwiki.contrib.replication.internal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityTemplate;
import org.apache.hc.core5.net.URIBuilder;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.internal.enpoint.ReplicationResourceReferenceHandler;
import org.xwiki.contrib.replication.internal.enpoint.instance.ReplicationInstancePingEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.instance.ReplicationInstanceRegisterEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.instance.ReplicationInstanceUnregisterEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.instance.ReplicationInstanceUpdateKeyEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.message.HttpServletRequestReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.enpoint.message.ReplicationMessageEndpoint;
import org.xwiki.contrib.replication.internal.instance.ReplicationInstanceStore;
import org.xwiki.contrib.replication.internal.sign.CertifiedKeyPairStore;
import org.xwiki.contrib.replication.internal.sign.SignatureManager;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;

/**
 * @version $Id$
 */
@Component(roles = ReplicationClient.class)
@Singleton
public class ReplicationClient implements Initializable
{
    private static final String UNKNWON_ERROR = "Unknown server error";

    @Inject
    private ReplicationInstanceStore instances;

    @Inject
    private SignatureManager signatureManager;

    @Inject
    private CertifiedKeyPairStore signatureStore;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private CloseableHttpClient client;

    /**
     * The result of the register.
     * 
     * @version $Id$
     */
    public class RegisterResponse
    {
        private final Status status;

        private final CertifiedPublicKey publicKey;

        /**
         * @param status the status of the instance
         * @param publicKey the public key to use to validate message sent by the target instance
         */
        public RegisterResponse(Status status, CertifiedPublicKey publicKey)
        {
            this.status = status;
            this.publicKey = publicKey;
        }

        /**
         * @return the status of the instance
         */
        public Status getStatus()
        {
            return this.status;
        }

        /**
         * @return the publicKey
         */
        public CertifiedPublicKey getPublicKey()
        {
            return this.publicKey;
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.client = HttpClients.createSystem();
    }

    private URIBuilder createURIBuilder(String uri, String endpoint) throws URISyntaxException, ReplicationException
    {
        URIBuilder builder = ReplicationInstanceStore.createURIBuilder(uri);
        builder.appendPath(ReplicationResourceReferenceHandler.HINT);
        builder.appendPath(endpoint);

        builder.setParameter(ReplicationInstanceUnregisterEndpoint.PARAMETER_URI,
            this.instances.getCurrentInstance().getURI());

        return builder;
    }

    private URIBuilder createURIBuilder(ReplicationInstance target, String endpoint)
        throws URISyntaxException, ReplicationException
    {
        return createURIBuilder(target, endpoint, String.valueOf(new Date().getTime()));
    }

    private URIBuilder createURIBuilder(ReplicationInstance target, String endpoint, String key)
        throws URISyntaxException, ReplicationException
    {
        URIBuilder builder = createURIBuilder(target.getURI(), endpoint);

        builder.setParameter(ReplicationInstancePingEndpoint.PARAMETER_KEY, key);
        builder.setParameter(ReplicationInstancePingEndpoint.PARAMETER_SIGNEDKEY,
            this.signatureManager.sign(target, key));

        return builder;
    }

    /**
     * @param message the message to send
     * @param target the instance to send the message to
     * @throws ReplicationException when failing to send the message
     * @throws URISyntaxException when failing to send the message
     * @throws IOException when failing to send the message
     */
    public void sendMessage(ReplicationSenderMessage message, ReplicationInstance target)
        throws ReplicationException, URISyntaxException, IOException
    {
        this.lock.readLock().lock();

        try {
            URIBuilder builder = createURIBuilder(target, ReplicationMessageEndpoint.PATH, message.getId());

            builder.setParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_ID, message.getId());
            builder.setParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_TYPE, message.getType());
            builder.setParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_DATE,
                HttpServletRequestReplicationReceiverMessage.fromDate(message.getDate()));

            String source = message.getSource();
            if (source == null) {
                source = this.instances.getCurrentInstance().getURI();
            }
            builder.setParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_SOURCE, source);

            HttpPut httpPut = new HttpPut(builder.build());

            httpPut.setEntity(new EntityTemplate(-1, ContentType.DEFAULT_BINARY, null, message::write));

            // Add custom headers
            for (Map.Entry<String, Collection<String>> entry : message.getCustomMetadata().entrySet()) {
                String header = HttpServletRequestReplicationReceiverMessage.HEADER_METADATA_PREFIX + entry.getKey();

                httpPut.setHeader(header, HTTPUtils.toString(entry.getValue()));
            }

            try (CloseableHttpResponse response = this.client.execute(httpPut)) {
                if (response.getCode() == 200) {
                    // TODO: done
                } else {
                    String error = HTTPUtils.getContent(response, UNKNWON_ERROR);

                    throw new ReplicationException(String.format("Failed to send message [%s] to instance [%s]: %s",
                        message.getId(), target.getURI(), error));
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * @param instance the instance to remove
     * @throws ReplicationException when failing to unregister the instance
     * @throws URISyntaxException when failing to unregister the instance
     * @throws IOException when failing to unregister the instance
     */
    public void unregister(ReplicationInstance instance) throws ReplicationException, URISyntaxException, IOException
    {
        this.lock.readLock().lock();

        try {
            URIBuilder builder = createURIBuilder(instance, ReplicationInstanceUnregisterEndpoint.PATH);

            HttpPut httpPut = new HttpPut(builder.build());

            try (CloseableHttpResponse response = this.client.execute(httpPut)) {
                if (response.getCode() == 200) {
                    // TODO: done
                } else if (response.getCode() == 404) {
                    // TODO: the instance does not actually exist on the server side
                } else {
                    String error = HTTPUtils.getContent(response, UNKNWON_ERROR);

                    throw new ReplicationException(
                        String.format("Failed to unregister instance [%s]: %s", instance.getURI(), error));
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * @param instance the instance to ping
     * @throws ReplicationException when failing to unregister the instance
     * @throws URISyntaxException when failing to unregister the instance
     * @throws IOException when failing to unregister the instance
     */
    public void ping(ReplicationInstance instance) throws ReplicationException, URISyntaxException, IOException
    {
        this.lock.readLock().lock();

        try {
            URIBuilder builder = createURIBuilder(instance, ReplicationInstancePingEndpoint.PATH);

            HttpPost httpPost = new HttpPost(builder.build());

            try (CloseableHttpResponse response = this.client.execute(httpPost)) {
                if (response.getCode() == 200) {
                    // TODO: done
                } else if (response.getCode() == 404) {
                    // TODO: the instance does not actually exist on the server side
                } else {
                    String error = HTTPUtils.getContent(response, UNKNWON_ERROR);

                    throw new ReplicationException(
                        String.format("Failed to ping instance [%s]: %s", instance.getURI(), error));
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Request passed instance for a link.
     * 
     * @param uri the URI of the target instance
     * @throws ReplicationException when failing to register the instance
     * @throws IOException when failing to register the instance
     * @throws URISyntaxException when failing to register the instance
     */
    public void register(String uri) throws ReplicationException, IOException, URISyntaxException
    {
        URIBuilder builder = createURIBuilder(uri, ReplicationInstanceRegisterEndpoint.PATH);

        builder.setParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_NAME,
            this.instances.getCurrentInstance().getName());

        // Indicate the key that will be used to send messages to the target instance
        builder.setParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_RECEIVEKEY,
            this.signatureManager.serializeKey(this.signatureManager.getSendKey(uri)));

        HttpPut httpPut = new HttpPut(builder.build());

        try (CloseableHttpResponse response = this.client.execute(httpPut)) {
            if (response.getCode() == 201) {
                // New registration
            } else if (response.getCode() == 202) {
                // Already requested
            } else {
                String error = HTTPUtils.getContent(response, UNKNWON_ERROR);

                throw new ReplicationException(String.format("Failed to register instance [%s]: %s", uri, error));
            }
        }
    }

    /**
     * Accept the link requested by the passed instance.
     * 
     * @param instance the the instance for which to accept the link
     * @return the target instance representation
     * @throws ReplicationException when failing to register the instance
     * @throws IOException when failing to register the instance
     * @throws URISyntaxException when failing to register the instance
     */
    public Status accept(ReplicationInstance instance) throws ReplicationException, IOException, URISyntaxException
    {
        this.lock.readLock().lock();

        try {
            URIBuilder builder = createURIBuilder(instance, ReplicationInstanceRegisterEndpoint.PATH);

            builder.setParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_NAME,
                this.instances.getCurrentInstance().getName());

            // Indicate the key that will be used to send messages to the target instance
            builder.setParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_RECEIVEKEY,
                this.signatureManager.serializeKey(this.signatureManager.getSendKey(instance)));

            // Send the receive key as a key to prove it was requested in the first place
            builder.setParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_REQUESTKEY,
                this.signatureManager.serializeKey(instance.getReceiveKey()));

            HttpPut httpPut = new HttpPut(builder.build());

            try (CloseableHttpResponse response = this.client.execute(httpPut)) {
                if (response.getCode() == 200) {
                    // Registered both ways
                    return Status.REGISTERED;
                } else if (response.getCode() == 201) {
                    // New registration
                } else if (response.getCode() == 202) {
                    // Already requested
                } else {
                    String error = HTTPUtils.getContent(response, UNKNWON_ERROR);

                    throw new ReplicationException(
                        String.format("Failed to accept instance [%s]: %s", instance.getURI(), error));
                }
            }

            return Status.REQUESTED;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Create a new key and replace the one current used to send message to the passed instance.
     * 
     * @param instance the the instance for which to reset the send key
     * @throws ReplicationException when failing to reset the instance send key
     * @throws IOException when failing to reset the instance send key
     * @throws URISyntaxException when failing to reset the instance send key
     */
    public void resetSendKey(ReplicationInstance instance) throws ReplicationException, URISyntaxException, IOException
    {
        this.lock.writeLock().lock();

        try {
            // Get the previous key
            CertifiedKeyPair oldKey = this.signatureStore.getCertifiedKeyPair(instance.getURI());

            // Generate the new key
            CertifiedKeyPair newKey = this.signatureStore.createCertifiedKeyPair(instance.getURI());

            // Notify the instance about the change
            sendKey(instance, oldKey, newKey);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void sendKey(ReplicationInstance instance, CertifiedKeyPair oldKey, CertifiedKeyPair newKey)
        throws URISyntaxException, ReplicationException, IOException
    {
        URIBuilder builder = createURIBuilder(instance.getURI(), ReplicationInstanceUpdateKeyEndpoint.PATH);

        // The target instance expect the old key
        String key = String.valueOf(new Date().getTime());
        builder.setParameter(ReplicationInstancePingEndpoint.PARAMETER_KEY, key);
        builder.setParameter(ReplicationInstancePingEndpoint.PARAMETER_SIGNEDKEY,
            this.signatureManager.sign(oldKey.getPrivateKey(), key));

        // Indicate the new key
        builder.setParameter(ReplicationInstanceUpdateKeyEndpoint.PARAMETER_NEWRECEIVEKEY,
            this.signatureManager.serializeKey(newKey.getCertificate()));

        HttpPost httpPost = new HttpPost(builder.build());

        try (CloseableHttpResponse response = this.client.execute(httpPost)) {
            if (response.getCode() == 200) {
                // Done
            } else if (response.getCode() == 404) {
                // The instance does not actually exist on the server side
            } else {
                String error = HTTPUtils.getContent(response, UNKNWON_ERROR);

                throw new ReplicationException(
                    String.format("Failed to update the key in the instance [%s]: %s", instance.getURI(), error));
            }
        }
    }
}
