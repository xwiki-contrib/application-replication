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
import org.xwiki.contrib.replication.internal.message.ReplicationReceiverMessageQueue;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

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
    private Provider<ReplicationInstanceManager> instances;

    @Inject
    private Provider<ReplicationSender> senderProvider;

    @Inject
    private Provider<ReplicationReceiverMessageQueue> receiverProvider;

    @Inject
    private ObservationManager observation;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ReplicationInstanceListener()
    {
        super(NAME, new ApplicationReadyEvent(),
            BaseObjectReference.anyEvents(ReplicationInstanceClassInitializer.CLASS_FULLNAME));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ApplicationReadyEvent) {
            initialize();
        } else if (event instanceof XObjectEvent) {
            reload();

            XWikiDocument document = (XWikiDocument) source;
            EntityReference objectReference = ((XObjectEvent) event).getReference();

            // Check if an instance has been unregistered
            ReplicationInstance oldInstance = handleOldInstance(document, objectReference);

            // Check if an instance has been registered
            handleNewInstance(document, objectReference, oldInstance);
        }
    }

    private ReplicationInstance handleOldInstance(XWikiDocument document, EntityReference objectReference)
    {
        XWikiDocument documentOld = document.getOriginalDocument();

        ReplicationInstanceStore store = this.storeProvider.get();

        BaseObject xobjectOld = documentOld.getXObject(objectReference);

        if (xobjectOld != null) {
            Status statusOld = store.getStatus(xobjectOld);

            if (statusOld == Status.REGISTERED) {
                String uriOld = store.getURI(xobjectOld);
                ReplicationInstance oldInstance = this.instances.get().getInstance(uriOld);
                if (oldInstance == null || oldInstance.getStatus() != Status.REGISTERED) {
                    this.observation.notify(new ReplicationInstanceUnregisteredEvent(uriOld),
                        store.toReplicationInstance(xobjectOld));
                }

                return oldInstance;
            }

        }

        return null;
    }

    private void handleNewInstance(XWikiDocument document, EntityReference objectReference,
        ReplicationInstance oldInstance)
    {
        ReplicationInstanceStore store = this.storeProvider.get();

        BaseObject xobjectNew = document.getXObject(objectReference);

        if (xobjectNew != null) {
            String uriNew = store.getURI(xobjectNew);

            ReplicationInstance instanceNew = this.instances.get().getInstance(uriNew);

            if (instanceNew != null && instanceNew.getStatus() == Status.REGISTERED
                && (oldInstance == null || !oldInstance.getURI().equals(uriNew))) {
                this.observation.notify(new ReplicationInstanceRegisteredEvent(uriNew), instanceNew);
            }
        }
    }

    private void reload()
    {
        try {
            this.instances.get().reload();
        } catch (ReplicationException e) {
            this.logger.error("Failed to reload stored instances", e);
        }
    }

    private void initialize()
    {
        // Load instances
        reload();

        // Initialize the sender
        this.senderProvider.get();

        // Initialize the receiver
        this.receiverProvider.get();
    }
}
