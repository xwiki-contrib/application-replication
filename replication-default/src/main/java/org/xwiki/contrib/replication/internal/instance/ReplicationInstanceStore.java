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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.xwiki.contrib.replication.internal.sign.SignatureManager;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

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

    private static final Set<String> STANDARD_PROPERTIES = Set.of(
        StandardReplicationInstanceClassInitializer.FIELD_NAME,
        StandardReplicationInstanceClassInitializer.FIELD_STATUS, StandardReplicationInstanceClassInitializer.FIELD_URI,
        StandardReplicationInstanceClassInitializer.FIELD_RECEIVEKEY);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Provider<InstanceIdManager> instanceIdProvider;

    @Inject
    private SignatureManager signatureManager;

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
            this.currentInstance = loadCurrentInstance();
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
        return this.instanceIdProvider.get().getInstanceId().getInstanceId();
    }

    @FunctionalInterface
    private interface InstanceDocumentExecutor<T>
    {
        T execute(XWikiDocument instancesDocument, XWikiContext xcontext) throws XWikiException, ReplicationException;
    }

    private <T> T executeReadInstanceDocument(InstanceDocumentExecutor<T> executor) throws ReplicationException
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

    private <T> T executeWriteInstanceDocument(InstanceDocumentExecutor<T> executor) throws ReplicationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext == null) {
            throw new ReplicationException("XWiki database is not ready");
        }

        WikiReference currentWiki = xcontext.getWikiReference();
        try {
            xcontext.setWikiId(xcontext.getMainXWiki());

            XWikiDocument instancesDocument = xcontext.getWiki().getDocument(REPLICATION_INSTANCES, xcontext);

            // Avoid modifying the cache document
            instancesDocument = instancesDocument.clone();

            // Make sure the document is hidden
            instancesDocument.setHidden(true);

            // Execute
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
        return instanceObject != null
            ? instanceObject.getStringValue(StandardReplicationInstanceClassInitializer.FIELD_URI) : null;
    }

    /**
     * @param instanceObject the object containing a replication instance
     * @return the URI of the instance
     */
    public String getName(BaseObject instanceObject)
    {
        return instanceObject != null
            ? instanceObject.getStringValue(StandardReplicationInstanceClassInitializer.FIELD_NAME) : null;
    }

    /**
     * @param instanceObject the object containing a replication instance
     * @return the URI of the instance
     */
    public Status getStatus(BaseObject instanceObject)
    {
        Status status = null;

        if (instanceObject != null) {
            String statusString =
                instanceObject.getStringValue(StandardReplicationInstanceClassInitializer.FIELD_STATUS);

            if (StringUtils.isNotEmpty(statusString)) {
                status = Enum.valueOf(Status.class, statusString);
            }
        }

        return status;
    }

    private void setStatus(BaseObject instanceObject, Status status)
    {
        instanceObject.setStringValue(StandardReplicationInstanceClassInitializer.FIELD_STATUS,
            status != null ? status.name() : "");
    }

    /**
     * @param instanceObject the object containing a replication instance
     * @return the public key to use to validate messages sent by this instance
     */
    public CertifiedPublicKey getPublicKey(BaseObject instanceObject)
    {
        if (instanceObject != null) {
            try {
                return this.signatureManager.unserializeKey(
                    instanceObject.getStringValue(StandardReplicationInstanceClassInitializer.FIELD_RECEIVEKEY));
            } catch (IOException e) {
                this.logger.error("Failed to parse public key from [{}]", instanceObject.getReference(), e);
            }
        }

        return null;
    }

    private void setPublicKey(BaseObject instanceObject, CertifiedPublicKey publicKey) throws IOException
    {
        instanceObject.setLargeStringValue(StandardReplicationInstanceClassInitializer.FIELD_RECEIVEKEY,
            this.signatureManager.serializeKey(publicKey));
    }

    private void setProperties(BaseObject instanceObject, Map<String, Object> properties, XWikiContext xcontext)
    {
        if (properties != null) {
            // Make sure all the xobject fields are initialized
            BaseClass instanceClass = instanceObject.getXClass(xcontext);

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                BaseProperty property = getProperty(instanceObject, entry.getKey());
                if (property == null && instanceClass != null) {
                    // If the property does not already exist try to initialize it from the class
                    PropertyClass classProperty = getClassProperty(instanceClass, entry.getKey());
                    if (classProperty != null) {
                        property = classProperty.newProperty();
                        instanceObject.safeput(classProperty.getName(), property);
                    }
                }

                if (property != null) {
                    if (property instanceof ListProperty) {
                        if (entry.getValue() instanceof List) {
                            property.setValue(entry.getValue());
                        } else if (entry.getValue() instanceof Collection) {
                            property.setValue(new ArrayList<>((Collection) entry.getValue()));
                        } else if (entry.getValue() == null) {
                            property.setValue(List.of());
                        } else {
                            property.setValue(List.of(entry.getValue().toString()));
                        }
                    } else {
                        Object value = entry.getValue();
                        if (entry.getValue() instanceof Iterable) {
                            value = ((Iterable) entry.getValue()).iterator().next();
                        }

                        // TODO: add proper support for Number and Date

                        property.setValue(value);
                    }
                }
            }
        }
    }

    private BaseProperty getProperty(BaseObject xobject, String name)
    {
        BaseProperty property = (BaseProperty) xobject.getField(name);

        if (property != null) {
            return property;
        }

        // Try to find the field in any other case
        for (BaseProperty xobjectProperty : (Collection<BaseProperty>) xobject.getFieldList()) {
            if (xobjectProperty.getName().equalsIgnoreCase(name)) {
                return xobjectProperty;
            }
        }

        return null;
    }

    private PropertyClass getClassProperty(BaseClass instanceClass, String name)
    {
        PropertyClass property = (PropertyClass) instanceClass.getField(name);

        if (property != null) {
            return property;
        }

        // Try to find the field in any other case
        for (PropertyClass xclassProperty : (Collection<PropertyClass>) instanceClass.getFieldList()) {
            if (xclassProperty.getName().equalsIgnoreCase(name)) {
                return xclassProperty;
            }
        }

        return null;
    }

    private Map<String, Object> getProperties(BaseObject instanceObject)
    {
        Map<String, Object> properties = new HashMap<>();

        if (instanceObject != null) {
            for (String propertyKey : instanceObject.getPropertyList()) {
                if (!STANDARD_PROPERTIES.contains(propertyKey)) {
                    PropertyInterface property = instanceObject.safeget(propertyKey);
                    if (property instanceof BaseProperty) {
                        properties.put(propertyKey.toLowerCase(), ((BaseProperty) property).getValue());
                    }
                }
            }
        }

        return properties;
    }

    /**
     * @param instanceObject the object to parse
     * @return the {@link ReplicationInstance}
     */
    public ReplicationInstance toReplicationInstance(BaseObject instanceObject)
    {
        return new DefaultReplicationInstance(getName(instanceObject), getURI(instanceObject),
            getStatus(instanceObject), getPublicKey(instanceObject), getProperties(instanceObject));
    }

    private void setReplicationInstance(ReplicationInstance instance, BaseObject instanceObject, XWikiContext xcontext)
        throws IOException
    {
        setReplicationInstance(instance.getName(), instance.getURI(), instance.getStatus(), instance.getReceiveKey(),
            instance.getProperties(), instanceObject, xcontext);
    }

    private void setReplicationInstance(String name, String uri, Status status, CertifiedPublicKey publicKey,
        Map<String, Object> properties, BaseObject instanceObject, XWikiContext xcontext) throws IOException
    {
        instanceObject.setStringValue(StandardReplicationInstanceClassInitializer.FIELD_NAME, name);
        instanceObject.setStringValue(StandardReplicationInstanceClassInitializer.FIELD_URI, uri);

        setStatus(instanceObject, status);

        setPublicKey(instanceObject, publicKey);

        setProperties(instanceObject, properties, xcontext);
    }

    /**
     * @return all the stored instances
     * @throws ReplicationException when failing to load the instances
     */
    public List<ReplicationInstance> loadInstances() throws ReplicationException
    {
        return executeReadInstanceDocument((instancesDocument, xcontext) -> {
            List<BaseObject> instanceObjects =
                instancesDocument.getXObjects(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE);

            List<ReplicationInstance> instances = new ArrayList<>(instanceObjects.size());
            for (BaseObject instanceObject : instanceObjects) {
                if (instanceObject != null) {
                    // Current instance
                    if (getStatus(instanceObject) == null) {
                        try {
                            this.currentInstance = toCurrentReplicationInstance(instanceObject);
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

    private DefaultReplicationInstance loadCurrentInstance() throws ReplicationException
    {
        return executeReadInstanceDocument((instancesDocument, xcontext) -> {
            BaseObject instanceObject =
                instancesDocument.getXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE,
                    StandardReplicationInstanceClassInitializer.FIELD_STATUS, "", false);

            return toCurrentReplicationInstance(instanceObject);
        });

    }

    private DefaultReplicationInstance toCurrentReplicationInstance(BaseObject instanceObject)
        throws ReplicationException
    {
        try {
            String name = getName(instanceObject);
            if (StringUtils.isBlank(name)) {
                name = getDefaultCurrentName();
            }
            String uri = getURI(instanceObject);
            if (StringUtils.isBlank(uri)) {
                uri = getDefaultCurrentURI();
            }

            return new DefaultReplicationInstance(name, uri, null, null, getProperties(instanceObject));
        } catch (Exception e) {
            throw new ReplicationException("Failed to load current instance", e);
        }
    }

    /**
     * @param instance the new instance to add
     * @throws ReplicationException when failing to add a new instance
     */
    public void addInstance(ReplicationInstance instance) throws ReplicationException
    {
        executeWriteInstanceDocument((instancesDocument, xcontext) -> {
            // Avoid modifying the cache document
            instancesDocument = instancesDocument.clone();

            BaseObject instanceObject =
                instancesDocument.newXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE, xcontext);

            try {
                setReplicationInstance(instance, instanceObject, xcontext);
            } catch (IOException e) {
                throw new ReplicationException("Failed to serialize the replication instance [" + instance + "]", e);
            }

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
        executeWriteInstanceDocument((instancesDocument, xcontext) -> {
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
     * @throws ReplicationException when failing to update the instance
     */
    public void updateInstance(ReplicationInstance instance) throws ReplicationException
    {
        executeWriteInstanceDocument((instancesDocument, xcontext) -> {
            BaseObject instanceObject =
                instancesDocument.getXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE,
                    StandardReplicationInstanceClassInitializer.FIELD_URI, instance.getURI(), false);

            if (instanceObject == null) {
                throw new ReplicationException("The instance with URI [" + instance.getURI() + "] does not exist");
            }

            try {
                setReplicationInstance(instance, instanceObject, xcontext);
            } catch (IOException e) {
                throw new ReplicationException("Failed to update the replication instance [" + instance + "]", e);
            }

            xcontext.getWiki().saveDocument(instancesDocument, "Update instance " + instance.getURI(), xcontext);

            return null;
        });
    }

    /**
     * @param instance the instance to update
     * @param status the status to update in the instance
     * @throws ReplicationException when failing to update the instance
     */
    public void updateStatus(ReplicationInstance instance, Status status) throws ReplicationException
    {
        updateInstance(new DefaultReplicationInstance(instance.getName(), instance.getURI(), status,
            instance.getReceiveKey(), instance.getProperties()));
    }

    /**
     * @param name the custom name of the current instance
     * @param uri the custom uri of the custom instance
     * @throws ReplicationException when failing to update the current instance
     */
    public void saveCurrentInstance(String name, String uri) throws ReplicationException
    {
        executeWriteInstanceDocument((instancesDocument, xcontext) -> {
            BaseObject instanceObject =
                instancesDocument.getXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE,
                    StandardReplicationInstanceClassInitializer.FIELD_STATUS, "", false);

            if (instanceObject == null) {
                instanceObject =
                    instancesDocument.newXObject(StandardReplicationInstanceClassInitializer.CLASS_REFERENCE, xcontext);
            }

            try {
                setReplicationInstance(name, uri, null, null, null, instanceObject, xcontext);
            } catch (IOException e) {
                throw new ReplicationException("Failed to current replication instance", e);
            }

            xcontext.getWiki().saveDocument(instancesDocument, "Update current instance", xcontext);

            return null;
        });
    }
}
