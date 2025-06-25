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
package org.xwiki.contrib.replication.test.po;

import java.util.Map;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible on the Replication Pane at the bottom of a page.
 * 
 * @version $Id: eea4af2078bb390fbfe0bbb883a0e5313bb9d4c9 $
 */
public class ReplicationDocExtraPane extends BaseElement
{
    private static final Map<String, DocumentReplicationLevel> LEVEL_MAPPING =
        Map.of("Everything", DocumentReplicationLevel.ALL, "Placeholder", DocumentReplicationLevel.REFERENCE);

    @FindBy(css = "dd[data-key='owner']")
    private WebElement ownerDD;

    @FindBy(css = "dd[data-key='readonly']")
    private WebElement readonlyDD;

    @FindBy(css = "dd[data-key='level']")
    private WebElement levelDD;

    public String getOwner()
    {
        return this.ownerDD.getText();
    }

    public boolean isReadonly()
    {
        return Boolean.parseBoolean(this.readonlyDD.getText());
    }

    public DocumentReplicationLevel getLevel()
    {
        return LEVEL_MAPPING.get(this.levelDD.getText());
    }
}
