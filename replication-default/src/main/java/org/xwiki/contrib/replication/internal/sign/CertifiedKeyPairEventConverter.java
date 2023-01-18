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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

/**
 * Convert all mail entity events to remote events and back to local events.
 *
 * @version $Id: c3a4828f079a4a41b66f1d29d54765d7407c1e5a $
 * @since 1.4.0
 */
@Component
@Singleton
@Named("replication.CertifiedKeyPair")
// TODO: move to XWiki Standard
public class CertifiedKeyPairEventConverter extends AbstractEventConverter
{
    private static final Set<Class<? extends Event>> EVENTS =
        new HashSet<>(Arrays.asList(CertifiedKeyPairCreatedEvent.class));

    private static final String PROP_PUBLIC = "public";

    private static final String PROP_PRIVATE = "private";

    @Inject
    private CryptTools cryptTools;

    @Inject
    private Logger logger;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (EVENTS.contains(localEvent.getEvent().getClass())) {
            try {
                remoteEvent.setEvent((Serializable) localEvent.getEvent());
                remoteEvent.setSource(serializeEntityEvent((CertifiedKeyPair) localEvent.getSource()));

                return true;
            } catch (Exception e) {
                this.logger.error("Failed to convert local event [{}]", localEvent, e);
            }
        }

        return false;
    }

    private Serializable serializeEntityEvent(CertifiedKeyPair keyPair) throws IOException
    {
        Serializable remote = null;

        if (keyPair != null) {
            Map<String, Object> map = new HashMap<>();

            if (keyPair.getCertificate() != null) {
                map.put(PROP_PUBLIC, this.cryptTools.serializePublicKey(keyPair.getCertificate()));
            }

            if (keyPair.getPrivateKey() != null) {
                map.put(PROP_PRIVATE, this.cryptTools.serializePrivateKey(keyPair.getPrivateKey()));
            }

            remote = (Serializable) map;
        }

        return remote;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (EVENTS.contains(remoteEvent.getEvent().getClass())) {
            try {
                localEvent.setEvent((Event) remoteEvent.getEvent());
                localEvent.setSource(unserializeEntityEvent(remoteEvent.getSource()));

                return true;
            } catch (Exception e) {
                this.logger.error("Failed to convert remote event [{}]", remoteEvent, e);
            }
        }

        return false;
    }

    private Object unserializeEntityEvent(Serializable remote) throws IOException
    {
        if (remote instanceof Map) {
            Map<String, ?> map = (Map<String, ?>) remote;

            CertifiedPublicKey publicKey = null;
            String publicKeyString = (String) map.get(PROP_PUBLIC);
            if (publicKeyString != null) {
                publicKey = this.cryptTools.unserializePublicKey(publicKeyString);
            }

            PrivateKeyParameters privateKey = null;
            byte[] privateKeyBytes = (byte[]) map.get(PROP_PRIVATE);
            if (privateKeyBytes != null) {
                privateKey = this.cryptTools.unserializePrivateKey(privateKeyBytes);
            }

            return new CertifiedKeyPair(privateKey, publicKey);
        }

        return null;
    }
}
