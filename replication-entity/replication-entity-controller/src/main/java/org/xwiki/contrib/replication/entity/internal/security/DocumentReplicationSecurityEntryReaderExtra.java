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
package org.xwiki.contrib.replication.entity.internal.security;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.internal.EntityReplicationStore;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.SecurityEntryReaderExtra;
import org.xwiki.security.authorization.SecurityRule;

/**
 * Inject extra rule to protect document with forbidden replication (REFERENCE, readonly).
 * 
 * @version $Id: 2eeed45b36d77426f0331137346a088f0ec0f833 $
 */
@Component
@Named("replication")
@Singleton
public class DocumentReplicationSecurityEntryReaderExtra implements SecurityEntryReaderExtra
{
    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private EntityReplicationStore store;

    @Override
    public Collection<SecurityRule> read(SecurityReference entityReference) throws AuthorizationException
    {
        DocumentReplicationControllerInstance configuration;
        try {
            configuration =
                this.store.resolveHibernateEntityReplication(entityReference, this.instances.getCurrentInstance());
        } catch (Exception e) {
            throw new AuthorizationException("Failed get replication rules", e);
        }

        if (configuration.isReadonly() || configuration.getLevel() == DocumentReplicationLevel.REFERENCE) {
            return Collections.singleton(DocumentReplicationSecurityRule.INSTANCE);
        }

        return null;
    }
}
