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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationAnswer;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.ReplicationMessageReader;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * @version $Id$
 * @since 2.0.0
 */
@Component(roles = ReplicationAnswerManager.class)
@Singleton
public class ReplicationAnswerManager
{
    @Inject
    private ReplicationMessageReader messageReader;

    @Inject
    private Logger logger;

    private final class ReplicationAnswerEntry
    {
        private final Set<String> receivers;

        private final DefaultReplicationAnswer answer = new DefaultReplicationAnswer();

        private final CompletableFuture<ReplicationAnswer> future = new CompletableFuture<>();

        /**
         * @param receivers the identifiers of the instances which should answer the question
         */
        private ReplicationAnswerEntry(Collection<String> receivers)
        {
            if (CollectionUtils.isNotEmpty(receivers)) {
                this.receivers = ConcurrentHashMap.newKeySet(receivers.size());
                this.receivers.addAll(receivers);
            } else {
                this.receivers = null;
            }
        }
    }

    private class DefaultReplicationAnswer implements ReplicationAnswer
    {
        private final List<ReplicationReceiverMessage> answers = new CopyOnWriteArrayList<>();

        @Override
        public List<ReplicationReceiverMessage> getAnswers()
        {
            return this.answers;
        }

    }

    private final Map<String, ReplicationAnswerEntry> answers = new ConcurrentHashMap<>();

    /**
     * @param message the received message
     * @throws InvalidReplicationMessageException when failing to parse the message
     */
    public void onReceive(ReplicationReceiverMessage message) throws InvalidReplicationMessageException
    {
        // Get the id of the question
        String questionId =
            this.messageReader.getMetadata(message, ReplicationMessage.METADATA_ANSWER_QUESTION_ID, true);

        // Check if we know the question
        if (questionId != null) {
            ReplicationAnswerEntry entry = this.answers.get(questionId);

            if (entry == null) {
                throw new InvalidReplicationMessageException(
                    "Received an answer [{}] to an unknown question id [" + questionId + "]");
            }

            // Update the receivers
            if (entry.receivers != null) {
                if (!entry.receivers.remove(message.getSource())) {
                    throw new InvalidReplicationMessageException(
                        "No answer is expected from instance [" + message.getSource() + "]");
                }

                // If there is no more awaited receiver, unlock the answer
                if (entry.receivers.isEmpty()) {
                    entry.future.complete(entry.answer);
                }
            }

            // Remember the message as an answer
            entry.answer.answers.add(message);
        }
    }

    /**
     * @param id the identifier of the question
     * @param receivers the identifiers of the instances which should answer the question
     * @return the new {@link CompletableFuture} providing the final answer
     */
    public CompletableFuture<ReplicationAnswer> ask(String id, Collection<String> receivers)
    {
        ReplicationAnswerEntry entry = new ReplicationAnswerEntry(receivers);

        this.answers.put(id, entry);

        return entry.future;
    }
}
