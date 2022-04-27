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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.contrib.replication.message.log.ReplicationMessageEventInitializer;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.user.api.XWikiRightService;

import groovyjarjarpicocli.CommandLine.ExecutionException;

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
     * @return true if the message was found
     * @throws EventStreamException when failing to search for the message
     */
    public boolean exist(String messageId) throws EventStreamException
    {
        return this.store.getEvent(messageId).isPresent();
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

        event.setId(message.getId());
        event.setUser(SUPERADMIN);
        event.setApplication(ReplicationMessageEventQuery.VALUE_APPLICATION);
        event.setImportance(Importance.BACKGROUND);

        event.setDate(message.getDate());
        event.setType("replication_message_" + message.getType());

        Map<String, Object> properties = new HashMap<>();

        // Standard metadata
        properties.put(ReplicationMessageEventQuery.KEY_ID, message.getId());
        properties.put(ReplicationMessageEventQuery.KEY_SOURCE, message.getSource());
        properties.put(ReplicationMessageEventQuery.KEY_TYPE, message.getType());

        // Add custom metadata
        for (Map.Entry<String, Collection<String>> entry : message.getCustomMetadata().entrySet()) {
            properties.put(ReplicationMessageEventQuery.PREFIX_CUSTOM_METADATA + entry.getKey(), entry.getValue());
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
     */
    public CompletableFuture<Optional<Event>> deleteAsync(String messageId)
    {
        return this.store.deleteEvent(messageId);
    }
}
