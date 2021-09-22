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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.xwiki.contrib.replication.test.po.RegisteredInstancePane;
import org.xwiki.contrib.replication.test.po.ReplicationAdministrationSectionPage;
import org.xwiki.contrib.replication.test.po.RequestedInstancePane;
import org.xwiki.contrib.replication.test.po.RequestingInstancePane;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;

import com.google.common.base.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Verify the document cache update based on distributed events.
 * 
 * @version $Id: f6ae6de6d59b97c88b228130b45cd26ce7b305ff $
 */
public class ReplicationIT extends AbstractTest
{
    private <T> void assertEqualsWithTimeout(T expected, Supplier<T> supplier) throws InterruptedException
    {
        long t2;
        long t1 = System.currentTimeMillis();
        T result;
        while (!Objects.equal((result = supplier.get()), expected)) {
            t2 = System.currentTimeMillis();
            if (t2 - t1 > 10000L) {
                fail(String.format("Should have been [%s] but was [%s]", expected, result));
            }
            Thread.sleep(100L);
        }
    }

    private void assertEqualsContentWithTimeout(LocalDocumentReference documentReference, String content)
        throws InterruptedException
    {
        assertEqualsWithTimeout(content, () -> {
            try {
                Page page = getUtil().rest().<Page>get(documentReference, false);

                return page != null ? page.getContent() : null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void assertEqualsHistorySizeWithTimeout(LocalDocumentReference documentReference, int historySize)
        throws InterruptedException
    {
        assertEqualsWithTimeout(historySize, () -> {
            try {
                return getHistory(documentReference).getHistorySummaries().size();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void assertDoesNotExistWithTimeout(LocalDocumentReference documentReference) throws InterruptedException
    {
        assertEqualsWithTimeout(null, () -> {
            try {
                return getUtil().rest().<Page>get(documentReference, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void saveMinor(Page page) throws Exception
    {
        Map<String, Object[]> queryParams = new HashMap<>();
        queryParams.put("minorRevision", new Object[] {Boolean.TRUE.toString()});

        TestUtils.assertStatusCodes(getUtil().rest().executePut(PageResource.class, page, queryParams,
            getUtil().getCurrentWiki(), page.getSpace(), page.getName()), true, TestUtils.STATUS_CREATED_ACCEPTED);
    }

    private History getHistory(LocalDocumentReference documentReference) throws Exception
    {
        return getUtil().rest().getResource("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/history", null,
            getUtil().getCurrentWiki(), documentReference.getParent().getName(), documentReference.getName());
    }

    // Tests

    @Test
    public void all() throws Exception
    {
        // Authenticate on both nodes
        getUtil().switchExecutor(0);
        getUtil().loginAsSuperAdmin();
        getUtil().switchExecutor(1);
        getUtil().loginAsSuperAdmin();

        // Link two instances
        instances();

        // Configure replication
        controller();

        // Replicate a page between the 2 registered instances
        replicate();
    }

    private void instances()
    {
        // Get instances uris
        getUtil().switchExecutor(1);
        String uri1 = StringUtils.removeEnd(getUtil().getBaseURL(), "/");
        getUtil().switchExecutor(0);
        String uri0 = StringUtils.removeEnd(getUtil().getBaseURL(), "/");

        // Login on instance0
        getUtil().loginAsSuperAdmin();
        ReplicationAdministrationSectionPage admin0 = ReplicationAdministrationSectionPage.gotoPage();

        admin0.setRequestedURI(uri1);
        admin0 = admin0.requestInstance();

        // Check if the instance has been added to requested instances
        List<RequestedInstancePane> requestedInstances = admin0.getRequestedInstances();
        assertEquals(1, requestedInstances.size());
        assertEquals(uri1, requestedInstances.get(0).getURI());

        // Go to instance1
        getUtil().switchExecutor(1);
        getUtil().loginAsSuperAdmin();
        ReplicationAdministrationSectionPage admin1 = ReplicationAdministrationSectionPage.gotoPage();

        // Check if the instance has been added to requesting instances
        List<RequestingInstancePane> requestingInstances = admin1.getRequestingInstances();
        assertEquals(1, requestingInstances.size());
        RequestingInstancePane requestingInstance = requestingInstances.get(0);
        assertEquals(uri0, requestingInstance.getURI());

        admin1 = requestingInstance.accept();

        // Check if the instance has been moved to registered instances
        requestingInstances = admin1.getRequestingInstances();
        assertEquals(0, requestingInstances.size());

        // Check if the instance has been moved to registered instances
        List<RegisteredInstancePane> registeredInstances = admin1.getRegisteredInstances();
        assertEquals(1, registeredInstances.size());
        assertEquals(uri0, registeredInstances.get(0).getURI());

        // Go back to instance0
        getUtil().switchExecutor(0);
        admin0 = ReplicationAdministrationSectionPage.gotoPage();

        // Check if the instance has been moved to registered instances
        requestedInstances = admin0.getRequestedInstances();
        assertEquals(0, requestedInstances.size());

        // Check if the instance has been moved to registered instances
        registeredInstances = admin1.getRegisteredInstances();
        assertEquals(1, registeredInstances.size());
        assertEquals(uri1, registeredInstances.get(0).getURI());
    }

    private void controller()
    {
        // TODO: create Replication space on instance0

        // TODO: enable full replication for the space Replication

        // TODO: make sure the configuration is replicated on instance1
    }

    private void replicate() throws Exception
    {
        Page page = new Page();
        page.setSpace("Replication");
        page.setName("ReplicatedPage");

        LocalDocumentReference documentReference = new LocalDocumentReference(page.getSpace(), page.getName());

        getUtil().rest().delete(documentReference);

        ////////////////////////////////////
        // Major edit on XWiki 0
        ////////////////////////////////////

        // Edit a page on XWiki 0
        getUtil().switchExecutor(0);
        page.setContent("content");
        getUtil().rest().save(page);
        assertEquals("content", getUtil().rest().<Page>get(documentReference).getContent());

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(1);
        assertEqualsContentWithTimeout(documentReference, "content");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "1.1", page.getVersion());

        ////////////////////////////////////
        // Minor edit on XWiki 0
        ////////////////////////////////////

        // Edit a page on XWiki 0
        getUtil().switchExecutor(0);
        page.setContent("minor content");
        saveMinor(page);
        assertEquals("minor content", getUtil().rest().<Page>get(documentReference).getContent());

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(1);
        assertEqualsContentWithTimeout(documentReference, "minor content");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "1.2", page.getVersion());

        ////////////////////////////////////
        // Major edit on XWiki 1
        ////////////////////////////////////

        // Modify content of the page on XWiki 1
        getUtil().switchExecutor(1);
        page.setContent("modified content");
        getUtil().rest().save(page);
        assertEquals("modified content", getUtil().rest().<Page>get(documentReference).getContent());

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertEqualsContentWithTimeout(documentReference, "modified content");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "2.1", page.getVersion());

        ////////////////////////////////////
        // Delete version on XWiki 0
        ////////////////////////////////////

        // Delete a page history version on XWiki 0
        getUtil().switchExecutor(0);
        History history = getHistory(documentReference);
        HistorySummary historySummary = history.getHistorySummaries().get(0);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("1.2", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(2);
        assertEquals("1.1", historySummary.getVersion());

        getUtil().recacheSecretToken();
        getUtil().deleteVersion(page.getSpace(), page.getName(), "1.2");

        assertEqualsHistorySizeWithTimeout(documentReference, 2);
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("1.1", historySummary.getVersion());

        // ASSERT) The history in XWiki 1 should be the one set in XWiki 0
        getUtil().switchExecutor(1);
        assertEqualsHistorySizeWithTimeout(documentReference, 2);
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("1.1", historySummary.getVersion());

        ////////////////////////////////////
        // Delete current version on XWiki 1
        ////////////////////////////////////

        // Delete a page history version on XWiki 1
        getUtil().switchExecutor(1);
        getUtil().recacheSecretToken();
        getUtil().deleteVersion(page.getSpace(), page.getName(), "2.1");

        assertEqualsHistorySizeWithTimeout(documentReference, 1);
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("1.1", historySummary.getVersion());

        // ASSERT) The history in XWiki 1 should be the one set in XWiki 0
        getUtil().switchExecutor(0);
        assertEqualsHistorySizeWithTimeout(documentReference, 1);
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("1.1", historySummary.getVersion());

        ////////////////////////////////////
        // Delete document on XWiki 0
        ////////////////////////////////////

        getUtil().switchExecutor(0);
        getUtil().rest().delete(documentReference);

        // TODO: ASSERT) Get the deleted document id

        // ASSERT) The document should not exist anymore on XWiki 1
        getUtil().switchExecutor(1);
        // Since it can take time for the replication to propagate the change, we need to wait and set up a timeout.
        assertDoesNotExistWithTimeout(documentReference);

        // TODO: ASSERT) The deleted document has the expected id
    }
}
