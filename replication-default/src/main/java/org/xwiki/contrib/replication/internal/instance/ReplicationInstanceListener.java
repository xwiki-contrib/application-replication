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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.event.ReplicationInstanceRegisteredEvent;
import org.xwiki.contrib.replication.event.ReplicationInstanceUnregisteredEvent;
import org.xwiki.contrib.replication.internal.message.ReplicationInstanceMessageSender;
import org.xwiki.contrib.replication.internal.message.ReplicationReceiverMessageQueue;
import org.xwiki.contrib.replication.internal.sign.ReplicationCertifiedKeyPairStore;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * @version $Id$
 */
@Component
@Named(ReplicationInstanceListener.NAME)
@Singleton
public class ReplicationInstanceListener extends AbstractEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "ReplicationInstanceListener";

    @Inject
    private Provider<ReplicationInstanceStore> storeProvider;

    @Inject
    private Provider<ReplicationInstanceManager> instanceProvider;

    @Inject
    private Provider<ReplicationSender> senderProvider;

    @Inject
    private Provider<ReplicationInstanceMessageSender> instanceSenderProvider;

    @Inject
    private Provider<ReplicationReceiverMessageQueue> receiverProvider;

    @Inject
    private ObservationManager observation;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private RemoteObservationManagerContext remoteContext;

    @Inject
    private ReplicationCertifiedKeyPairStore keyStore;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ReplicationInstanceListener()
    {
        super(NAME, new ApplicationReadyEvent(),
            BaseObjectReference.anyEvents(StandardReplicationInstanceClassInitializer.CLASS_FULLNAME));
    }

    private void forceResetDocumentCache(Event event, Object source, Object data)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            XWikiStoreInterface store = xcontext.getWiki().getStore();
            if (store instanceof XWikiCacheStore) {
                ((XWikiCacheStore) store).onEvent(event, source, data);
            }
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ApplicationReadyEvent) {
            initialize();
        } else if (event instanceof XObjectEvent) {
            // Workaround https://jira.xwiki.org/browse/XWIKI-20564
            // Make sure the document in the cache is the right one in case of remote events
            if (this.remoteContext.isRemoteState()) {
                forceResetDocumentCache(event, source, data);
            }

            ReplicationInstanceManager instancesManager = this.instanceProvider.get();

            try {
                instancesManager.reload();
            } catch (ReplicationException e) {
                this.logger.error("Failed to reload stored instances", e);
            }

            XWikiDocument document = (XWikiDocument) source;
            EntityReference objectReference = ((XObjectEvent) event).getReference();

            ReplicationInstance oldInstance = getInstance(document.getOriginalDocument(), objectReference);
            ReplicationInstance newInstance = getInstance(document, objectReference);

            // Check if an instance has been unregistered
            checkUnregistered(oldInstance, newInstance);

            // Check if an instance has been registered
            checkRegistered(oldInstance, newInstance);

            // Check if current instance has been modified
            if (event instanceof XObjectUpdatedEvent) {
                checCurrentUpdated(oldInstance, newInstance);
            }
        }
    }

    private void checkUnregistered(ReplicationInstance oldInstance, ReplicationInstance newInstance)
    {
        if (oldInstance != null && oldInstance.getStatus() == Status.REGISTERED
            && (newInstance == null || newInstance.getStatus() != Status.REGISTERED)) {
            this.observation.notify(new ReplicationInstanceUnregisteredEvent(oldInstance.getURI()), oldInstance);
        }
    }

    private void checkRegistered(ReplicationInstance oldInstance, ReplicationInstance newInstance)
    {
        if (newInstance != null && newInstance.getStatus() == Status.REGISTERED
            && (oldInstance == null || oldInstance.getStatus() != Status.REGISTERED)) {
            this.observation.notify(new ReplicationInstanceRegisteredEvent(newInstance.getURI()), newInstance);
        }
    }

    private void checCurrentUpdated(ReplicationInstance oldInstance, ReplicationInstance newInstance)
    {
        if (oldInstance.getStatus() == null && newInstance.getStatus() == null) {
            try {
                this.instanceSenderProvider.get().updateCurrentInstance();
            } catch (Exception e) {
                this.logger.error("Failed to send update message for current instance", e);
            }
        }
    }

    private ReplicationInstance getInstance(XWikiDocument document, EntityReference objectReference)
    {
        ReplicationInstanceStore store = this.storeProvider.get();

        BaseObject xobjectOld = document.getXObject(objectReference);

        return xobjectOld != null ? store.toReplicationInstance(xobjectOld) : null;
    }

    private void initialize()
    {
        // Refresh the keys on cluster members
        try {
            for (ReplicationInstance instance : this.instanceProvider.get().getRegisteredInstances()) {
                try {
                    this.keyStore.refresh(instance);
                } catch (ReplicationException e) {
                    this.logger.error("Failed to refresh the key for instance [{}]", instance, e);
                }
            }
        } catch (ReplicationException e) {
            this.logger.error("Failed to get the registered instances", e);
        }

        // Initialize the sender
        this.senderProvider.get();

        // Initialize the receiver
        this.receiverProvider.get();
    }
}
