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

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.Select;

/**
 * Represents the actions possible on the Replication Pane at the bottom of a page.
 * 
 * @version $Id: eea4af2078bb390fbfe0bbb883a0e5313bb9d4c9 $
 */
public class ReplicationDocExtraPane extends BaseElement
{
    public void setSpaceLevel(DocumentReplicationLevel level)
    {
        setLevel("space", level);
    }

    public void setDocumentLevel(DocumentReplicationLevel level)
    {
        setLevel("document", level);
    }

    private void setLevel(String scope, DocumentReplicationLevel level)
    {
        // Make sure "All instances" option is selected
        getDriver().findElement(By.id("space_replication_instance_type_all")).click();

        // Set the level
        Select levelSelect = new Select(getDriver().findElement(By.id(scope + "_replication_instance_level")));
        levelSelect.selectByVisibleText(StringUtils.capitalize(level.name().toLowerCase()));
    }

    public void setSpaceLevel(int index, DocumentReplicationLevel level)
    {
        setLevel("space", index, level);
    }

    public void setDocumentLevel(int index, DocumentReplicationLevel level)
    {
        setLevel("document", index, level);
    }

    private void setLevel(String scope, int index, DocumentReplicationLevel level)
    {
        // Make sure "All instances" option is selected
        getDriver().findElement(By.id("space_replication_instance_type_single")).click();

        // Set the level
        Select levelSelect = new Select(getDriver().findElement(By.id(scope + "_replication_instance_level_" + index)));
        levelSelect.selectByVisibleText(StringUtils.capitalize(level.name().toLowerCase()));
    }
}
