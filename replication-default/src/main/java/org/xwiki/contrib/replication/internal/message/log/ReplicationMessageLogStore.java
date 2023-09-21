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
package org.xwiki.contrib.replication.internal.message.log;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.internal.DefaultReplicationSenderMessage;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.contrib.replication.message.log.ReplicationMessageEventInitializer;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * @version $Id$
 */
@Component(roles = ReplicationMessageLogStore.class)
@Singleton
public class ReplicationMessageLogStore
{
    private static final DocumentReference SUPERADMIN =
        new DocumentReference("xwiki", "XWiki", XWikiRightService.SUPERADMIN_USER);

    @Inject
    private EventStore store;

    @Inject
    private EventFactory eventFactory;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    /**
     * @param messageId the identifier of the message
     * @return the first event corresponding to this message
     * @throws EventStreamException when failing to search for the message
     */
    public Optional<String> getEventId(String messageId) throws EventStreamException
    {
        SimpleEventQuery eventQuery = new SimpleEventQuery();
        // Check of an event exist with the same message id
        eventQuery.custom().eq(ReplicationMessageEventQuery.KEY_ID, messageId);
        // We don't need to actually get the result, we just want to know if some exist
        eventQuery.setLimit(1);

        try (EventSearchResult result = this.store.search(eventQuery, Set.of(Event.FIELD_ID))) {
            Optional<Event> event = result.stream().findFirst();

            return event.isPresent() ? Optional.of(event.get().getId()) : Optional.empty();
        } catch (Exception e) {
            throw new EventStreamException("Failed to close the search result", e);
        }
    }

    /**
     * @param messageId the identifier of the message
     * @return true if the message was found
     * @throws EventStreamException when failing to search for the message
     */
    public boolean exist(String messageId) throws EventStreamException
    {
        return getEventId(messageId).isPresent();
    }

