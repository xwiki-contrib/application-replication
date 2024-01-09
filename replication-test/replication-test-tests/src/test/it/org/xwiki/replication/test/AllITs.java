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
package org.xwiki.replication.test;

import java.util.List;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.PageObjectSuite;

/**
 * Runs all functional tests found in the classpath.
 * 
 * @version $Id: 11a75bb6173a87e719b5af67bd16b664a8ff23a7 $
 */
@RunWith(PageObjectSuite.class)
@PageObjectSuite.Executors(4)
public class AllITs
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AllITs.class);

    public static final int INSTANCE_0 = 0;

    public static final int INSTANCE_0_2 = 3;

    public static final int INSTANCE_1 = 1;

    public static final int INSTANCE_2 = 2;

    public static boolean INSTANCE_0_2_ENABLED = false;

    @PageObjectSuite.PreStart
    public void preStart(List<XWikiExecutor> executors) throws Exception
    {
        INSTANCE_0_2_ENABLED = executors.size() > INSTANCE_0_2;

        if (INSTANCE_0_2_ENABLED) {
            setupChannel(executors.get(INSTANCE_0), "tcp");
            setupChannel(executors.get(INSTANCE_0_2), "tcp");
        }

        // Disable DW
        disableDW(executors.get(INSTANCE_0));
        if (INSTANCE_0_2_ENABLED) {
            disableDW(executors.get(INSTANCE_0_2));
        }
        disableDW(executors.get(INSTANCE_1));
        disableDW(executors.get(INSTANCE_2));
    }

    private void setupChannel(XWikiExecutor executor, String channelName) throws Exception
    {
        if (executor.getExecutionDirectory() != null) {
            PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();
            properties.setProperty("observation.remote.enabled", "true");
            properties.setProperty("observation.remote.channels", channelName);
            executor.saveXWikiProperties();

            setupExecutor(executor);
        }
    }

    private void disableDW(XWikiExecutor executor) throws Exception
    {
        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();
        properties.setProperty("distribution.automaticStartOnMainWiki", "false");
        properties.setProperty("distribution.automaticStartOnWiki", "false");
        executor.saveXWikiProperties();
    }

    public static void setupExecutor(XWikiExecutor executor)
    {
        // Force bind_addr since tcp jgroups configuration expect cluster members to listen localhost by default
        executor.setXWikiOpts("-Djgroups.bind_addr=localhost -Xmx512m");
    }
}
