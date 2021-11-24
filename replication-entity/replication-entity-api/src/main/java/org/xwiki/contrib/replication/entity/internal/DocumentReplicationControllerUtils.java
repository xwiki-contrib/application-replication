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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.model.reference.DocumentReference;

/**
 * Various helpers around {@link DocumentReplicationController}.
 * 
 * @version $Id$
 */
@Component(roles = DocumentReplicationControllerUtils.class)
@Singleton
public class DocumentReplicationControllerUtils
{
    @Inject
    private DocumentReplicationController controller;

    /**
     * @param reference the reference of the document
     * @return true if the current instance is configured to directly replicate changes made to the passed document
     * @throws ReplicationException when failing to get the configuration
     */
    public boolean isReplicated(DocumentReference reference) throws ReplicationException
    {
        return !this.controller.getReplicationConfiguration(reference).isEmpty();
    }
}
