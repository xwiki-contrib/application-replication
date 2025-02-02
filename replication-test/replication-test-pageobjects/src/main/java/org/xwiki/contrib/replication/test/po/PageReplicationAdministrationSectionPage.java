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
        getUtil().gotoPage(
            new EntityReference("WebPreferences", EntityType.SPACE,
                reference.getType() == EntityType.DOCUMENT ? reference.getParent() : reference),
            "admin", "section=Replication");

        return new PageReplicationAdministrationSectionPage();
    }

    public DocumentReplicationLevel getSpaceLevel()
    {
        return getLevel("space", null);
    }

    public DocumentReplicationLevel getDocumentLevel()
    {
        return getLevel("document", null);
    }

    private String getOptionId(String scope, boolean single)
    {
        return scope + "_replication_instance_type_" + (single ? "single" : "all");
    }

    private Select getLevelSelect(String scope, String instance)
    {
        WebElement levelElement;
        if (instance == null) {
            levelElement = getDriver().findElement(By.id(scope + "_replication_instance_level"));
        } else {
            levelElement = getDriver().findElement(By.xpath("//fieldset[input[@id=\"" + getOptionId(scope, instance != null)
                + "\"]]//dd[input[@value=\"" + instance + "\"]]/div[@class='replication-configuration-container']/select"));
        }
        return new Select(levelElement);
    }

    private DocumentReplicationLevel getLevel(String scope, String instance)
    {
        // Set the level
        Select levelSelect = getLevelSelect(scope, instance);
        String value = levelSelect.getFirstSelectedOption().getAttribute("value");
        return StringUtils.isEmpty(value) ? null : DocumentReplicationLevel.valueOf(value);
    }

    public String getMode(String scope)
    {
        WebElement element = getDriver().findElement(By.id(scope + "_replication_instance_type_default"));
        if (element.isSelected()) {
            return "default";
        }

        element = getDriver().findElement(By.id(getOptionId(scope, false)));
        if (element.isSelected()) {
            return "all";
        }

        element = getDriver().findElement(By.id(getOptionId(scope, true)));
        if (element.isSelected()) {
            return "single";
        }

        return null;
    }

    public void setSpaceLevel(DocumentReplicationLevel level)
    {
        setLevel("space", null, level);
    }

    public void setDocumentLevel(DocumentReplicationLevel level)
    {
        setLevel("document", null, level);
    }

    public void setSpaceLevel(String instance, DocumentReplicationLevel level)
    {
        setLevel("space", instance, level);
    }

    public void setDocumentLevel(String instance, DocumentReplicationLevel level)
    {
        setLevel("document", instance, level);
    }

    private void setLevel(String scope, String instance, DocumentReplicationLevel level)
    {
        // Make sure the right option is selected
        getDriver().findElement(By.id(getOptionId(scope, instance != null))).click();

        // Set the level
        Select levelSelect = getLevelSelect(scope, instance);
        levelSelect.selectByValue(level != null ? StringUtils.capitalize(level.name()) : "");
    }

    public void save()
    {
        getDriver().findElement(By.id("replication_save")).click();
    }
}
