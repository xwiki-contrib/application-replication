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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

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
import org.xwiki.contrib.replication.internal.enpoint.instance.ReplicationInstanceRegisterEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.instance.ReplicationInstanceUnregisterEndpoint;
import org.xwiki.contrib.replication.internal.enpoint.message.HttpServletRequestReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.enpoint.message.ReplicationMessageEndpoint;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.instance.InstanceIdManager;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 */
@Component(roles = ReplicationClient.class)
@Singleton
public class ReplicationClient implements Initializable
{
    private static final String UNKNWON_ERROR = "Unknown server error";

    private CloseableHttpClient client;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private InstanceIdManager instanceId;

    private String currentURI;

    @Override
    public void initialize() throws InitializationException
    {
        this.client = HttpClients.createSystem();
    }

    private URIBuilder createURIBuilder(String uri, String endpoint) throws URISyntaxException
    {
        URIBuilder builder = new URIBuilder(uri);
        builder.appendPath(endpoint);

        return builder;
    }

    private String getCurrentURI() throws ReplicationException
    {
        if (this.currentURI != null) {
            try {
                // We want the reference URI and not the current one
                XWikiContext xcontext = this.xcontextProvider.get();
                URL url = xcontext.getWiki().getServerURL(xcontext.getMainXWiki(), xcontext);
                String webapp = xcontext.getWiki().getWebAppPath(xcontext);

                URIBuilder builder = new URIBuilder(url.toURI());
                if (webapp != null) {
                    builder.appendPath(webapp);
                }
                this.currentURI = builder.build().toString();
            } catch (Exception e) {
                throw new ReplicationException("Failedd to get the current instance URI", e);
            }
        }

        return this.currentURI;
    }

    private String getCurrentName()
    {
        // TODO: introduce a configuration for the instance name
        return this.instanceId.getInstanceId().getInstanceId();
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
        URIBuilder builder = createURIBuilder(target.getURI(), ReplicationMessageEndpoint.PATH);

        builder.addParameter(ReplicationMessageEndpoint.PARAMETER_INSTANCE, getCurrentURI());
        // TODO: send a key

        builder.addParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_ID, message.getId());
        builder.addParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_TYPE, message.getType());
        builder.addParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_DATE,
            HttpServletRequestReplicationReceiverMessage.fromDate(message.getDate()));
        builder.addParameter(HttpServletRequestReplicationReceiverMessage.PARAMETER_SOURCE,
            message.getSource().getURI());

        HttpPut httpPut = new HttpPut(builder.build());

        httpPut.setEntity(new EntityTemplate(-1, ContentType.DEFAULT_BINARY, null, s -> message.write(s)));

        // Add custom headers
        for (Map.Entry<String, Collection<String>> entry : message.getCustomMetadata().entrySet()) {
            String header = HttpServletRequestReplicationReceiverMessage.HEADER_METADATA_PREFIX + entry.getKey();

            entry.getValue().forEach(v -> httpPut.addHeader(header, v));
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
        URIBuilder builder = createURIBuilder(instance.getURI(), ReplicationInstanceUnregisterEndpoint.PATH);
        builder.addParameter(ReplicationInstanceUnregisterEndpoint.PARAMETER_URI, getCurrentURI());
        // TODO: send a key

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
                    String.format("Failedd to unregister instance [%s]: %s", instance.getURI(), error));
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
    public ReplicationInstance register(String uri) throws ReplicationException, IOException, URISyntaxException
    {
        URIBuilder builder = createURIBuilder(uri, ReplicationInstanceRegisterEndpoint.PATH);
        builder.addParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_URI, getCurrentURI());
        builder.addParameter(ReplicationInstanceRegisterEndpoint.PARAMETER_NAME, getCurrentName());
        // TODO: send a key

        HttpPut httpPut = new HttpPut(builder.build());

        try (CloseableHttpResponse response = this.client.execute(httpPut)) {
            if (response.getCode() == 200) {
                // TODO: registered both ways
                return new DefaultReplicationInstance(null, uri, Status.REGISTERED);
            } else if (response.getCode() == 201) {
                // TODO: new registration
            } else if (response.getCode() == 202) {
                // TODO: already registered
                return new DefaultReplicationInstance(null, uri, Status.REGISTERED);
            } else if (response.getCode() == 204) {
                // TODO: already requested
            } else {
                String error;
                try {
                    error = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    error = UNKNWON_ERROR;
                }

                throw new ReplicationException(String.format("Failedd to register instance [%s]: %s", uri, error));
            }
        }

        return new DefaultReplicationInstance(null, uri, Status.REQUESTED);
    }
}
