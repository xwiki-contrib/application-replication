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
package org.xwiki.contrib.replication.entity.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.contrib.replication.entity.internal.EntityReplicationStore;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;

/**
 * Allow manipulating the configuration of replicated entities.
 * 
 * @version $Id$
 */
@Component
@Named("replication.controller")
@Singleton
public class ControllerReplicationScriptService implements ScriptService
{
    @Inject
    private EntityReplicationStore store;

    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * @param reference the reference of the entity
     * @return the instances directly configured at this entity level
     * @throws XWikiException when failing to get the instances
     * @throws AccessDeniedException if the current script author does not have the right to use this API
     */
    public List<DocumentReplicationControllerInstance> getHibernateEntityReplication(EntityReference reference)
        throws XWikiException, AccessDeniedException
    {
        this.authorization.checkAccess(Right.PROGRAM);

        return this.store.getHibernateEntityReplication(reference);
    }

    /**
     * @param reference the reference of the entity
     * @return the instances to send the entity to
     * @throws XWikiException when failing to get the instances
     * @throws AccessDeniedException if the current script author does not have the right to use this API
     */
    public List<DocumentReplicationControllerInstance> resolveHibernateEntityReplication(EntityReference reference)
        throws XWikiException, AccessDeniedException
    {
        this.authorization.checkAccess(Right.PROGRAM);

        return this.store.resolveHibernateEntityReplication(reference);
    }
}
