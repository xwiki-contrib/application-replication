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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.internal.ReplicationClient;

/**
 * Maintain a queue of replication data to send to a specific instance.
 * 
 * @version $Id$
 */
@Component(roles = ReplicationSenderMessageQueue.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ReplicationSenderMessageQueue implements Disposable, Runnable
{
    @Inject
    private ReplicationSenderMessageStore store;

    @Inject
    private ReplicationClient client;

    @Inject
    private Logger logger;

    private final BlockingQueue<ReplicationSenderMessage> queue = new LinkedBlockingQueue<>(1000);

    private boolean disposed;

    private Thread thread;

    private ReplicationInstance instance;

    private long wait;

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

        // Create a thread in charge of dispatching data to other instances
        this.thread = new Thread(this);
        this.thread.setName("Replication message sending to [" + instance.getURI() + "]");
        this.thread.setPriority(Thread.NORM_PRIORITY - 2);
        // That thread can be stopped any time without really loosing anything
        this.thread.setDaemon(true);
        this.thread.start();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.disposed = true;
    }

    @Override
    public void run()
    {
        while (!this.disposed) {
            ReplicationSenderMessage entry;
            try {
                entry = this.queue.take();

                syncSend(entry);
            } catch (InterruptedException e) {
                this.logger.warn("The replication sending thread has been interrupted");

                // Mark the thread as interrupted
                this.thread.interrupt();

                // Stop the loop
                break;
            }
        }
    }

    private void syncSend(ReplicationSenderMessage message) throws InterruptedException
    {
        // Try to send the message until it works
        while (true) {
            // Send the data to the instance
            try {
                this.client.sendMessage(message, this.instance);

                // Stop the loop
                break;
            } catch (Exception e) {
                // Wait before trying to send the message again

                // Wait a maximum of 2h (120 min)
                if (this.wait <= 60) {
                    // Increment the wait
                    // Start waiting at 1 minute
                    this.wait = this.wait == 0 ? 1 : this.wait * 2;
                } else {
                    this.wait = 120;
                }

                this.logger.warn("Failed to send relication message to instance [{}], retrying in [{}] minutes: {}",
                    this.instance.getURI(), this.wait, ExceptionUtils.getRootCauseMessage(e));

                // Wait
                Thread.sleep(this.wait * 60 * 1000);
            }
        }

        // Reset the wait
        this.wait = 0;

        // Remove this target (and data if it's the last target) from disk
        // TODO: put the remove in an async queue
        try {
            this.store.removeTarget(message, this.instance);
        } catch (ReplicationException e) {
            this.logger.error("Failed to remove sent message with id [{}] for target instance [{}]", message.getId(),
                this.instance.getURI(), e);
        }
    }

    /**
     * @param message the message to send
     */
    public void add(ReplicationSenderMessage message)
    {
        this.queue.add(message);
    }
}
