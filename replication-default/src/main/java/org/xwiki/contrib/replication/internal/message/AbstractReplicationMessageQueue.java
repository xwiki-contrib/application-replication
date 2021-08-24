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

    protected final BlockingQueue<M> queue = new LinkedBlockingQueue<>(10000);

    @Inject
    protected Logger logger;

    protected void initializeQueue()
    {
        // Initialize handling thread
        this.thread = new Thread(this);
        this.thread.setName(getThreadName());
        this.thread.setPriority(Thread.NORM_PRIORITY - 2);
        // That thread can be stopped any time without really loosing anything
        this.thread.setDaemon(true);
        this.thread.start();
    }

    protected abstract String getThreadName();

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.disposed = true;
    }

    @Override
    public void run()
    {
        while (!this.disposed) {
            M message;
            try {
                message = this.queue.take();

                // Handle the message
                handle(message);

                // Remove the message from the store
                // TODO: put the remove in an async queue
                removeFromStore(message);
            } catch (InterruptedException e) {
                this.logger.warn("The replication sending thread has been interrupted");

                // Mark the thread as interrupted
                this.thread.interrupt();

                // Stop the loop
                break;
            } catch (Throwable t) {
                this.logger.error("An unexpected throwable was thrown while handling replication message", t);
            }
        }
    }

    protected abstract void removeFromStore(M message) throws ReplicationException;

    protected abstract void handle(M message) throws Exception;
}
