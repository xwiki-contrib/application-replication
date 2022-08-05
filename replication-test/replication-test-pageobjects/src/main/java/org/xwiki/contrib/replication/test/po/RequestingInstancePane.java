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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Displays information about a replication instance.
 * 
 * @version $Id: 0bdab4653171162f3b27340382543e218ce66d49 $
 */
public class RequestingInstancePane extends AbstractInstancePane
{
    /**
     * Creates a new instance.
     * 
     * @param container the dependency container
     */
    public RequestingInstancePane(WebElement container)
    {
        super(container);
    }

    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public String getReceiveKey()
    {
        return super.getReceiveKey();
    }

    public WikiReplicationAdministrationSectionPage accept()
    {
        WebElement acceptButton = this.container.findElement(By.name("requesting_accept"));

        acceptButton.click();

        return new WikiReplicationAdministrationSectionPage();
    }

    public WikiReplicationAdministrationSectionPage decline()
    {
        WebElement declineButton = this.container.findElement(By.name("requesting_decline"));

        declineButton.click();

        return new WikiReplicationAdministrationSectionPage();
    }
}
