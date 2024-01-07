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
package org.xwiki.contrib.replication.entity.internal.message;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.internal.AbstractEntityReplicationInstanceRecoverHandler;
import org.xwiki.contrib.replication.entity.internal.EntityReplicationStore;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.ConverterManager;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named(EntityReplicationControllerMessage.TYPE)
public class EntityReplicationControllerRecoverHandler extends AbstractEntityReplicationInstanceRecoverHandler
{
    @Inject
    private EventStore eventStore;

    @Inject
    private ConverterManager converter;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private EntityReplicationStore replicationStore;

    @Inject
    private EntityReplicationControllerSender sender;

    @Override
    public int getPriority()
    {
        // Replication configuration needs to be updated first as otherwise the other instance may refuse the document
        // update messages
        return DEFAULT_PRIORITY / 2;
    }

    @Override
    public void receive(Date dateMin, Date dateMax, ReplicationReceiverMessage message) throws ReplicationException
    {
        ReplicationMessageEventQuery query = new ReplicationMessageEventQuery();

        // Get only the messages related to replication configuration
        query.eq(Event.FIELD_TYPE,
            ReplicationMessageEventQuery.messageTypeValue(EntityReplicationControllerMessage.TYPE));

        // And only the stored and received ones
        query.custom().in(ReplicationMessageEventQuery.KEY_STATUS, ReplicationMessageEventQuery.VALUE_STATUS_STORED,
            ReplicationMessageEventQuery.VALUE_STATUS_RECEIVED);

        // Minimum date
        query.after(dateMin);
        query.before(dateMax);

        // Sort by document reference
        query.custom().addSort(EVENT_FIELD_METADATA_REFERENCE, Order.ASC);
        // And by date
        query.addSort(Event.FIELD_DATE, Order.DESC);

        // Search with only the needed field in the result
        // TODO: reduce the number of results with field collapsing when support for it is added to the event store API
        // TODO: reduce the field fetched when support for custom fields is added
        try (EventSearchResult result = this.eventStore.search(query)) {
            handle(result, message.getSource());
        } catch (Exception e) {
            throw new ReplicationException("Failed to request messages log", e);
        }
    }

    private void handle(EventSearchResult result, String source) throws XWikiException, ReplicationException
    {
        ReplicationInstance sourceInstance = this.instances.getRegisteredInstanceByURI(source);

        // Taking care of this only in direct linked instance is enough
        if (sourceInstance == null) {
            return;
        }

        // If any message was "lost" make sure to replicate the current configuration of the entity
        String currentEntityReferenceString = null;
        for (Event event : (Iterable<Event>) result.stream()::iterator) {
            String entityReferenceString = getCustomMetadata(event, EVENT_FIELD_METADATA_REFERENCE);
            if (!StringUtils.equals(entityReferenceString, currentEntityReferenceString)) {
                EntityReference entityReference =
                    this.converter.<EntityReference>convert(EntityReference.class, entityReferenceString);

                currentEntityReferenceString = entityReferenceString;

                // Get current configuration
                Collection<DocumentReplicationControllerInstance> configurations =
                    this.replicationStore.getHibernateEntityReplication(entityReference);

                // Send the configuration
                this.sender.send(entityReference, configurations, List.of(sourceInstance));
            }
        }
    }
}
