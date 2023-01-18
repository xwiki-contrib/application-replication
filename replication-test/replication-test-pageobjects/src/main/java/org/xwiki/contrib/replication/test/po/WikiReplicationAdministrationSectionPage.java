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
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationSectionPage;

/**
 * Represents the actions possible on the Replication Administration at wiki level.
 *
 * @version $Id: a9d09c8b6af7f1411760636950fa485cc241efd1 $
 */
public class WikiReplicationAdministrationSectionPage extends AdministrationSectionPage
{
    @FindBy(id = "current_name")
    private WebElement currentNameInput;

    @FindBy(id = "current_uri")
    private WebElement currentURIInput;

    @FindBy(name = "current_save")
    private WebElement sameButton;

    @FindBy(id = "requested_uri")
    private WebElement requestedInput;

    @FindBy(name = "requested_add")
    private WebElement requestedAddButton;

    public WikiReplicationAdministrationSectionPage()
    {
        super("Replication");

        waitUntilActionButtonIsLoaded();
    }

    public static WikiReplicationAdministrationSectionPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "section=Replication");

        return new WikiReplicationAdministrationSectionPage();
    }

    /**
     * @since 1.4.0
     */
    public String getCurrentName()
    {
        return this.currentNameInput.getAttribute("value");
    }

    public void setCurrentName(String name)
    {
        this.currentNameInput.clear();
        this.currentNameInput.sendKeys(name);
    }

    /**
     * @since 1.4.0
     */
    public String getCurrentURI()
    {
        return this.currentURIInput.getAttribute("value");
    }

    public void setCurrentURI(String uri)
    {
        this.currentURIInput.clear();
        this.currentURIInput.sendKeys(uri);
    }

    public WikiReplicationAdministrationSectionPage clickSaveButton()
    {
        this.sameButton.click();

        return new WikiReplicationAdministrationSectionPage();
    }

    public void setRequestedURI(String uri)
    {
        this.requestedInput.clear();
        this.requestedInput.sendKeys(uri);
    }

    private List<WebElement> getInstances(String buttonName)
    {
        return getDriver()
            .findElementsWithoutWaiting(By.xpath("//li[.//button[contains(@name, '" + buttonName + "')]]"));
    }

    public List<RequestedInstancePane> getRequestedInstances()
    {
        List<WebElement> elements = getInstances("requested_cancel");

        return elements.stream().map(RequestedInstancePane::new).collect(Collectors.toList());
    }

    public List<RequestingInstancePane> getRequestingInstances()
    {
        List<WebElement> elements = getInstances("requesting_accept");

        return elements.stream().map(RequestingInstancePane::new).collect(Collectors.toList());
    }

    public List<RegisteredInstancePane> getRegisteredInstances()
    {
        List<WebElement> elements = getInstances("remove");

        return elements.stream().map(RegisteredInstancePane::new).collect(Collectors.toList());
    }

    public WikiReplicationAdministrationSectionPage requestInstance()
    {
        this.requestedAddButton.click();

        return new WikiReplicationAdministrationSectionPage();
    }
}
