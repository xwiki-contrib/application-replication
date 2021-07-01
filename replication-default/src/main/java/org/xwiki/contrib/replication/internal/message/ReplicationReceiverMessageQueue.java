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

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiver;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.internal.DefaultReplicationContext;

/**
 * Maintain a queue of replication data to give to the various receivers.
 * 
 * @version $Id$
 */
@Component(roles = ReplicationReceiverMessageQueue.class)
@Singleton
public class ReplicationReceiverMessageQueue implements Initializable, Disposable, Runnable
{
    private boolean disposed;

    private Thread thread;

    private final BlockingQueue<ReplicationReceiverMessage> queue = new LinkedBlockingQueue<>(10000);

    @Inject
    private ReplicationReceiverMessageStore store;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private ReplicationContext replicationContext;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        // Initialize handling thread
        this.thread = new Thread(this);
        this.thread.setName("Replication receiver");
        this.thread.setPriority(Thread.NORM_PRIORITY - 1);
        this.thread.start();

        // Load the queue from disk
        Queue<ReplicationReceiverMessage> messages = this.store.load();
        messages.forEach(this.queue::add);
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
            ReplicationReceiverMessage data;
            try {
                data = this.queue.take();

                handle(data);
            } catch (ExecutionContextException e) {
                this.logger.error("Failed to initialize an ExecutionContext", e);
            } catch (InterruptedException e) {
                this.logger.warn("The replication sending thread has been interrupted");

                // Mark the thread as interrupted
                this.thread.interrupt();

                // Stop the loop
                break;
            } catch (Throwable t) {
                this.logger.error("An unexpected throwable was thrown while handling replication data", t);
            }
        }
    }

    private void handle(ReplicationReceiverMessage message)
        throws ComponentLookupException, ReplicationException, ExecutionContextException
    {
        // Find a the receiving corresponding to the type
        ReplicationReceiver replicationReceiver =
            this.componentManager.getInstance(ReplicationReceiver.class, message.getType());

        // Make sure an ExecutionContext is available
        this.executionContextManager.pushContext(new ExecutionContext(), false);

        // Indicate in the context that this is a replication change
        ((DefaultReplicationContext) this.replicationContext).setReplicationMessage(true);

        try {
            // Execute the receiver
            replicationReceiver.receive(message);
        } finally {
            this.executionContextManager.popContext();
        }

        // Delete it from disk
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
}
