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

import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRule;

/**
 * A rule to forbid document modifications.
 *
 * @version $Id: fd60a643ffe3530b78cd4b32d9640ce537811521 $
 */
public class DocumentReplicationSecurityRule implements SecurityRule
{
    /**
     * Unique instance.
     */
    public static final DocumentReplicationSecurityRule INSTANCE = new DocumentReplicationSecurityRule();

    private static final Set<Right> EDIT_RIGHTS = SetUtils.hashSet(Right.EDIT, Right.DELETE, Right.COMMENT);

    @Override
    public boolean match(Right right)
    {
        return EDIT_RIGHTS.contains(right);
    }

    @Override
    public boolean match(GroupSecurityReference group)
    {
        return true;
    }

    @Override
    public boolean match(UserSecurityReference user)
    {
        return true;
    }

    @Override
    public RuleState getState()
    {
        return RuleState.DENY;
    }

    @Override
    public int hashCode()
    {
        return DocumentReplicationSecurityRule.class.hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof DocumentReplicationSecurityRule;
    }

    @Override
    public String toString()
    {
        return DocumentReplicationSecurityRule.class.toString();
    }
}
