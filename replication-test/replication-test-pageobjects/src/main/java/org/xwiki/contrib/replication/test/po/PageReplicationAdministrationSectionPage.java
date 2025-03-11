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
import org.xwiki.contrib.replication.entity.DocumentReplicationDirection;
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
    private static final String SCOPE_SPACE = "space";

    private static final String SCOPE_DOCUMENT = "document";

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
        return getLevel(SCOPE_SPACE, null);
    }

    public DocumentReplicationLevel getSpaceLevel(String instance)
    {
        return getLevel(SCOPE_SPACE, instance);
    }

    public DocumentReplicationDirection getSpaceDirection()
    {
        return getDirection(SCOPE_SPACE, null);
    }

    public DocumentReplicationDirection getSpaceDirection(String instance)
    {
        return getDirection(SCOPE_SPACE, instance);
    }

    public DocumentReplicationLevel getDocumentLevel()
    {
        return getLevel(SCOPE_DOCUMENT, null);
    }

    public DocumentReplicationLevel getDocumentLevel(String instance)
    {
        return getLevel(SCOPE_DOCUMENT, instance);
    }

    public DocumentReplicationDirection getDocumentDirection()
    {
        return getDirection(SCOPE_DOCUMENT, null);
    }

    public DocumentReplicationDirection getDocumentDirection(String instance)
    {
        return getDirection(SCOPE_DOCUMENT, instance);
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
            levelElement =
                getDriver().findElement(By.xpath("//fieldset[input[@id=\"" + getOptionId(scope, instance != null)
                    + "\"]]//dd[input[@value=\"" + instance + "\"]]//select[contains(@class, 'replication-level-select')]"));
        }

        return new Select(levelElement);
    }

    private Select getDirectionSelect(String scope, String instance)
    {
        WebElement levelElement;
        if (instance == null) {
            levelElement = getDriver().findElement(By.id(scope + "_replication_instance_direction"));
        } else {
            levelElement =
                getDriver().findElement(By.xpath("//fieldset[input[@id=\"" + getOptionId(scope, instance != null)
                    + "\"]]//dd[input[@value=\"" + instance + "\"]]//select[contains(@class, 'replication-direction-select')]"));
        }

        return new Select(levelElement);
    }

    private DocumentReplicationLevel getLevel(String scope, String instance)
    {
        Select select = getLevelSelect(scope, instance);
        String value = select.getFirstSelectedOption().getAttribute("value");
        return StringUtils.isEmpty(value) ? null : DocumentReplicationLevel.valueOf(value);
    }

    private DocumentReplicationDirection getDirection(String scope, String instance)
    {
        Select select = getDirectionSelect(scope, instance);
        String value = select.getFirstSelectedOption().getAttribute("value");
        return StringUtils.isEmpty(value) ? null : DocumentReplicationDirection.valueOf(value);
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
        setLevel(SCOPE_SPACE, null, level);
    }

    public void setSpaceDirection(DocumentReplicationDirection direction)
    {
        setDirection(SCOPE_SPACE, null, direction);
    }

    public void setDocumentLevel(DocumentReplicationLevel level)
    {
        setLevel(SCOPE_DOCUMENT, null, level);
    }

    public void setDocumentDirection(DocumentReplicationDirection direction)
    {
        setDirection(SCOPE_DOCUMENT, null, direction);
    }

    public void setSpaceLevel(String instance, DocumentReplicationLevel level)
    {
        setLevel(SCOPE_SPACE, instance, level);
    }

    public void setSpaceDirection(String instance, DocumentReplicationDirection direction)
    {
        setDirection(SCOPE_SPACE, instance, direction);
    }

    public void setDocumentLevel(String instance, DocumentReplicationLevel level)
    {
        setLevel(SCOPE_DOCUMENT, instance, level);
    }

    public void setDocumentDirection(String instance, DocumentReplicationDirection direction)
    {
        setDirection(SCOPE_DOCUMENT, instance, direction);
    }

    private void clickOption(String scope, String instance)
    {
        getDriver().findElement(By.id(getOptionId(scope, instance != null))).click();
    }

    private void setLevel(String scope, String instance, DocumentReplicationLevel level)
    {
        // Make sure the right option is selected
        clickOption(scope, instance);

        // Set the level
        Select levelSelect = getLevelSelect(scope, instance);
        levelSelect.selectByValue(level != null ? StringUtils.capitalize(level.name()) : "");
    }

    private void setDirection(String scope, String instance, DocumentReplicationDirection direction)
    {
        // Make sure the right option is selected
        clickOption(scope, instance);

        // Set the level
        Select select = getDirectionSelect(scope, instance);
        select.selectByValue(direction != null ? StringUtils.capitalize(direction.name()) : "");
    }

    public void save()
    {
        getDriver().findElement(By.id("replication_save")).click();
    }
}
