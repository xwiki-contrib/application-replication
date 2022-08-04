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
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityTemplate;
import org.apache.hc.core5.http.io.entity.EntityUtils;
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
import org.xwiki.contrib.replication.internal.enpoint.message.HttpServletRequestReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.enpoint.message.ReplicationMessageEndpoint;
import org.xwiki.contrib.replication.internal.instance.ReplicationInstanceStore;
import org.xwiki.contrib.replication.internal.sign.SignatureManager;
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

        builder.addParameter(ReplicationInstanceUnregisterEndpoint.PARAMETER_URI,
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

        builder.addParameter(ReplicationInstancePingEndpoint.PARAMETER_KEY, key);
        builder.addParameter(ReplicationInstancePingEndpoint.PARAMETER_SIGNEDKEY,
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
        URIBuilder builder = createURIBuilder(target, ReplicationMessageEndpoint.PATH, message.getId());

        builder.addParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_ID, message.getId());
        builder.addParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_TYPE, message.getType());
        builder.addParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_DATE,
            HttpServletRequestReplicationReceiverMessage.fromDate(message.getDate()));

        String source = message.getSource();
        if (source == null) {
            source = this.instances.getCurrentInstance().getURI();
        }
        builder.addParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_SOURCE, source);

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
                String error;
                try {
                    error = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    error = UNKNWON_ERROR;
                }

                throw new ReplicationException(String.format("Failed to send message [%s] to instance [%s]: %s",
                    message.getId(), target.getURI(), error));
            }
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
        URIBuilder builder = createURIBuilder(instance, ReplicationInstanceUnregisterEndpoint.PATH);

        HttpPut httpPut = new HttpPut(builder.build());

        try (CloseableHttpResponse response = this.client.execute(httpPut)) {
            if (response.getCode() == 200) {
                // TODO: done
            } else if (response.getCode() == 404) {
                // TODO: the instance does not actually exist on the server side
            } else {
                String error;
                try {
                    error = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    error = UNKNWON_ERROR;
                }

                throw new ReplicationException(
                    String.format("Failed to unregister instance [%s]: %s", instance.getURI(), error));
            }
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
        URIBuilder builder = createURIBuilder(instance, ReplicationInstancePingEndpoint.PATH);

        HttpPost httpPost = new HttpPost(builder.build());

        try (CloseableHttpResponse response = this.client.execute(httpPost)) {
            if (response.getCode() == 200) {
                // TODO: done
            } else if (response.getCode() == 404) {
                // TODO: the instance does not actually exist on the server side
            } else {
                String error;
                try {
                    error = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    error = UNKNWON_ERROR;
                }

                throw new ReplicationException(
                    String.format("Failed to ping instance [%s]: %s", instance.getURI(), error));
            }
        }
    }

    /**
     * @param uri the URI of the target instance
     * @return the target instance representation
     * @throws ReplicationException when failing to register the instance
     * @throws IOException when failing to register the instance
     * @throws URISyntaxException when failing to register the instance
     */
    public RegisterResponse register(String uri) throws ReplicationException, IOException, URISyntaxException
    {
        URIBuilder builder = createURIBuilder(uri, ReplicationInstanceRegisterEndpoint.PATH);

        builder.addParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_NAME,
            this.instances.getCurrentInstance().getName());
        builder.addParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_PUBLICKEY,
            this.signatureManager.serializePublicKey(this.signatureManager.getSendPublicKey(uri)));

        HttpPut httpPut = new HttpPut(builder.build());

        try (CloseableHttpResponse response = this.client.execute(httpPut)) {
            if (response.getCode() == 200) {
                // Registered both ways
                Map<String, Object> responseContent = HTTPUtils.fromJSON(response);

                return new RegisterResponse(Status.REGISTERED, this.signatureManager.unserializePublicKey(
                    (String) responseContent.get(ReplicationInstanceRegisterEndpoint.PARAMETER_PUBLICKEY)));
            } else if (response.getCode() == 201) {
                // New registration
            } else if (response.getCode() == 202) {
                // Already requested
            } else {
                String error;
                try {
                    error = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    error = UNKNWON_ERROR;
                }

                throw new ReplicationException(String.format("Failed to register instance [%s]: %s", uri, error));
            }
        }

        return new RegisterResponse(Status.REQUESTED, null);
    }
}
