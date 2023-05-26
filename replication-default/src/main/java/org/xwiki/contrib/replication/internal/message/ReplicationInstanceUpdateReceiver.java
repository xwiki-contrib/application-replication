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
package org.xwiki.contrib.replication.internal.message;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.AbstractReplicationReceiver;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.instance.DefaultReplicationInstance;
import org.xwiki.contrib.replication.internal.instance.ReplicationInstanceStore;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(ReplicationInstanceUpdateMessage.TYPE)
public class ReplicationInstanceUpdateReceiver extends AbstractReplicationReceiver
{
    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private ReplicationInstanceStore store;

    @Override
    public void receive(ReplicationReceiverMessage message) throws ReplicationException
    {
        ReplicationInstance instance = this.instances.getInstanceByURI(message.getSource());

        String uri = message.getSource();
        Status status;
        CertifiedPublicKey publicKey;
        Map<String, Object> properties;
        if (instance != null) {
            // It's a known instance
            status = instance.getStatus();
            publicKey = instance.getReceiveKey();
            properties = new HashMap<>(instance.getProperties());
        } else {
            // It's a new relayed instance
            status = Status.RELAYED;
            publicKey = null;
            properties = new HashMap<>();
        }

        // Instance name
        String name = this.messageReader.getMetadata(message, ReplicationInstanceUpdateMessage.METADATA_NAME, false);

        // Instance custom properties
        for (Map.Entry<String, Collection<String>> entry : message.getCustomMetadata().entrySet()) {
            if (entry.getKey().startsWith(ReplicationInstanceUpdateMessage.PREFIX_METADATE_CUSTOM)) {
                String propertyKey =
                    entry.getKey().substring(ReplicationInstanceUpdateMessage.PREFIX_METADATE_CUSTOM.length());
                properties.put(propertyKey.toLowerCase(), entry.getValue());
            }
        }

        // Save the changes
        ReplicationInstance newInstance = new DefaultReplicationInstance(name, uri, status, publicKey, properties);
        if (instance != null) {
            try {
                this.store.updateInstance(newInstance);
            } catch (ReplicationException e) {
                throw new ReplicationException("Failed to update the replication instance [" + instance + "]", e);
            }
        } else {
            try {
                this.store.addInstance(newInstance);
            } catch (ReplicationException e) {
                throw new ReplicationException("Failed to add the replication instance [" + instance + "]", e);
            }
        }
    }
}
