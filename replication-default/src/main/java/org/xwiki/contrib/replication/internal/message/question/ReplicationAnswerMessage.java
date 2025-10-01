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
package org.xwiki.contrib.replication.internal.message.question;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.AbstractReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationMessage;

/**
 * @version $Id$
 * @since 2.0.0
 */
@Component(roles = ReplicationAnswerMessage.class)
public class ReplicationAnswerMessage extends AbstractReplicationSenderMessage
{
    @Override
    public String getType()
    {
        return TYPE_ANSWER;
    }

    /**
     * @param questionMessage the message containing the question
     * @param customMetadata the actual content of the answer
     * @throws ReplicationException when failing to initialize the message
     */
    public void initialize(ReplicationMessage questionMessage, Map<String, Collection<String>> customMetadata)
        throws ReplicationException
    {
        super.initialize();

        // Set the answer to the question
        this.modifiableMetadata.putAll(customMetadata);

        // The answer is sent to the instance which asked the question
        this.receivers = Set.of(questionMessage.getSource());

        // Indicate the identifier of the question this answer is about
        putCustomMetadata(METADATA_ANSWER_QUESTION_ID, questionMessage.getId());
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        // No content
    }
}
