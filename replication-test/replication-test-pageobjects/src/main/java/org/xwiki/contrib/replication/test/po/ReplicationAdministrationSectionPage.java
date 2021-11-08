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
 * Represents the actions possible on the Replication Administration Page.
 *
 * @version $Id: a9d09c8b6af7f1411760636950fa485cc241efd1 $
 */
public class ReplicationAdministrationSectionPage extends AdministrationSectionPage
{
    @FindBy(id = "requested_uri")
    private WebElement requestedInput;

    @FindBy(name = "requested_add")
    private WebElement requestedAddButton;

    public ReplicationAdministrationSectionPage()
    {
        super("Replication");

        waitUntilActionButtonIsLoaded();
    }

    public static ReplicationAdministrationSectionPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "section=Replication");

        return new ReplicationAdministrationSectionPage();
    }

    public void setRequestedURI(String uri)
    {
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

    public ReplicationAdministrationSectionPage requestInstance()
    {
        this.requestedAddButton.click();

        return new ReplicationAdministrationSectionPage();
    }
}
