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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

/**
 * Inject a UI under each document to give replication related informations.
 * 
 * @version $Id$
 */
@Component
@Named(DocumentReplicationUIExtension.ID)
@Singleton
public class DocumentReplicationUIExtension implements UIExtension
{
    /**
     * The id of the UI extension.
     */
    public static final String ID = "replication.docextra";

    @Inject
    private ContextualLocalizationManager localization;

    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private TemplateManager templates;

    @Inject
    private Logger logger;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.plaftorm.template.docextra";
    }

    @Override
    public Map<String, String> getParameters()
    {
        Map<String, String> parameters = new HashMap<>();

        boolean show;
        try {
            show = !this.instances.getRegisteredInstances().isEmpty();
        } catch (ReplicationException e) {
            this.logger.error("Failed to get registered instances", e);
            show = false;
        }

        parameters.put("show", String.valueOf(show));

        if (show) {
            parameters.put("title", translate("replication.entity.docextra.title", "Replication"));
            parameters.put("itemnumber", "-1");
            parameters.put("name", "replication");
            // parameters.put("shortcut", "");
            // parameters.put("order", "");
        }

        return parameters;
    }

    private String translate(String key, String def)
    {
        String translation = this.localization.getTranslationPlain(key);

        return translation != null ? translation : def;
    }

    @Override
    public Block execute()
    {
        return this.templates.executeNoException("replication/docextra.vm");
    }
}
