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
package org.xwiki.contrib.replication.test;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.AbstractReplicationReceiver;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSender;

/**
 * @version $Id$
 */
@Component
@Named(TestQuestionReceiver.ID)
@Singleton
public class TestQuestionReceiver extends AbstractReplicationReceiver
{
    public static final String ID = "test-question";

    public static final String PROP_NAME = "NAME";

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private ReplicationSender sender;

    @Override
    public void receive(ReplicationReceiverMessage message) throws ReplicationException
    {
        this.sender.answer(message, Map.of(PROP_NAME, List.of(this.instances.getCurrentInstance().getName())));
    }
}
