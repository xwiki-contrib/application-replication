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
package org.xwiki.contrib.replication.internal.instance;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.internal.ReplicationConstants;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component(roles = ReplicationInstanceStore.class)
@Singleton
public class ReplicationInstanceStore
{
    /**
     * The local reference of the document containing the instances.
     */
    public static final LocalDocumentReference REPLICATION_INSTANCES =
        new LocalDocumentReference("Instances", ReplicationConstants.REPLICATION_HOME);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private InstanceIdManager instanceId;

    @Inject
    private Logger logger;

    private ReplicationInstance currentInstance;

    /**
     * @param uri the uri as String
     * @return the clean {@link URIBuilder} instance
     * @throws URISyntaxException if the input is not a valid URI
     */
    public static URIBuilder createURIBuilder(String uri) throws URISyntaxException
    {
        // Cleanup trailing / to avoid empty path element
        return new URIBuilder(DefaultReplicationInstance.cleanURI(uri));
    }

    /**
     * Remove the cached current instance so that it can be recalculated.
     */
    public void resetCurrentInstance()
    {
        this.currentInstance = null;
    }

    /**
     * @return the current instance representation
     * @throws ReplicationException when failing to resolve the create the current instance
     */
    public ReplicationInstance getCurrentInstance() throws ReplicationException
    {
        if (this.currentInstance == null) {
            try {
                String currentURI = getDefaultCurrentURI();
                String currentName = getDefaultCurrentName();

                this.currentInstance = new DefaultReplicationInstance(currentName, currentURI, null);
            } catch (Exception e) {
                throw new ReplicationException("Failed to get the current instance URI", e);
            }
        }

        return this.currentInstance;
    }

    private String getDefaultCurrentURI() throws MalformedURLException, URISyntaxException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // We want the reference URI and not the current one
        // TODO: force getting the main wiki URI for now but it might be interesting to support replication
        // between subwikis of the same instance
        URL url = xcontext.getWiki().getServerURL(xcontext.getMainXWiki(), xcontext);
        String webapp = xcontext.getWiki().getWebAppPath(xcontext);

        URIBuilder builder = createURIBuilder(url.toURI().toString());
        if (webapp != null) {
            builder.appendPath(webapp);
        }

