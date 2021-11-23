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
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.Select;

/**
 * Represents the actions possible on the Replication Administration at page level.
 *
 * @version $Id: a9d09c8b6af7f1411760636950fa485cc241efd1 $
 */
public class PageReplicationAdministrationSectionPage extends AdministrationSectionPage
{
    public PageReplicationAdministrationSectionPage()
    {
        super("Replication");

        waitUntilActionButtonIsLoaded();
    }

    public static PageReplicationAdministrationSectionPage gotoPage(EntityReference reference)
    {
        getUtil().gotoPage(new EntityReference("WebPreferences", EntityType.SPACE, reference.getParent()), "admin",
            "section=Replication");

        return new PageReplicationAdministrationSectionPage();
    }

    public DocumentReplicationLevel getSpaceLevel()
    {
        return getLevel("space");
    }

    public void setSpaceLevel(DocumentReplicationLevel level)
    {
        setLevel("space", level);
    }

    public DocumentReplicationLevel getDocumentLevel()
    {
        return getLevel("document");
    }

    public void setDocumentLevel(DocumentReplicationLevel level)
    {
        setLevel("document", level);
    }

    private DocumentReplicationLevel getLevel(String scope)
    {
        // Set the level
        Select levelSelect = new Select(getDriver().findElement(By.id(scope + "_replication_instance_level")));
        String value = levelSelect.getFirstSelectedOption().getAttribute("value");
        return StringUtils.isEmpty(value) ? null : DocumentReplicationLevel.valueOf(value);
    }

    public String getMode(String scope)
    {
        WebElement element = getDriver().findElement(By.id(scope + "_replication_instance_type_default"));
        if (element.isSelected()) {
            return "default";
        }

        element = getDriver().findElement(By.id(scope + "_replication_instance_type_all"));
        if (element.isSelected()) {
            return "all";
        }

        element = getDriver().findElement(By.id(scope + "_replication_instance_type_single"));
        if (element.isSelected()) {
            return "single";
        }

        return null;
    }

    private void setLevel(String scope, DocumentReplicationLevel level)
    {
        // Make sure "All instances" option is selected
        getDriver().findElement(By.id(scope + "_replication_instance_type_all")).click();

        // Set the level
        Select levelSelect = new Select(getDriver().findElement(By.id(scope + "_replication_instance_level")));
        levelSelect.selectByValue(StringUtils.capitalize(level.name()));
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

    public void save()
    {
        getDriver().findElement(By.id("replication_save")).click();
    }
}
