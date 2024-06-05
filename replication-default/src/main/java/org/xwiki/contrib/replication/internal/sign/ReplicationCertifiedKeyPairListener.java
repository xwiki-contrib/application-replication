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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * A listener in charge of storing a key pair when another cluster member created one.
 * 
 * @version $Id$
 * @since 2.1.0
 */
@Component
@Named(ReplicationCertifiedKeyPairListener.NAME)
@Singleton
public class ReplicationCertifiedKeyPairListener extends AbstractEventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME = "org.xwiki.contrib.replication.internal.sign.CertifiedKeyPairListener";

    @Inject
    private RemoteObservationManagerContext remoteContext;

    @Inject
    private ObservationManager observation;

    @Inject
    private ReplicationCertifiedKeyPairStore store;

    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public ReplicationCertifiedKeyPairListener()
    {
        super(NAME, new ReplicationCertifiedKeyPairCreatedEvent(), new ReplicationCertifiedKeyPairRefreshEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.remoteContext.isRemoteState()) {
            String instance = ((AbstractReplicationCertifiedKeyPairEvent) event).getInstance();
            ReplicationCertifiedKeyPair remoteKeyPair = (ReplicationCertifiedKeyPair) source;
            ReplicationCertifiedKeyPair currentKeyPair = getCurrentKeyPair(instance);

            if (!ReplicationCertifiedKeyPair.samePublicKey(remoteKeyPair, currentKeyPair)) {
                if (remoteKeyPair != null && (currentKeyPair == null || currentKeyPair.before(remoteKeyPair))) {
                    // There is no key in this cluster member, or the key is older: update it
                    updateCurrentKeyPair(instance, remoteKeyPair);
                } else if (currentKeyPair != null && (remoteKeyPair == null || currentKeyPair.after(remoteKeyPair))) {
                    // The key in the current cluster member is more recent: fix the remote one

                    // We have to switch the state of the remote event context to indicate this is a local event and not
                    // a remote one
                    this.remoteContext.popRemoteState();
                    try {
                        this.observation.notify(new ReplicationCertifiedKeyPairRefreshEvent(instance), currentKeyPair);
                    } finally {
                        // Put back remote state
                        this.remoteContext.pushRemoteState();
                    }
                }
            }
        }
    }

    private ReplicationCertifiedKeyPair getCurrentKeyPair(String instance)
    {
        try {
            return this.store.getCertifiedKeyPair(instance, false);
        } catch (ReplicationException e) {
            this.logger.error("Failed to load the current key pair for instance [{}]", instance, e);
        }

        return null;
    }

    private void updateCurrentKeyPair(String instance, ReplicationCertifiedKeyPair remoteKeyPair)
    {
        try {
            this.store.storeCertifiedKeyPair(instance, remoteKeyPair.getKey());
        } catch (KeyStoreException e) {
            this.logger.error("Failed to store the key pair for instance [{}]", instance, e);
        }
    }
}