        return builder.build().toString();
    }

    private String getDefaultCurrentName()
    {
        return this.instanceId.getInstanceId().getInstanceId();
    }

    @FunctionalInterface
    private interface InstanceDocumentExecutor<T>
    {
        T execute(XWikiDocument instancesDocument, XWikiContext xcontext) throws XWikiException, ReplicationException;
    }

    private <T> T executeInstanceDocument(InstanceDocumentExecutor<T> executor) throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext == null) {
            throw new ReplicationException("XWiki database is not ready");
        }

        WikiReference currentWiki = xcontext.getWikiReference();
        try {
            xcontext.setWikiId(xcontext.getMainXWiki());

            XWikiDocument instancesDocument = xcontext.getWiki().getDocument(REPLICATION_INSTANCES, xcontext);

            return executor.execute(instancesDocument, xcontext);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to load the replication instances from the store", e);
        } finally {
            xcontext.setWikiReference(currentWiki);
        }
    }

    /**
     * @param instanceObject the object containing a replication instance
     * @return the URI of the instance
     */
    public String getURI(BaseObject instanceObject)
    {
        return instanceObject.getStringValue(StandardReplicationInstanceClassInitializer.FIELD_URI);
    }

    /**
     * @param instanceObject the object containing a replication instance
     * @return the URI of the instance
     */
    public String getName(BaseObject instanceObject)
    {
        return instanceObject.getStringValue(StandardReplicationInstanceClassInitializer.FIELD_NAME);
    }

    /**
     * @param instanceObject the object containing a replication instance
     * @return the URI of the instance
     */
    public Status getStatus(BaseObject instanceObject)
    {
        String statusString = instanceObject.getStringValue(StandardReplicationInstanceClassInitializer.FIELD_STATUS);

        return StringUtils.isEmpty(statusString) ? null : Enum.valueOf(Status.class, statusString);
    }

    private void setStatus(BaseObject instanceObject, Status status)
    {
        instanceObject.setStringValue(StandardReplicationInstanceClassInitializer.FIELD_STATUS, status.name());
    }

    /**
     * @param instanceObject the object to parse
     * @return the {@link ReplicationInstance}
     */
    public ReplicationInstance toReplicationInstance(BaseObject instanceObject)
    {
        return new DefaultReplicationInstance(getName(instanceObject), getURI(instanceObject),
            getStatus(instanceObject));
    }

    private void setReplicationInstance(ReplicationInstance instance, BaseObject instanceObject)
    {
        setReplicationInstance(instance.getName(), instance.getURI(), instance.getStatus(), instanceObject);
    }

    private void setReplicationInstance(String name, String uri, Status status, BaseObject instanceObject)
    {
        instanceObject.setStringValue(StandardReplicationInstanceClassInitializer.FIELD_NAME, name);
        instanceObject.setStringValue(StandardReplicationInstanceClassInitializer.FIELD_URI, uri);

        instanceObject.setStringValue(StandardReplicationInstanceClassInitializer.FIELD_STATUS,
            status != null ? status.name() : "");
    }

    /**
     * @return all the stored instances
     * @throws ReplicationException when failing to load the instances
     */
    public List<ReplicationInstance> loadInstances() throws ReplicationException
    {
        return executeInstanceDocument((instancesDocument, xcontext) -> {
            List<BaseObject> instanceObjects =
                instancesDocument.getXObjects(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE);

            List<ReplicationInstance> instances = new ArrayList<>(instanceObjects.size());
            for (BaseObject instanceObject : instanceObjects) {
                if (instanceObject != null) {
                    // Current instance
                    if (getStatus(instanceObject) == null) {
                        try {
                            String name = getName(instanceObject);
                            if (StringUtils.isBlank(name)) {
                                name = getDefaultCurrentName();
                            }
                            String uri = getURI(instanceObject);
                            if (StringUtils.isBlank(uri)) {

                                uri = getDefaultCurrentURI();
                            }

                            this.currentInstance = new DefaultReplicationInstance(name, uri, null);
                        } catch (Exception e) {
                            // Skip invalid instance
                            this.logger.error("Failed to load instance from xobject with reference [{}]",
                                instanceObject.getReference(), e);
                        }
                    } else {
                        // Remote instance
                        instances.add(toReplicationInstance(instanceObject));
                    }
                }
            }

            return instances;
        });
    }

    /**
     * @param instance the new instance to add
     * @throws ReplicationException when failing to add a new instance
     */
    public void addInstance(ReplicationInstance instance) throws ReplicationException
    {
        executeInstanceDocument((instancesDocument, xcontext) -> {
            BaseObject instanceObject =
                instancesDocument.newXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE, xcontext);

            setReplicationInstance(instance, instanceObject);

            xcontext.getWiki().saveDocument(instancesDocument, "Add instance " + instance.getURI(), xcontext);

            return null;
        });
    }

    /**
     * @param uri the URI of the instance to remove
     * @throws ReplicationException when failing to remove the instance
     */
    public void deleteInstance(String uri) throws ReplicationException
    {
        executeInstanceDocument((instancesDocument, xcontext) -> {
            BaseObject objectToDelete =
                instancesDocument.getXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE,
                    StandardReplicationInstanceClassInitializer.FIELD_URI, uri, false);

            if (objectToDelete != null) {
                instancesDocument.removeXObject(objectToDelete);

                xcontext.getWiki().saveDocument(instancesDocument, "Remove instance " + uri, xcontext);
            }

            return null;
        });
    }

    /**
     * @param instance the instance to update
     * @param status the new status of the instance to update
     * @throws ReplicationException when failing to update the instance status
     */
    public void updateStatus(ReplicationInstance instance, Status status) throws ReplicationException
    {
        executeInstanceDocument((instancesDocument, xcontext) -> {
            BaseObject instanceObject =
                instancesDocument.getXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE,
                    StandardReplicationInstanceClassInitializer.FIELD_URI, instance.getURI(), false);

            setStatus(instanceObject, status);

            xcontext.getWiki().saveDocument(instancesDocument,
                "Set status [" + status.name() + "] for instance " + instance.getURI(), xcontext);

            if (instance instanceof DefaultReplicationInstance) {
                ((DefaultReplicationInstance) instance).setStatus(status);
            }

            return null;
        });
    }

    /**
     * @param instance the instance to update
     * @throws ReplicationException when failing to update the instance
     */
    public void updateInstance(ReplicationInstance instance) throws ReplicationException
    {
        executeInstanceDocument((instancesDocument, xcontext) -> {
            BaseObject instanceObject =
                instancesDocument.getXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE,
                    StandardReplicationInstanceClassInitializer.FIELD_URI, instance.getURI(), false);

            if (instanceObject == null) {
                throw new ReplicationException("The instance with URI [" + instance.getURI() + "] does not exist");
            }

            setReplicationInstance(instance, instanceObject);

            xcontext.getWiki().saveDocument(instancesDocument, "Update instance " + instance.getURI(), xcontext);

            return null;
        });
    }

    /**
     * @param name the custom name of the current instance
     * @param uri the custom uri of the custom instance
     * @throws ReplicationException when failing to update the current instance
     */
    public void saveCurrentInstance(String name, String uri) throws ReplicationException
    {
        executeInstanceDocument((instancesDocument, xcontext) -> {
            BaseObject instanceObject =
                instancesDocument.getXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE,
                    StandardReplicationInstanceClassInitializer.FIELD_STATUS, "", false);

            if (instanceObject == null) {
                instanceObject =
                    instancesDocument.newXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE, xcontext);
            }

            setReplicationInstance(name, uri, null, instanceObject);

            xcontext.getWiki().saveDocument(instancesDocument, "Update current instance", xcontext);

            return null;
        });
    }
}
