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
package org.xwiki.contrib.replication.entity.internal;

import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.contrib.replication.entity.EntityReplicationBuilders;
import org.xwiki.contrib.replication.entity.EntityReplicationMessage;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.ConverterManager;

/**
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named(EntityReplicationMessage.VALUE_DOCUMENT_RECOVER_TYPE)
public class DocumentReplicationInstanceRecoverHandler extends AbstractEntityReplicationInstanceRecoverHandler
{
    private static final String EVENT_FIELD_METADATA_LOCALE =
        ReplicationMessageEventQuery.customMetadataName(EntityReplicationMessage.METADATA_ENTITY_LOCALE);

    @Inject
    private EventStore eventStore;

    @Inject
    private ConverterManager converter;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private DocumentReplicationController controller;

    @Inject
    private EntityReplicationBuilders builders;

    @Inject
    private EntityReplication entityReplication;

    @Override
    public void receive(Date dateMin, Date dateMax, ReplicationReceiverMessage message) throws ReplicationException
    {
        ReplicationMessageEventQuery query = new ReplicationMessageEventQuery();

        // Get only messages related to document updates
        query.customMetadata().eq(EVENT_FIELD_METADATA_RECOVER_TYPE,
            EntityReplicationMessage.VALUE_DOCUMENT_RECOVER_TYPE);

        // And only the stored and received ones
        query.custom().in(ReplicationMessageEventQuery.KEY_STATUS, ReplicationMessageEventQuery.VALUE_STATUS_STORED,
            ReplicationMessageEventQuery.VALUE_STATUS_RECEIVED);

        // Minimum date
        query.after(dateMin);
        query.before(dateMax);

        // Sort by document reference
        query.custom().addSort(EVENT_FIELD_METADATA_REFERENCE, Order.ASC);
        // Then by locale
        query.custom().addSort(EVENT_FIELD_METADATA_LOCALE, Order.ASC);
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

    private void handle(EventSearchResult result, String source) throws ReplicationException
    {
        ReplicationInstance currentInstance = this.instances.getCurrentInstance();
        ReplicationInstance sourceInstance = this.instances.getInstanceByURI(source);

        String currentDocumentReferenceString = null;
        String currentLocaleString = null;
        boolean skipCurrentDocument = false;
        DocumentReference documentReference = null;
        for (Event event : (Iterable<Event>) result.stream()::iterator) {
            String documentReferenceString = getCustomMetadata(event, EVENT_FIELD_METADATA_REFERENCE);
            String documentLocaleString = getCustomMetadata(event, EVENT_FIELD_METADATA_LOCALE);
            if (StringUtils.equals(documentReferenceString, currentDocumentReferenceString)) {
                // Same document
                if (skipCurrentDocument) {
                    // A previous run asked to skip until we hit a different document
                    continue;
                }

                if (StringUtils.equals(documentLocaleString, currentLocaleString)) {
                    // This document locale was already handled
                    continue;
                }

                documentReference = new DocumentReference(documentReference,
                    this.converter.<Locale>convert(Locale.class, documentLocaleString));
            } else {
                // New document
                documentReference = new DocumentReference(
                    this.converter.<EntityReference>convert(EntityReference.class, documentReferenceString),
                    this.converter.<Locale>convert(Locale.class, documentLocaleString));

                // Check if the instance is allowed to take care of this document recovery
                if (!shouldHandle(documentReference, currentInstance, source, sourceInstance)) {
                    skipCurrentDocument = true;

                    continue;
                }

                skipCurrentDocument = false;
            }

            currentDocumentReferenceString = documentReferenceString;
            currentLocaleString = documentLocaleString;

            // If any message was "lost" make sure to replicate the current status of the document locale
            this.controller.send(this.builders.documentMessageBuilder(documentReference).receivers(source));
        }
    }

    protected boolean shouldHandle(DocumentReference documentReference, ReplicationInstance currentInstance,
        String source, ReplicationInstance sourceInstance) throws ReplicationException
    {
        String ownerURI = this.entityReplication.getOwner(documentReference);

        // Check if the instance should take care of this document:
        // * there is no document owner
        // * or current instance is the owner of the document
        // * or the owner of the document is the requesting instance and current instance is directly linked with it
        return ownerURI == null || currentInstance.getURI().equals(ownerURI)
            || (source.equals(ownerURI) && sourceInstance != null);
    }
}
