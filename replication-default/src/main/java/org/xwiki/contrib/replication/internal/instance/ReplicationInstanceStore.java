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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWiki;
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
     * The local reference of the space where all replication related information are stored.
     */
    public static final EntityReference REPLICATION_HOME =
        new EntityReference("Replication", EntityType.SPACE, new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    /**
     * The local reference of the space where all replication related information are stored as a String.
     */
    public static final String REPLICATION_HOME_STRING = XWiki.SYSTEM_SPACE + ".Replication";

    /**
     * The local reference of the document containing the instances.
     */
    public static final LocalDocumentReference REPLICATION_INSTANCES =
        new LocalDocumentReference("Instances", REPLICATION_HOME);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @FunctionalInterface
    private interface InstanceDocumentExecutor<T>
    {
        T execute(XWikiDocument instancesDocument, XWikiContext xcontext) throws XWikiException;
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

    private String getName(BaseObject instanceObject)
    {
        return instanceObject.getStringValue(ReplicationInstanceClassInitializer.FIELD_NAME);
    }

    private String getURI(BaseObject instanceObject)
    {
        return instanceObject.getStringValue(ReplicationInstanceClassInitializer.FIELD_URI);
    }

    private Status getStatus(BaseObject instanceObject)
    {
        return Enum.valueOf(Status.class,
            instanceObject.getStringValue(ReplicationInstanceClassInitializer.FIELD_STATUS));
    }

    private void setStatus(BaseObject instanceObject, Status status)
    {
        instanceObject.setStringValue(ReplicationInstanceClassInitializer.FIELD_STATUS, status.name());
    }

    private ReplicationInstance toReplicationInstance(BaseObject instanceObject)
    {
        return new DefaultReplicationInstance(getName(instanceObject), getURI(instanceObject),
            getStatus(instanceObject));
    }

    private void setReplicationInstance(ReplicationInstance instance, BaseObject instanceObject)
    {
        instanceObject.setStringValue(ReplicationInstanceClassInitializer.FIELD_NAME, instance.getName());
        instanceObject.setStringValue(ReplicationInstanceClassInitializer.FIELD_URI, instance.getURI());

        instanceObject.setStringValue(ReplicationInstanceClassInitializer.FIELD_STATUS, instance.getStatus().name());
    }

    /**
     * @return all the stored instances
     * @throws ReplicationException when failing to load the instances
     */
    public List<ReplicationInstance> loadInstances() throws ReplicationException
    {
        return executeInstanceDocument((instancesDocument, xcontext) -> {
            List<BaseObject> instanceObjects =
                instancesDocument.getXObjects(ReplicationInstanceClassInitializer.CLASS_REFERENCE);

            List<ReplicationInstance> instances = new ArrayList<>(instanceObjects.size());
            for (BaseObject instanceObject : instanceObjects) {
                if (instanceObject != null) {
                    ReplicationInstance instance = toReplicationInstance(instanceObject);

                    instances.add(instance);
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
                instancesDocument.newXObject(ReplicationInstanceClassInitializer.CLASS_REFERENCE, xcontext);

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
            BaseObject objectToDelete = instancesDocument.getXObject(REPLICATION_HOME,
                ReplicationInstanceClassInitializer.FIELD_URI, uri, false);

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
            BaseObject instanceObject = instancesDocument.getXObject(REPLICATION_HOME,
                ReplicationInstanceClassInitializer.FIELD_URI, instance.getURI(), false);

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
            BaseObject instanceObject = instancesDocument.getXObject(REPLICATION_HOME,
                ReplicationInstanceClassInitializer.FIELD_URI, instance.getURI(), false);

            setReplicationInstance(instance, instanceObject);

            xcontext.getWiki().saveDocument(instancesDocument, "Update instance " + instance.getURI(), xcontext);

            return null;
        });
    }
}
