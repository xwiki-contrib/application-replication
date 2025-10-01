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

import java.io.Serializable;

import org.xwiki.observation.event.Event;

/**
 * Event triggered when and answer message has been received.
 * <p>
 * The following information are also sent:
 * <ul>
 * <li>source: the target receivers as {@code Collection<String>}</li>
 * <li>data: null</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.3.0
 */
public class ReplicationQuestionAskedEvent implements Event, Serializable
{
    private static final long serialVersionUID = 1L;

    private String questionId;

    /**
     * Listener to all questions.
     */
    public ReplicationQuestionAskedEvent()
    {
    }

    /**
     * @param questionId the identifier of the question
     */
    public ReplicationQuestionAskedEvent(String questionId)
    {
        this.questionId = questionId;
    }

    /**
     * @return the question identifier
     */
    public String getQuestionId()
    {
        return questionId;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ReplicationQuestionAskedEvent;
    }
}
