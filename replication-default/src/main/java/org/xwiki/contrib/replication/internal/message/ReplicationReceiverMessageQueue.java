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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiver;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.event.ReplicationMessageHandlingEvent;
import org.xwiki.contrib.replication.internal.DefaultReplicationContext;
import org.xwiki.contrib.replication.internal.ReplicationClient;
import org.xwiki.contrib.replication.internal.message.log.ReplicationMessageLogStore;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.observation.ObservationManager;

/**
 * Maintain a queue of replication data to give to the various receivers.
 * 
 * @version $Id$
 */
@Component(roles = ReplicationReceiverMessageQueue.class)
@Singleton
public class ReplicationReceiverMessageQueue extends AbstractReplicationMessageQueue<ReplicationReceiverMessage>
    implements Initializable
{
    @Inject
    private ReplicationReceiverMessageStore store;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private ReplicationContext replicationContext;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private ReplicationClient client;

    @Inject
    private ObservationManager observation;

    @Inject
    private ReplicationMessageLogStore logStore;

    @Override
    public void initialize() throws InitializationException
    {
        // Initialize handling thread
        initializeQueue();

        // Load the queue from disk
        Queue<ReplicationReceiverMessage> messages = this.store.load();
        messages.forEach(this.queue::add);

        // Notify the other instances that we are ready to receive messages
        try {
            for (ReplicationInstance instance : this.instances.getRegisteredInstances()) {
                try {
                    this.client.ping(instance);
                } catch (Exception e) {
                    this.logger.warn("Failed to send a ping to instance [{}]: {}", instance.getURI(),
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
        } catch (ReplicationException e) {
            throw new InitializationException("Failed to get registered istances", e);
        }
    }

    @Override
    protected String getThreadName()
    {
        return "Replication receiver";
    }

    @Override
    protected void handle(ReplicationReceiverMessage message) throws Exception
    {
        // Make sure an ExecutionContext is available
        this.executionContextManager.pushContext(new ExecutionContext(), false);

        try {
            // Find the receiver corresponding to the type
            ReplicationReceiver replicationReceiver =
                this.componentManager.getInstance(ReplicationReceiver.class, message.getType());

            // Notify that a message is about to be handled by a receiver
            ReplicationMessageHandlingEvent event = new ReplicationMessageHandlingEvent();
            this.observation.notify(event, message);
            if (event.isCanceled()) {
                this.logger.warn("The message with id [{}] and coming from [{}] was ignored: {}", message.getId(),
                    message.getSource(), event.getReason());

                return;
            }

            // Indicate in the context that this is a replication change
            ((DefaultReplicationContext) this.replicationContext).setReplicationMessage(message);

            // Relay the message and wait until it's stored
            replicationReceiver.relay(message).get();

            // Execute the receiver if the current instance is supposed to
            ReplicationInstance currentInstance = this.instances.getCurrentInstance();
            if (message.getReceivers() == null || message.getReceivers().contains(currentInstance.getURI())) {
                replicationReceiver.receive(message);

                // Log the successfully handled message
                this.logStore.saveAsync(message, (m, e) -> {
                    Map<String, Object> custom = new HashMap<>(e.getCustom());

                    custom.put(ReplicationMessageEventQuery.KEY_STATUS,
                        ReplicationMessageEventQuery.VALUE_STATUS_HANDLED);

                    e.setCustom(custom);
                });
            }
        } catch (InvalidReplicationMessageException e) {
            this.logger.error("Message with id [{}] is invalid and is not going to be tried again", message.getId(), e);
        } finally {
            this.executionContextManager.popContext();
        }

        // Delete it from disk
        this.store.delete(message);
    }

    @Override
    protected void removeFromStore(ReplicationReceiverMessage message) throws ReplicationException
    {
        this.store.delete(message);
    }

    /**
     * @param message the message to store and add to the queue
     */
    public void add(ReplicationReceiverMessage message) throws ReplicationException
    {
        // Serialize the data
        ReplicationReceiverMessage storedMessage;
        try {
            storedMessage = this.store.store(message);
        } catch (Exception e) {
            throw new ReplicationException("Failed to store received message with id [" + message.getId() + "]", e);
        }

        // Add the data to the queue
        this.queue.add(storedMessage);
    }

    /**
     * @param maxDate the maximum date to take into account
     * @return the date of the most recent know message
     * @since 1.1
     */
    public Date getLastMessageBefore(Date maxDate)
    {
        Date date = null;

        for (ReplicationReceiverMessage message : this.queue) {
            if (date == null || date.after(message.getDate())) {
                date = message.getDate();
            }
        }

        return date;
    }
}
