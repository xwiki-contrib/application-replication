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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Displays information about a replication instance.
 * 
 * @version $Id: 0bdab4653171162f3b27340382543e218ce66d49 $
 */
public class RegisteredInstancePane extends BaseElement
{
    /**
     * The dependency container.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the dependency container
     */
    public RegisteredInstancePane(WebElement container)
    {
        this.container = container;
    }

    public String getURI()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.tagName("a")).getText();
    }

    public String getWarning()
    {
        List<WebElement> elements =
            getDriver().findElementsWithoutWaiting(this.container, By.className("warningmessage"));

        return elements.isEmpty() ? null : elements.get(0).getText();
    }

    public WikiReplicationAdministrationSectionPage clickRetry()
    {
        getDriver().findElementWithoutWaiting(this.container, By.name("replication_wakeup")).click();

        return new WikiReplicationAdministrationSectionPage();
    }

    public WikiReplicationAdministrationSectionPage remove()
    {
        WebElement removeButton = this.container.findElement(By.name("requesting_remove"));

        removeButton.click();

        return new WikiReplicationAdministrationSectionPage();
    }
}