    /**
     * @param message the message to save
     * @param initializer custom initializer
     * @return the stored {@link Event}
     * @throws EventStreamException when failing to save the message
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public Event saveSync(ReplicationMessage message, ReplicationMessageEventInitializer initializer)
        throws EventStreamException, InterruptedException
    {
        // Save the event synchronously
        try {
            return saveAsyncInternal(message, initializer).get();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e;
            if (e instanceof ExecutionException) {
                cause = e.getCause();

                if (cause instanceof EventStreamException) {
                    throw (EventStreamException) cause;
                }
            }

            throw new EventStreamException("Failed to save the message in the event store", cause);
        }
    }

    /**
     * @param message the message to save
     * @param initializer custom initializer
     * @return the new {@link CompletableFuture} providing the added {@link Event}
     */
    public CompletableFuture<Event> saveAsync(ReplicationMessage message,
        ReplicationMessageEventInitializer initializer)
    {
        CompletableFuture<Event> future = new CompletableFuture<>();

        // Make the full process asynchronous and not only storing the Event instance
        CompletableFuture.runAsync(() -> {
            this.execution.setContext(new ExecutionContext());

            try {
                future.complete(saveAsyncInternal(message, initializer).get());
            } catch (Exception e) {
                future.completeExceptionally(e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                logger.error("Failed to log the message with id {}", message.getId(), e);
            } finally {
                this.execution.removeContext();
            }
        });

        return future;
    }

    private CompletableFuture<Event> saveAsyncInternal(ReplicationMessage message,
        ReplicationMessageEventInitializer initializer)
    {
        Event event = this.eventFactory.createRawEvent();

        // We don't want this even to go through pre filtering so we mark it as done
        event.setPrefiltered(true);
        // We want to hide this event as much as possible
        event.setHidden(true);

        event.setUser(SUPERADMIN);
        event.setApplication(ReplicationMessageEventQuery.VALUE_APPLICATION);
        event.setImportance(Importance.BACKGROUND);

        event.setType(ReplicationMessageEventQuery.messageTypeValue(message.getType()));

        Map<String, Object> properties = new HashMap<>();

        // Standard metadata
        properties.put(ReplicationMessageEventQuery.KEY_ID, message.getId());
        properties.put(ReplicationMessageEventQuery.KEY_DATE, message.getDate());
        properties.put(ReplicationMessageEventQuery.KEY_SOURCE, message.getSource());
        if (message.getReceivers() != null) {
            properties.put(ReplicationMessageEventQuery.KEY_RECEIVERS, message.getReceivers());
        }
        properties.put(ReplicationMessageEventQuery.KEY_TYPE, message.getType());

        // Add custom metadata
        for (Map.Entry<String, Collection<String>> entry : message.getCustomMetadata().entrySet()) {
            properties.put(ReplicationMessageEventQuery.customMetadataName(entry.getKey()), entry.getValue());
        }

        event.setCustom(properties);

        // Call custom initializer
        if (initializer != null) {
            initializer.initialize(message, event);
        }

        // Call extended event initializers
        try {
            this.componentManager
                .<ReplicationMessageEventInitializer>getInstanceList(ReplicationMessageEventInitializer.class)
                .forEach(i -> i.initialize(message, event));
        } catch (Exception e) {
            this.logger.error("Failed to execute event initializers for message with id [{}]", message.getId(), e);
        }

        // Save the event asynchronously
        return this.store.saveEvent(event);
    }

    /**
     * @param messageId the identifier of the message to delete
     * @return the new {@link CompletableFuture} providing the deleted {@link Event} or empty if none could be found
     * @throws EventStreamException when failing to delete the event
     */
    public CompletableFuture<Optional<Event>> deleteAsync(String messageId) throws EventStreamException
    {
        Optional<String> eventId = getEventId(messageId);

        return eventId.isPresent() ? this.store.deleteEvent(eventId.get()) : null;
    }

    /**
     * @param id the identifier of the logged message
     * @return the message extracted from the log or null if none exist for this id
     * @throws EventStreamException when failing to load the event matching the id
     * @since 1.1
     */
    public ReplicationSenderMessage loadMessage(String id) throws EventStreamException
    {
        return loadMessage(id, null);
    }

    /**
     * @param id the identifier of the logged message
     * @param receivers the instances which should handle the message
     * @return the message extracted from the log or null if none exist for this id
     * @throws EventStreamException when failing to load the event matching the id
     * @since 1.3.0
     */
    public ReplicationSenderMessage loadMessage(String id, Collection<String> receivers) throws EventStreamException
    {
        Optional<Event> eventOptional = this.store.getEvent(id);

        if (eventOptional.isEmpty()) {
            return null;
        }

        Event event = eventOptional.get();

        String messageId = (String) event.getCustom().get(ReplicationMessageEventQuery.KEY_ID);
        Date messageDate = (Date) event.getCustom().get(ReplicationMessageEventQuery.KEY_DATE);
        String source = (String) event.getCustom().get(ReplicationMessageEventQuery.KEY_SOURCE);
        Collection<String> finalReceivers = receivers != null ? receivers
            : (Collection) event.getCustom().get(ReplicationMessageEventQuery.KEY_RECEIVERS);
        String type = (String) event.getCustom().get(ReplicationMessageEventQuery.KEY_TYPE);

        Map<String, Collection<String>> metadata = new HashMap<>(event.getCustom().size());
        for (Map.Entry<String, Object> entry : event.getCustom().entrySet()) {
            if (entry.getKey().startsWith(ReplicationMessageEventQuery.PREFIX_CUSTOM_METADATA)) {
                metadata.put(entry.getKey().substring(ReplicationMessageEventQuery.PREFIX_CUSTOM_METADATA.length()),
                    (Collection<String>) entry.getValue());
            }
        }

        return new DefaultReplicationSenderMessage(messageId, messageDate, type, source, finalReceivers, metadata,
            null);
    }

    /**
     * @param dateMax the maximum date to take into account
     * @return the date of the most recent know message
     * @throws Exception when failing to search for the last message date
     * @since 1.1
     */
    public Date getLastMessageBefore(Date dateMax) throws Exception
    {
        SimpleEventQuery query = new SimpleEventQuery();

        // Only events related to replication messages
        query.eq(Event.FIELD_APPLICATION, ReplicationMessageEventQuery.VALUE_APPLICATION);

        // Minimum date
        query.before(dateMax);

        // Sort by date
        query.addSort(Event.FIELD_DATE, Order.DESC);

        // We are only interested in the first result
        query.setLimit(1);

        // Search with only the needed field in the result
        // TODO: reduce the number of results with field collapsing when support for it is added to the event store API
        try (EventSearchResult result = this.store.search(query, Set.of(Event.FIELD_DATE))) {
            Optional<Event> firstMessage = result.stream().findFirst();
            if (firstMessage.isPresent()) {
                return firstMessage.get().getDate();
            }
        }

        return null;
    }
}
