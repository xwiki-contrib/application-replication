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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

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
import org.xwiki.contrib.replication.internal.message.log.ReplicationMessageLogStore;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;

/**
 * @version $Id$
 */
@Component
@Singleton
// Make sure the component is disposed at the end in case some message still need saving
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
    private ReplicationMessageLogStore logStore;

    @Inject
    private EventStore eventStore;

    @Inject
    private Logger logger;

    private Thread storeThread;

    private final Map<String, ReplicationSenderMessageQueue> sendQueues = new ConcurrentHashMap<>();

    private final BlockingQueue<QueueEntry> storeQueue = new LinkedBlockingQueue<>(1000);

    private static final class QueueEntry
    {
        private final Collection<ReplicationInstance> targets;

        private final ReplicationSenderMessage message;

        private final CompletableFuture<ReplicationSenderMessage> future;

        private QueueEntry(ReplicationSenderMessage message, ReplicationInstance target)
        {
            this(message, Collections.singletonList(target));
        }

        private QueueEntry(ReplicationSenderMessage message, Collection<ReplicationInstance> targets)
        {
            this.targets = targets;
            this.message = message;

            this.future = new CompletableFuture<>();
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Create a thread in charge of locally serializing message to send to other instances
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
        synchronized (instance) {
            ReplicationSenderMessageQueue queue = this.sendQueues.get(instance.getURI());

            if (queue != null) {
                return queue;
            }

            // Create the queue
            queue = this.sendQueueProvider.get();
            queue.start(instance);

            // Put the new queue in the map
            this.sendQueues.put(instance.getURI(), queue);

            return queue;
        }
    }

    /**
     * @param instance the instance
     * @return the queue associated to the provided instance
     */
    public ReplicationSenderMessageQueue getQueue(ReplicationInstance instance)
    {
        return this.sendQueues.get(instance.getURI());
    }

    private void store()
    {
        while (true) {
            QueueEntry entry = null;
            try {
                entry = this.storeQueue.take();

                // Stop the loop when asked to
                if (entry == STOP) {
                    break;
                }

                syncStore(entry);
            } catch (InterruptedException e) {
                // Complete all remaining entries
                for (QueueEntry remaining : this.storeQueue) {
                    remaining.future.completeExceptionally(e);
                }

                this.logger.warn("The replication storing thread has been interrupted");

                // Mark back the thread as interrupted
                this.storeThread.interrupt();

                // Stop thread
                return;
            } catch (Exception e) {
                if (entry != null) {
                    entry.future.completeExceptionally(e);
                }

                this.logger.error("Failed to store the message", e);
            }
        }
    }

    private void syncStore(QueueEntry entry) throws ExecutionContextException, ReplicationException
    {
        // Get the instances to send the message to
        Collection<ReplicationInstance> targets = entry.targets;
        if (entry.targets == null) {
            targets = this.instances.getRegisteredInstances();
        }

        // Stop there if there is no instance to send the message to
        if (!targets.isEmpty()) {
            // Make sure an ExecutionContext is available
            this.executionContextManager.pushContext(new ExecutionContext(), false);

            try {
                FileReplicationSenderMessage fileMessage = this.store.store(entry.message, targets);

                // Log the message
                this.logStore.saveAsync(fileMessage, (m, e) -> {
                    Map<String, Object> custom = new HashMap<>(e.getCustom());

                    // Make the event date be the stored date
                    e.setDate(new Date());

                    custom.put(ReplicationMessageEventQuery.KEY_STATUS,
                        ReplicationMessageEventQuery.VALUE_STATUS_STORED);
                    custom.put(ReplicationMessageEventQuery.KEY_TARGETS, fileMessage.getTargets().stream()
                        .map(ReplicationInstance::getURI).collect(Collectors.toList()));

                    e.setCustom(custom);
                });

                // Notify that the message is stored
                entry.future.complete(fileMessage);

                // Put the stored message in the sending queue
                addSend(fileMessage, fileMessage.getTargets());
            } catch (Exception e) {
                this.logger.error("Failed to store the message [{}] on disk. It will be lost.", entry.message, e);

                // Unlock those waiting for the future even if the message is not really stored
                entry.future.completeExceptionally(e);
            } finally {
                this.executionContextManager.popContext();
            }
        } else {
            entry.future.complete(entry.message);
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
    public CompletableFuture<ReplicationSenderMessage> send(ReplicationSenderMessage message)
        throws ReplicationException
    {
        QueueEntry entry = new QueueEntry(message, (Collection<ReplicationInstance>) null);
        try {
            this.storeQueue.put(entry);
        } catch (InterruptedException e) {
            entry.future.completeExceptionally(e);

            // Mark the thread as interrupted
            this.storeThread.interrupt();

            throw new ReplicationException(String.format("Failed to queue the message [%s]", message), e);
        }

        return entry.future;
    }

    @Override
    public CompletableFuture<ReplicationSenderMessage> send(ReplicationSenderMessage message,
        Collection<ReplicationInstance> targets) throws ReplicationException
    {
        if (targets.isEmpty()) {
            CompletableFuture<ReplicationSenderMessage> future = new CompletableFuture<>();
            future.complete(message);

            return future;
        }

        QueueEntry entry = new QueueEntry(message, targets);
        try {
            this.storeQueue.put(entry);
        } catch (InterruptedException e) {
            entry.future.completeExceptionally(e);

            // Mark the thread as interrupted
            this.storeThread.interrupt();

            throw new ReplicationException(
                String.format("Failed to queue the message [%s] targetting instances %s", message, targets), e);
        }

        return entry.future;
    }

    @Override
    public void ping(ReplicationInstance instance)
    {
        ReplicationSenderMessageQueue queue = getSendQueue(instance, false);

        if (queue != null) {
            queue.wakeUp();
        }
    }

    @Override
    public void resend(ReplicationMessageEventQuery query, Collection<String> receivers) throws ReplicationException
    {
        try (EventSearchResult result = this.eventStore.search(query, Set.of(Event.FIELD_ID))) {
            for (Event event : (Iterable<Event>) result.stream()::iterator) {
                send(this.logStore.loadMessage(event.getId(), receivers));
            }
        } catch (Exception e) {
            throw new ReplicationException("Failed to query logged messages", e);
        }
    }
}
