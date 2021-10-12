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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.DisposePriority;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.internal.message.ReplicationSenderMessageStore.FileReplicationSenderMessage;

/**
 * @version $Id$
 */
@Component
@Singleton
// Make sure the component is disposed at the end in case some data still need saving
@DisposePriority(10000)
public class DefaultReplicationSender implements ReplicationSender, Initializable, Disposable
{
    private static final QueueEntry STOP = new QueueEntry(null, (Collection<ReplicationInstance>) null);

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private ReplicationSenderMessageStore store;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private Provider<ReplicationSenderMessageQueue> sendQueueProvider;

    @Inject
    private Logger logger;

    private Thread storeThread;

    private final Map<String, ReplicationSenderMessageQueue> sendQueues = new ConcurrentHashMap<>();

    private final BlockingQueue<QueueEntry> storeQueue = new LinkedBlockingQueue<>(1000);

    private static final class QueueEntry
    {
        private final Collection<ReplicationInstance> targets;

        private final ReplicationSenderMessage message;

        private QueueEntry(ReplicationSenderMessage data, ReplicationInstance target)
        {
            this(data, Collections.singletonList(target));
        }

        private QueueEntry(ReplicationSenderMessage data, Collection<ReplicationInstance> targets)
        {
            this.targets = targets;
            this.message = data;
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Create a thread in charge of locally serializing data to send to other instances
        this.storeThread = new Thread(this::store);
        this.storeThread.setName("Replication serializing");
        this.storeThread.setPriority(Thread.NORM_PRIORITY - 1);
        this.storeThread.start();

        // Load the queue from disk
        Queue<ReplicationSenderMessage> messages = this.store.load();
        for (ReplicationSenderMessage message : messages) {
            FileReplicationSenderMessage fileMessage = (FileReplicationSenderMessage) message;

            addSend(fileMessage, fileMessage.getTargets());
        }
    }

    private void addSend(ReplicationSenderMessage message, Iterable<ReplicationInstance> instances)
    {
        for (ReplicationInstance instance : instances) {
            addSend(message, instance);
        }
    }

    private void addSend(ReplicationSenderMessage message, ReplicationInstance instance)
    {
        getSendQueue(instance, true).add(message);
    }

    private ReplicationSenderMessageQueue getSendQueue(ReplicationInstance instance, boolean create)
    {
        ReplicationSenderMessageQueue queue = this.sendQueues.get(instance.getURI());

        if (queue != null || !create) {
            return queue;
        }

        // Create the queue
        return createSendQueue(instance);
    }

    private ReplicationSenderMessageQueue createSendQueue(ReplicationInstance instance)
    {
        ReplicationSenderMessageQueue queue = this.sendQueues.get(instance.getURI());

        if (queue != null) {
            return queue;
        }

        // Create the queue
        queue = this.sendQueueProvider.get();
        queue.start(instance);

        return queue;
    }

    private void store()
    {
        while (true) {
            QueueEntry entry;
            try {
                entry = this.storeQueue.take();

                // Stop the loop when asked to
                if (entry == STOP) {
                    break;
                }

                syncStore(entry.message, entry.targets);
            } catch (InterruptedException e) {
                this.logger.warn("The replication storing thread has been interrupted");

                // Mark back the thread as interrupted
                this.storeThread.interrupt();

                // Stop thread
                return;
            } catch (Exception e) {
                this.logger.error("Failed to store the message", e);
            }
        }
    }

    private void syncStore(ReplicationSenderMessage message, Collection<ReplicationInstance> targets)
        throws ExecutionContextException, ReplicationException
    {
        // Get the instances to send the data to
        Collection<ReplicationInstance> finalTargets = targets;
        if (targets == null) {
            finalTargets = this.instances.getRegisteredInstances();
        }

        // Stop there if there is no instance to send the message to
        if (!finalTargets.isEmpty()) {
            // Make sure an ExecutionContext is available
            this.executionContextManager.pushContext(new ExecutionContext(), false);

            try {
                FileReplicationSenderMessage fileMessage = this.store.store(message, finalTargets);

                // Put the stored message in the sending queue
                addSend(fileMessage, fileMessage.getTargets());
            } catch (Exception e) {
                this.logger.error("Failed to store the message with id [" + message.getId() + "] on disk."
                    + " Might be lost if it cannot be sent to the target instance before next restart.", e);

                // Put the initial message in the sending queue and hope it's reusable
                addSend(message, finalTargets);
            } finally {
                this.executionContextManager.popContext();
            }
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // Dispose sending
        for (ReplicationSenderMessageQueue queue : this.sendQueues.values()) {
            try {
                queue.dispose();
            } catch (ComponentLifecycleException e) {
                this.logger.error("Failed to dispose queue for replication instance [{}]", queue.getInstance(), e);
            }
        }

        // Dispose serialization
        disposeSerialize();
    }

    private void disposeSerialize()
    {
        try {
            this.storeQueue.put(STOP);

            // Wait for the processing to be over but not more than 60s in case it's stuck for some reason
            this.storeThread.join(60000);

            // Stop the thread if it's still running
            if (this.storeThread.isAlive()) {
                this.logger.warn("The replication serialization thread is still running, killing it");

                this.storeThread.interrupt();
            }
        } catch (InterruptedException e) {
            this.logger.warn("The replication serialization thread has been interrupted: {}",
                ExceptionUtils.getRootCauseMessage(e));

            this.storeThread.interrupt();
        }
    }

    @Override
    public void send(ReplicationSenderMessage data) throws ReplicationException
    {
        try {
            this.storeQueue.put(new QueueEntry(data, (Collection<ReplicationInstance>) null));
        } catch (InterruptedException e) {
            // Mark the thread as interrupted
            this.storeThread.interrupt();

            throw new ReplicationException(String.format("Failed to queue the data [%s]", data), e);
        }
    }

    @Override
    public void send(ReplicationSenderMessage data, Collection<ReplicationInstance> targets) throws ReplicationException
    {
        try {
            this.storeQueue.put(new QueueEntry(data, targets));
        } catch (InterruptedException e) {
            // Mark the thread as interrupted
            this.storeThread.interrupt();

            throw new ReplicationException(
                String.format("Failed to queue the data [%s] targetting instances %s", data, targets), e);
        }
    }

    @Override
    public void ping(ReplicationInstance instance)
    {
        ReplicationSenderMessageQueue queue = getSendQueue(instance, false);

        if (queue != null) {
            queue.wakeUp();
        }
    }
}
