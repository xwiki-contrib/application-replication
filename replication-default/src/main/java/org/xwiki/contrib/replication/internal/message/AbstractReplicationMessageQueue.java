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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationMessage;

/**
 * @param <M> the type of message handled
 * @version $Id$
 */
public abstract class AbstractReplicationMessageQueue<M extends ReplicationMessage> implements Disposable, Runnable
{
    protected boolean disposed;

    protected Thread thread;

    protected Thread errorThread;

    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected final BlockingQueue<M> queue = new LinkedBlockingQueue<>(10000);

    protected final BlockingQueue<M> errorQueue = new LinkedBlockingQueue<>(10000);

    @Inject
    protected Logger logger;

    protected M currentMessage;

    protected void initializeQueue()
    {
        // Initialize messages handling thread
        this.thread = new Thread(this);
        this.thread.setName(getThreadName());
        this.thread.setPriority(Thread.NORM_PRIORITY - 2);
        // That thread can be stopped any time without really loosing anything
        this.thread.setDaemon(true);
        this.thread.start();

        // Initialize failed messages handling thread
        this.errorThread = new Thread(this::runError);
        this.errorThread.setName("FAILED - " + getThreadName());
        this.errorThread.setPriority(Thread.NORM_PRIORITY - 3);
        // That thread can be stopped any time without really loosing anything
        this.errorThread.setDaemon(true);
        this.errorThread.start();
    }

    protected abstract String getThreadName();

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.disposed = true;
    }

    /**
     * @return the message currently being handled
     */
    public M getCurrentMessage()
    {
        return this.currentMessage;
    }

    /**
     * @return the queue of messages plus the currently handled one to send
     */
    public List<M> getMessages()
    {
        int size = this.queue.size() + (this.currentMessage != null ? 1 : 0);

        if (size > 0) {
            List<M> messages = new ArrayList<>(size);

            if (this.currentMessage != null) {
                messages.add(this.currentMessage);
            }

            messages.addAll(this.queue);

            return messages;
        }

        return Collections.emptyList();
    }

    @Override
    public void run()
    {
        while (!this.disposed) {
            this.currentMessage = null;

            try {
                this.currentMessage = this.queue.take();

                // Handle the message
                handle(this.currentMessage);

                // Make sure the current message has been purged
                if (this.currentMessage != null) {
                    // Remove the message from the store
                    // TODO: put the remove in an async queue
                    removeFromStore(this.currentMessage);

                    // Reset the current message
                    this.currentMessage = null;
                }
            } catch (InterruptedException e) {
                this.logger.warn("The replication sending thread has been interrupted");

                // Mark the thread as interrupted
                this.thread.interrupt();

                // Stop the loop
                break;
            } catch (Throwable t) {
                onFailed(t);
            }
        }
    }

    /**
     * Handle previously failed messages.
     */
    public void runError()
    {
        while (!this.disposed) {
            try {
                M message = this.errorQueue.take();

                // Wait 1h before handling the previously failed message
                Thread.sleep(3600000);

                List<M> messages = new ArrayList<>(this.errorQueue.size() + 1);
                messages.add(message);
                messages.addAll(this.errorQueue);

                // Handle the message
                handleFailed(messages);
            } catch (InterruptedException e) {
                this.logger.warn("The replication failed input message thread has been interrupted");

                // Mark the thread as interrupted
                this.thread.interrupt();

                // Stop the loop
                break;
            }
        }
    }

    private void handleFailed(List<M> messages) throws InterruptedException
    {
        for (M message : messages) {
            try {
                // Handle the message
                handle(message);

                // Remove the message from the store
                // TODO: put the remove in an async queue
                removeFromStore(message);
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable t) {
                onFailed(t);
            }
        }
    }

    private void onFailed(Throwable t)
    {
        if (this.currentMessage == null) {
            this.logger
                .error("An unexpected throwable was thrown while handling a previously failed replication message", t);
        } else {
            this.logger
                .error("An unexpected throwable was thrown while handling a previously failed replication message with"
                    + " id [{}] and type [{}]", this.currentMessage.getId(), this.currentMessage.getType(), t);

            // Put back the message in the queue
            this.errorQueue.add(this.currentMessage);
        }
    }

    protected abstract void removeFromStore(M message) throws ReplicationException;

    protected abstract void handle(M message) throws Exception;
}
