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
package org.xwiki.contrib.replication.entity.internal.like;

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
import org.xwiki.contrib.replication.entity.internal.AbstractEntityReplicationInstanceRecoverHandler;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.like.LikeException;
import org.xwiki.like.LikeManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.user.UserReference;

/**
 * Replay all the like messages if any is in the indicated range (including more recent messages).
 * 
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named(LikeReplicationMessage.TYPE_LIKE)
public class LikeRecoverHandler extends AbstractEntityReplicationInstanceRecoverHandler
{
    private static final String EVENT_FIELD_METADATA_CREATOR =
        ReplicationMessageEventQuery.customMetadataName(LikeReplicationMessage.METADATA_ENTITY_CREATOR);

    @Inject
    private EventStore eventStore;

    @Inject
    private ConverterManager converter;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private LikeMessageSender likeSender;

    @Inject
    private LikeManager likes;

    @Override
    public void receive(Date dateMin, Date dateMax, ReplicationReceiverMessage message) throws ReplicationException
    {
        ReplicationInstance sourceInstance = this.instances.getInstanceByURI(message.getSource());

        // Taking care of this only in direct linked instances is enough
        if (sourceInstance == null) {
            return;
        }

        ReplicationMessageEventQuery query = new ReplicationMessageEventQuery();

        // Get all message related to likes
        query.eq(Event.FIELD_TYPE, ReplicationMessageEventQuery.messageTypeValue(LikeReplicationMessage.TYPE_LIKE));

        // And only the stored and received ones
        query.custom().in(ReplicationMessageEventQuery.KEY_STATUS, ReplicationMessageEventQuery.VALUE_STATUS_STORED,
            ReplicationMessageEventQuery.VALUE_STATUS_RECEIVED);

        // Minimum date
        query.after(dateMin);
        query.before(dateMax);

        // Sort by document reference and user
        query.custom().addSort(EVENT_FIELD_METADATA_REFERENCE, Order.ASC);
        query.custom().addSort(EVENT_FIELD_METADATA_CREATOR, Order.ASC);
        // And by date
        query.addSort(Event.FIELD_DATE, Order.ASC);

        // Search with only the needed field in the result
        // TODO: reduce the number of results with field collapsing when support for it is added to the event store API
        // TODO: reduce the field fetched when support for custom fields is added
        try (EventSearchResult result = this.eventStore.search(query)) {
            handle(result, message.getSource());
        } catch (Exception e) {
            throw new ReplicationException("Failed to request messages log", e);
        }
    }

    private void handle(EventSearchResult result, String source) throws ReplicationException, LikeException
    {
        String currentDocumentReferenceString = null;
        String currentUserReferenceString = null;
        EntityReference entityReference = null;
        UserReference userReference = null;
        for (Event event : (Iterable<Event>) result.stream()::iterator) {
            String documentReferenceString = getCustomMetadata(event, EVENT_FIELD_METADATA_REFERENCE);
            String userReferenceString = getCustomMetadata(event, EVENT_FIELD_METADATA_CREATOR);

            if (StringUtils.equals(documentReferenceString, currentDocumentReferenceString)) {
                if (StringUtils.equals(userReferenceString, currentUserReferenceString)) {
                    // This user was already handled for this document
                    continue;
                }
            } else {
                // New document
                entityReference =
                    this.converter.<EntityReference>convert(EntityReference.class, documentReferenceString);
            }

            userReference = this.converter.<UserReference>convert(UserReference.class, userReferenceString);

            currentDocumentReferenceString = documentReferenceString;
            currentUserReferenceString = userReferenceString;

            // If any message was "lost" make sure to replicate the current status of the document locale
            this.likeSender.send(userReference, entityReference, this.likes.isLiked(userReference, entityReference),
                List.of(source));
        }
    }
}
