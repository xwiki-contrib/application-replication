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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.event.ReplicationMessageSendingEvent;
import org.xwiki.contrib.replication.internal.ReplicationClient;
import org.xwiki.contrib.replication.internal.message.log.ReplicationMessageLogStore;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.observation.ObservationManager;

/**
 * Maintain a queue of replication data to send to a specific instance.
 * 
 * @version $Id$
 */
@Component(roles = ReplicationSenderMessageQueue.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ReplicationSenderMessageQueue extends AbstractReplicationMessageQueue<ReplicationSenderMessage>
{
    @Inject
    private ReplicationSenderMessageStore store;

    @Inject
    private ReplicationClient client;

    @Inject
    private ObservationManager observation;

    @Inject
    private ReplicationMessageLogStore logStore;

    /**
     * Used to wait for a ping or a timeout.
     */
    private final ReentrantLock pingLock = new ReentrantLock();

    /**
     * Condition for waiting answer.
     */
    private final Condition pingCondition = this.pingLock.newCondition();

    private ReplicationInstance instance;

    private int wait;

    private Throwable lastError;

    private Date nextTry;

    /**
     * @return the instance to send messages to
     */
    public ReplicationInstance getInstance()
    {
        return this.instance;
    }

    /**
     * @param instance the instance to send messages to
     */
    public void start(ReplicationInstance instance)
    {
        this.instance = instance;

        initializeQueue();
    }

    @Override
    protected String getThreadName()
    {
        return "Replication message sending to [" + instance.getURI() + "]";
    }

    @Override
    protected void handle(ReplicationSenderMessage message) throws InterruptedException
    {
        // Notify that a message is about to be sent
        ReplicationMessageSendingEvent event = new ReplicationMessageSendingEvent();
        this.observation.notify(event, message, this.instance);
        if (event.isCanceled()) {
            this.logger.warn("The sending of the message with id [{}] was cancelled: {}", message.getId(),
                event.getReason());

            return;
        }

        // Try to send the message until it works
        while (true) {
            try {
                // Send the data to the instance
                this.client.sendMessage(message, this.instance);

                // Log the successfully sent message
                this.logStore.saveAsync(message, (m, e) -> {
                    Map<String, Object> custom = new HashMap<>(e.getCustom());

                    // Generate a new id to avoid overwriting the stored one
                    e.setId(UUID.randomUUID().toString());

                    // Make the event date be the sent date
                    e.setDate(new Date());

                    custom.put(ReplicationMessageEventQuery.KEY_STATUS, ReplicationMessageEventQuery.VALUE_STATUS_SENT);
                    custom.put(ReplicationMessageEventQuery.KEY_TARGET, this.instance.getURI());

                    e.setCustom(custom);
                });

                // Stop the loop
                break;
            } catch (Exception e) {
                // Remember the last error
                this.lastError = e;

                // Wait before trying to send the message again

                if (this.wait < 60) {
                    // Double the wait
                    // Start waiting at 1 minute
                    this.wait = this.wait == 0 ? 1 : this.wait * 2;
                } else {
                    // Wait a maximum of 2h (120 min)
                    this.wait = 120;
                }

                // Calculate next try date
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, this.wait);
                this.nextTry = calendar.getTime();

                this.logger.warn("Failed to send relication message to instance [{}], retrying in [{}] minutes: {}",
                    this.instance.getURI(), this.wait, ExceptionUtils.getRootCauseMessage(e));

                // Wait
                this.pingLock.lockInterruptibly();
                try {
                    this.pingCondition.awaitUntil(this.nextTry);
                } finally {
                    this.pingLock.unlock();
                }
            }
        }

        // Reset the wait
        this.wait = 0;
        this.nextTry = null;
        // Reset the last error
        this.lastError = null;
    }

    /**
     * @return the last error catched when trying to send a message
     */
    public Throwable getLastError()
    {
        return this.lastError;
    }

    /**
     * @return the date when to try again the last failing message
     */
    public Date getNextTry()
    {
        return this.nextTry;
    }

    @Override
    protected void removeFromStore(ReplicationSenderMessage message) throws ReplicationException
    {
        this.store.removeTarget(message, this.instance);
    }

    /**
     * @param message the message to send
     */
    public void add(ReplicationSenderMessage message)
    {
        this.queue.add(message);
    }

    /**
     * Force the queue to resume sending messages.
     */
    public void wakeUp()
    {
        this.pingLock.lock();

        try {
            // Reset the wait
            this.wait = 0;
            this.nextTry = null;

            // Wake up the queue
            this.pingCondition.signal();
        } finally {
            this.pingLock.unlock();
        }
    }
}
