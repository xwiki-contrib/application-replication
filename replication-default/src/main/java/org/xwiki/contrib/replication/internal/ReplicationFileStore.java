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
package org.xwiki.contrib.replication.internal;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;

/**
 * Helper to manipulate files stored for replication.
 * 
 * @version $Id$
 */
@Component(roles = ReplicationFileStore.class)
@Singleton
public class ReplicationFileStore
{
    private static final String DIRECTORY_REPLICATION = "replication";

    @Inject
    protected Environment environment;

    /**
     * @return the root folder where to store replication related data
     */
    public File getReplicationFolder()
    {
        return new File(this.environment.getPermanentDirectory(), DIRECTORY_REPLICATION);
    }
}
