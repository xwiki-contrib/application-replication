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
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * A listener in charge of storing a key pair when another cluster member created one.
 * 
 * @version $Id$
 * @since 1.4.0
 */
@Component
@Named(CertifiedKeyPairListener.NAME)
@Singleton
public class CertifiedKeyPairListener extends AbstractEventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME = "org.xwiki.contrib.replication.internal.sign.CertifiedKeyPairListener";

    @Inject
    private RemoteObservationManagerContext remoteContext;

    @Inject
    private CertifiedKeyPairStore store;

    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public CertifiedKeyPairListener()
    {
        super(NAME, new CertifiedKeyPairCreatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.remoteContext.isRemoteState()) {
            String instance = ((CertifiedKeyPairCreatedEvent) event).getInstance();
            CertifiedKeyPair keyPair = (CertifiedKeyPair) source;

            try {
                this.store.storeCertifiedKeyPair(instance, keyPair);
            } catch (KeyStoreException e) {
                this.logger.error("Failed to store the key pair for instance [{}]", instance, e);
            }
        }
    }
}
