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
package org.xwiki.contrib.replication.entity.internal;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Default implementation of {@link DocumentReplicationController}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultDocumentReplicationController implements DocumentReplicationController
{
    @Inject
    private EntityReplicationStore store;

    @Override
    public List<DocumentReplicationControllerInstance> getDocumentInstances(DocumentReference documentReference)
        throws ReplicationException
    {
        try {
            List<DocumentReplicationControllerInstance> instances =
                this.store.resolveHibernateEntityReplication(documentReference);

            // Remove current instance
            return instances.stream().filter(i -> i.getInstance().getStatus() == Status.REGISTERED)
                .collect(Collectors.toList());
        } catch (XWikiException e) {
            throw new ReplicationException("Failed to retrieve instances from the store", e);
        }
    }
}
