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
import org.xwiki.contrib.replication.internal.instance.ReplicationInstanceClassInitializer.ReplicationInstanceStatus;
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
    public static final EntityReference REPLICATION_HOME =
        new EntityReference("Replication", EntityType.SPACE, new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    public static final String REPLICATION_HOME_STRING = XWiki.SYSTEM_SPACE + ".Replication";

    public static final LocalDocumentReference REPLICATION_INSTANCES =
        new LocalDocumentReference("Instances", REPLICATION_HOME);

    public static final LocalDocumentReference REPLICATION_REQUESTING_INSTANCES =
        new LocalDocumentReference("RequestingInstances", REPLICATION_HOME);

    public static final LocalDocumentReference REPLICATION_REQUESTED_INSTANCES =
        new LocalDocumentReference("RequestedInstances", REPLICATION_HOME);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @FunctionalInterface
    private interface InstanceDocumentExecutor<T>
    {
        T execute(XWikiDocument instancesDocument) throws ReplicationException;
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

            return executor.execute(instancesDocument);
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to load the replication instances from the store", e);
        } finally {
            xcontext.setWikiReference(currentWiki);
        }
    }

    private List<ReplicationInstance> loadInstances(String status) throws ReplicationException
    {
        return executeInstanceDocument(instancesDocument -> {
            List<BaseObject> instanceObjects =
                instancesDocument.getXObjects(ReplicationInstanceClassInitializer.CLASS_REFERENCE);

            List<ReplicationInstance> instances = new ArrayList<>(instanceObjects.size());
            for (BaseObject instanceObject : instanceObjects) {
                if (instanceObject != null) {
                    String objectStatus =
                        instanceObject.getStringValue(ReplicationInstanceClassInitializer.FIELD_STATUS);

                    if (status.equals(objectStatus)) {
                        ReplicationInstance instance = toReplicationInstance(instanceObject);

                        instances.add(instance);
                    }
                }
            }

            return instances;
        });
    }

    private ReplicationInstance toReplicationInstance(BaseObject instanceObject)
    {

    }

    private void setReplicationInstance(ReplicationInstance instance, BaseObject instanceObject)
    {

    }

    public List<ReplicationInstance> loadInstances() throws ReplicationException
    {
        return loadInstances(ReplicationInstanceStatus.REGISTERED.toString());
    }

    public void saveInstance(ReplicationInstance instance) throws ReplicationException
    {
        executeInstanceDocument(instancesDocument -> {

            return null;
        });
    }

    public void deleteInstance(ReplicationInstance instance) throws ReplicationException
    {
        executeInstanceDocument(instancesDocument -> {

            return null;
        });
    }

    public List<ReplicationInstance> loadRequestingInstances() throws ReplicationException
    {
        return loadInstances(ReplicationInstanceStatus.REQUESTING.toString());
    }

    public void saveRequestingInstance(ReplicationInstance instance) throws ReplicationException
    {
        executeInstanceDocument(instancesDocument -> {

            return null;
        });
    }

    public void deleteRequestingInstance(ReplicationInstance instance) throws ReplicationException
    {
        executeInstanceDocument(instancesDocument -> {

            return null;
        });
    }

    public List<String> loadRequestedInstances() throws ReplicationException
    {
        return loadInstances(ReplicationInstanceStatus.REQUESTED.toString());
    }

    public void saveRequestedInstance(String instanceURL) throws ReplicationException
    {
        executeInstanceDocument(instancesDocument -> {

            return null;
        });
    }

    public void deleteRequestedInstance(String instanceURL) throws ReplicationException
    {
        executeInstanceDocument(instancesDocument -> {

            return null;
        });
    }
}
