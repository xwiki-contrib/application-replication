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
package org.xwiki.contrib.replication.entity.internal.ui;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

/**
 * Inject a UI under each document to give replication related informations.
 * 
 * @version $Id$
 */
@Component
@Named(DocumentReplicationConflictUIExtension.ID)
@Singleton
public class DocumentReplicationConflictUIExtension implements UIExtension
{
    /**
     * The id of the UI extension.
     */
    public static final String ID = "replication.conflict";

    @Inject
    private TemplateManager templates;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.template.content.header.after";
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Collections.emptyMap();
    }

    @Override
    public Block execute()
    {
        return this.templates.executeNoException("replication/conflict.vm");
    }
}
