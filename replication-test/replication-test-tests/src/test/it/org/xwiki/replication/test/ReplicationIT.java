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
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.test.po.PageReplicationAdministrationSectionPage;
import org.xwiki.contrib.replication.test.po.RegisteredInstancePane;
import org.xwiki.contrib.replication.test.po.RequestedInstancePane;
import org.xwiki.contrib.replication.test.po.RequestingInstancePane;
import org.xwiki.contrib.replication.test.po.WikiReplicationAdministrationSectionPage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Verify the document cache update based on distributed events.
 * 
 * @version $Id: f6ae6de6d59b97c88b228130b45cd26ce7b305ff $
 */
public class ReplicationIT extends AbstractTest
{
    private static final LocalDocumentReference REPLICATION_ALL =
        new LocalDocumentReference("ReplicationALL", "WebHome");

    private static final LocalDocumentReference REPLICATION_REFERENCE =
        new LocalDocumentReference("ReplicationREFERENCE", "WebHome");

    @Rule
    public WireMockRule proxy0 = new WireMockRule(WireMockConfiguration.options().port(8070));

    @Rule
    public WireMockRule proxy1 = new WireMockRule(WireMockConfiguration.options().port(8071));

    @Rule
    public WireMockRule proxy2 = new WireMockRule(WireMockConfiguration.options().port(8072));

    private String uri0;

    private String uri1;

    private String uri2;

    private String proxyURI0;

    private String proxyURI1;

    private String proxyURI2;

    MappingBuilder proxyStubBuilder0;

    StubMapping proxyStub0;

    MappingBuilder proxyStubBuilder1;

    StubMapping proxyStub1;

    MappingBuilder proxyStubBuilder2;

    StubMapping proxyStub2;

    private <T> void assertEqualsWithTimeout(T expected, Supplier<T> supplier) throws InterruptedException
    {
        long t2;
        long t1 = System.currentTimeMillis();
        T result;
        while (!Objects.equals((result = supplier.get()), expected)) {
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

    private PageReplicationAdministrationSectionPage assertReplicationMode(LocalDocumentReference documentReference,
        String scope, String value) throws InterruptedException
    {
        assertEqualsWithTimeout(value, () -> {
            try {
                return PageReplicationAdministrationSectionPage.gotoPage(documentReference).getMode(scope);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return new PageReplicationAdministrationSectionPage();
    }

    private WikiReplicationAdministrationSectionPage assertEqualsRequestingInstancesWithTimeout(int requestingInstances)
        throws InterruptedException
    {
        assertEqualsWithTimeout(requestingInstances, () -> {
            try {
                WikiReplicationAdministrationSectionPage admin = WikiReplicationAdministrationSectionPage.gotoPage();

                return admin.getRequestingInstances().size();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return new WikiReplicationAdministrationSectionPage();
    }

    private WikiReplicationAdministrationSectionPage assertEqualsRequestedInstancesWithTimeout(int requestedInstances)
        throws InterruptedException
    {
        assertEqualsWithTimeout(requestedInstances, () -> {
            try {
                WikiReplicationAdministrationSectionPage admin = WikiReplicationAdministrationSectionPage.gotoPage();

                return admin.getRequestedInstances().size();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return new WikiReplicationAdministrationSectionPage();
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
        getUtil().switchExecutor(0);
        getUtil().loginAsSuperAdmin();
        this.uri0 = StringUtils.removeEnd(getUtil().getBaseURL(), "/");
        getUtil().switchExecutor(1);
        this.uri1 = StringUtils.removeEnd(getUtil().getBaseURL(), "/");
        getUtil().loginAsSuperAdmin();
        getUtil().switchExecutor(2);
        this.uri2 = StringUtils.removeEnd(getUtil().getBaseURL(), "/");
        getUtil().loginAsSuperAdmin();

        // Setup Wiremock
        this.proxyURI0 = this.uri0.replace("8080", "8070");
        this.proxyStubBuilder0 = WireMock.any(WireMock.urlMatching(".*"))
            .willReturn(WireMock.aResponse().proxiedFrom(StringUtils.removeEnd(this.uri0, "/xwiki")));
        this.proxyStub0 = this.proxy0.stubFor(this.proxyStubBuilder0);
        this.proxyURI1 = this.uri1.replace("8081", "8071");
        this.proxyStubBuilder1 = WireMock.any(WireMock.urlMatching(".*"))
            .willReturn(WireMock.aResponse().proxiedFrom(StringUtils.removeEnd(this.uri1, "/xwiki")));
        this.proxyStub1 = this.proxy1.stubFor(this.proxyStubBuilder1);
        this.proxyURI2 = this.uri2.replace("8082", "8072");
        this.proxyStubBuilder2 = WireMock.any(WireMock.urlMatching(".*"))
            .willReturn(WireMock.aResponse().proxiedFrom(StringUtils.removeEnd(this.uri2, "/xwiki")));
        this.proxyStub2 = this.proxy2.stubFor(this.proxyStubBuilder2);

        // Link two instances
        instances();

        // Configure replication
        controller();

        // Full replication a page between the 2 registered instances
        replicateFull();

        // Reference replication
        replicateEmpty();

        // Replication reaction to configuration change
        changeController();

        // Replication reliability
        network();
    }

    private void instances() throws InterruptedException
    {
        // Login on instance0
        getUtil().switchExecutor(0);
        WikiReplicationAdministrationSectionPage admin0 = WikiReplicationAdministrationSectionPage.gotoPage();
        // Update current URI
        admin0.setCurrentURI(this.proxyURI0);
        admin0 = admin0.clickSaveButton();

        // Link to instance1
        admin0.setRequestedURI(this.proxyURI1);
        admin0 = admin0.requestInstance();

        // Check if the instance has been added to requested instances
        List<RequestedInstancePane> requestedInstances = admin0.getRequestedInstances();
        assertEquals(1, requestedInstances.size());
        assertEquals(this.proxyURI1, requestedInstances.get(0).getURI());

        // Go to instance1
        getUtil().switchExecutor(1);
        // Check if the instance has been added to requesting instances
        WikiReplicationAdministrationSectionPage admin1 = assertEqualsRequestingInstancesWithTimeout(1);
        // Update current URI
        admin1.setCurrentURI(this.proxyURI1);
        admin1 = admin1.clickSaveButton();

        List<RequestingInstancePane> requestingInstances = admin1.getRequestingInstances();
        RequestingInstancePane requestingInstance = requestingInstances.get(0);
        assertEquals(this.proxyURI0, requestingInstance.getURI());

        // Accept the instance
        admin1 = requestingInstance.accept();

        // Link to instance2
        admin1.setRequestedURI(this.proxyURI2);
        admin1 = admin1.requestInstance();

        // Check if the instance has been moved to registered instances
        requestingInstances = admin1.getRequestingInstances();
        assertEquals(0, requestingInstances.size());

        // Check if the instance has been moved to registered instances
        List<RegisteredInstancePane> registeredInstances = admin1.getRegisteredInstances();
        assertEquals(1, registeredInstances.size());
        assertEquals(this.proxyURI0, registeredInstances.get(0).getURI());

        // Go back to instance0
        getUtil().switchExecutor(0);
        // Check if the instance has been moved to registered instances
        admin0 = assertEqualsRequestedInstancesWithTimeout(0);

        // Check if the instance has been moved to registered instances
        registeredInstances = admin0.getRegisteredInstances();
        assertEquals(1, registeredInstances.size());
        assertEquals(this.proxyURI1, registeredInstances.get(0).getURI());

        // Go to instance2
        getUtil().switchExecutor(2);
        // Check if the instance has been added to requesting instances
        WikiReplicationAdministrationSectionPage admin2 = assertEqualsRequestingInstancesWithTimeout(1);
        // Update current URI
        admin2.setCurrentURI(this.proxyURI2);
        admin1 = admin2.clickSaveButton();

        requestingInstances = admin2.getRequestingInstances();
        requestingInstance = requestingInstances.get(0);
        assertEquals(this.proxyURI1, requestingInstance.getURI());

        // Accept the instance
        requestingInstance.accept();
    }

    private void controller() throws Exception
    {
        ////////////////////////
        // FULL replication

        // Configure ReplicationALL space replication on instance0
        getUtil().switchExecutor(0);
        PageReplicationAdministrationSectionPage replicationPageAdmin =
            PageReplicationAdministrationSectionPage.gotoPage(REPLICATION_ALL);
        replicationPageAdmin.setSpaceLevel(DocumentReplicationLevel.ALL);
        // Save replication configuration
        replicationPageAdmin.save();

        // Make sure the configuration is replicated on instance1
        getUtil().switchExecutor(1);
        replicationPageAdmin = assertReplicationMode(REPLICATION_ALL, "space", "all");
        assertSame(DocumentReplicationLevel.ALL, replicationPageAdmin.getSpaceLevel());

        // Make sure the configuration is replicated on instance2
        getUtil().switchExecutor(2);
        replicationPageAdmin = assertReplicationMode(REPLICATION_ALL, "space", "all");
        assertSame(DocumentReplicationLevel.ALL, replicationPageAdmin.getSpaceLevel());

        ////////////////////////
        // REFERENCE replication

        // Configure ReplicationREFERENCE space replication on instance0
        getUtil().switchExecutor(0);
        replicationPageAdmin = PageReplicationAdministrationSectionPage.gotoPage(REPLICATION_REFERENCE);
        replicationPageAdmin.setSpaceLevel(DocumentReplicationLevel.REFERENCE);
        // Save replication configuration
        replicationPageAdmin.save();
    }

    private void replicateFull() throws Exception
    {
        Page page = new Page();
        page.setSpace(REPLICATION_ALL.getParent().getName());
        page.setName("ReplicatedPage");

        LocalDocumentReference documentReference = new LocalDocumentReference(page.getSpace(), page.getName());

        // Clean any pre-existing
        getUtil().rest().delete(documentReference);

        ////////////////////////////////////
        // Page creation on XWiki 0
        ////////////////////////////////////

        // Edit a page on XWiki 0
        getUtil().switchExecutor(0);
        page.setContent("content");
        getUtil().rest().save(page);
        assertEquals("content", getUtil().rest().<Page>get(documentReference).getContent());

        // ASSERT) The content in XWiki 1 should be the one set in XWiki 0
        getUtil().switchExecutor(1);
        assertEqualsContentWithTimeout(documentReference, "content");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "1.1", page.getVersion());

        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
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

        // ASSERT) The content in XWiki 1 should be the one set in XWiki 0
        getUtil().switchExecutor(1);
        assertEqualsContentWithTimeout(documentReference, "minor content");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "1.2", page.getVersion());

        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
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

        // ASSERT) The content in XWiki 2 should be the one set in XWiki 1
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(documentReference, "modified content");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "2.1", page.getVersion());

        ////////////////////////////////////
        // Major edit on XWiki 2
        ////////////////////////////////////

        // Modify content of the page on XWiki 2
        getUtil().switchExecutor(2);
        page.setContent("modified content 2");
        getUtil().rest().save(page);
        assertEquals("modified content 2", getUtil().rest().<Page>get(documentReference).getContent());

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 2
        getUtil().switchExecutor(0);
        assertEqualsContentWithTimeout(documentReference, "modified content 2");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "3.1", page.getVersion());

        // ASSERT) The content in XWiki 1 should be the one set in XWiki 2
        getUtil().switchExecutor(1);
        assertEqualsContentWithTimeout(documentReference, "modified content 2");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "3.1", page.getVersion());

        ////////////////////////////////////
        // Delete version on XWiki 0
        ////////////////////////////////////

        // Delete a page history version on XWiki 0
        getUtil().switchExecutor(0);
        History history = getHistory(documentReference);
        HistorySummary historySummary = history.getHistorySummaries().get(0);
        assertEquals("3.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(2);
        assertEquals("1.2", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(3);
        assertEquals("1.1", historySummary.getVersion());

        getUtil().recacheSecretToken();
        getUtil().deleteVersion(page.getSpace(), page.getName(), "1.2");

        assertEqualsHistorySizeWithTimeout(documentReference, 3);
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("3.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(2);
        assertEquals("1.1", historySummary.getVersion());

        // ASSERT) The history in XWiki 1 should be the one set in XWiki 0
        getUtil().switchExecutor(1);
        assertEqualsHistorySizeWithTimeout(documentReference, 3);
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("3.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(2);
        assertEquals("1.1", historySummary.getVersion());

        // ASSERT) The history in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(1);
        assertEqualsHistorySizeWithTimeout(documentReference, 3);
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("3.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(2);
        assertEquals("1.1", historySummary.getVersion());

        ////////////////////////////////////
        // Delete current version on XWiki 1
        ////////////////////////////////////

        // Delete a page history version on XWiki 1
        getUtil().switchExecutor(1);
        getUtil().gotoPage(page.getSpace(), page.getName());
        getUtil().recacheSecretToken();
        getUtil().deleteVersion(page.getSpace(), page.getName(), "3.1");

        assertEqualsHistorySizeWithTimeout(documentReference, 2);
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("1.1", historySummary.getVersion());

        // ASSERT) The history in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertEqualsHistorySizeWithTimeout(documentReference, 2);
        assertEquals("modified content", getUtil().rest().<Page>get(documentReference).getContent());
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
        assertEquals("1.1", historySummary.getVersion());

        // ASSERT) The history in XWiki 2 should be the one set in XWiki 1
        getUtil().switchExecutor(2);
        assertEqualsHistorySizeWithTimeout(documentReference, 2);
        assertEquals("modified content", getUtil().rest().<Page>get(documentReference).getContent());
        history = getHistory(documentReference);
        historySummary = history.getHistorySummaries().get(0);
        assertEquals("2.1", historySummary.getVersion());
        historySummary = history.getHistorySummaries().get(1);
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

        // ASSERT) The document should not exist anymore on XWiki 2
        getUtil().switchExecutor(2);
        // Since it can take time for the replication to propagate the change, we need to wait and set up a timeout.
        assertDoesNotExistWithTimeout(documentReference);
    }

    private void replicateEmpty() throws Exception
    {
        Page page = new Page();
        page.setSpace(REPLICATION_REFERENCE.getParent().getName());
        page.setName("ReplicatedPage");

        LocalDocumentReference documentReference = new LocalDocumentReference(page.getSpace(), page.getName());

        // Clean any pre-existing
        getUtil().rest().delete(documentReference);

        ////////////////////////////////////
        // Page creation on XWiki 0
        ////////////////////////////////////

        // Edit a page on XWiki 0
        getUtil().switchExecutor(0);
        page.setContent("content");
        getUtil().rest().save(page);
        assertEquals("content", getUtil().rest().<Page>get(documentReference).getContent());

        // ASSERT) The page should exist but be empty on XWiki 1
        getUtil().switchExecutor(1);
        assertEqualsContentWithTimeout(documentReference,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "1.1", page.getVersion());

        // ASSERT) The page should exist but be empty on XWiki 2
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(documentReference,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "1.1", page.getVersion());

        ////////////////////////////////////
        // Edit on XWiki 0
        ////////////////////////////////////

        // Edit a page on XWiki 0
        getUtil().switchExecutor(0);
        page.setContent("modified content");
        getUtil().rest().save(page);
        assertEquals("modified content", getUtil().rest().<Page>get(documentReference).getContent());

        // ASSERT) That should not have any kind of impact on XWiki 1
        getUtil().switchExecutor(1);
        assertEqualsContentWithTimeout(documentReference,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "1.1", page.getVersion());

        // ASSERT) The page should exist but be empty on XWiki 2
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(documentReference,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        page = getUtil().rest().<Page>get(documentReference);
        assertEquals("Wrong version in the replicated document", "1.1", page.getVersion());

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

    private void setConfiguration(EntityReference reference, DocumentReplicationLevel level)
    {
        setConfiguration(reference, null, level);
    }

    private void setConfiguration(EntityReference reference, String instance, DocumentReplicationLevel level)
    {
        PageReplicationAdministrationSectionPage replicationPageAdmin =
            PageReplicationAdministrationSectionPage.gotoPage(reference);

        if (reference.getType() == EntityType.SPACE) {
            replicationPageAdmin.setSpaceLevel(instance, level);
        } else {
            replicationPageAdmin.setDocumentLevel(instance, level);
        }

        replicationPageAdmin.save();
    }

    private void changeController() throws Exception
    {
        EntityReference page1Space = new EntityReference("page1", EntityType.SPACE);
        LocalDocumentReference page1 = new LocalDocumentReference("WebHome", page1Space);
        EntityReference page1_1Space = new EntityReference("page1_1", EntityType.SPACE, page1Space);
        LocalDocumentReference page1_1 = new LocalDocumentReference("WebHome", page1_1Space);
        LocalDocumentReference page1_1_1 =
            new LocalDocumentReference("WebHome", new EntityReference("page1_1_1", EntityType.SPACE, page1_1Space));
        LocalDocumentReference page1_2 =
            new LocalDocumentReference("WebHome", new EntityReference("page1_2", EntityType.SPACE, page1Space));

        getUtil().rest().savePage(page1, "content1", "");
        getUtil().rest().savePage(page1_1, "content1_1", "");
        getUtil().rest().savePage(page1_1_1, "content1_1_1", "");
        getUtil().rest().savePage(page1_2, "content1_2", "");

        ////////////////////////////////////
        // Start ALL replication of page1.page1_1.WebHome
        ////////////////////////////////////

        // Set replication configuration
        getUtil().switchExecutor(1);
        setConfiguration(page1_1, DocumentReplicationLevel.ALL);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertEqualsContentWithTimeout(page1_1, "content1_1");
        assertDoesNotExistWithTimeout(page1_1_1);
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(page1_1, "content1_1");
        assertDoesNotExistWithTimeout(page1_1_1);

        ////////////////////////////////////
        // Switch to REFERENCE replication of page1.page1_1.WebHome
        ////////////////////////////////////

        // Set replication configuration
        getUtil().switchExecutor(1);
        setConfiguration(page1_1, DocumentReplicationLevel.REFERENCE);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertEqualsContentWithTimeout(page1_1,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        assertDoesNotExistWithTimeout(page1_1_1);
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(page1_1,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        assertDoesNotExistWithTimeout(page1_1_1);

        ////////////////////////////////////
        // STOP replication of page1_1 with all
        ////////////////////////////////////

        // Set replication configuration
        getUtil().switchExecutor(1);
        setConfiguration(page1_1, null);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertDoesNotExistWithTimeout(page1_1);
        assertDoesNotExistWithTimeout(page1_1_1);
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertDoesNotExistWithTimeout(page1_1);
        assertDoesNotExistWithTimeout(page1_1_1);

        ////////////////////////////////////
        // Start ALL replication of page1.page1 space
        ////////////////////////////////////

        // Set replication configuration
        getUtil().switchExecutor(1);
        setConfiguration(page1Space, DocumentReplicationLevel.ALL);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertEqualsContentWithTimeout(page1, "content1");
        assertDoesNotExistWithTimeout(page1_1);
        assertEqualsContentWithTimeout(page1_1_1, "content1_1_1");
        assertEqualsContentWithTimeout(page1_2, "content1_2");
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(page1, "content1");
        assertDoesNotExistWithTimeout(page1_1);
        assertEqualsContentWithTimeout(page1_1_1, "content1_1_1");
        assertEqualsContentWithTimeout(page1_2, "content1_2");

        ////////////////////////////////////
        // Switch to REFERENCE replication of page1.page1 space
        ////////////////////////////////////

        // Set replication configuration
        getUtil().switchExecutor(1);
        setConfiguration(page1Space, DocumentReplicationLevel.REFERENCE);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertEqualsContentWithTimeout(page1,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        assertDoesNotExistWithTimeout(page1_1);
        assertEqualsContentWithTimeout(page1_1_1,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        assertEqualsContentWithTimeout(page1_2,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(page1,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        assertDoesNotExistWithTimeout(page1_1);
        assertEqualsContentWithTimeout(page1_1_1,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");
        assertEqualsContentWithTimeout(page1_2,
            "{{warning}}{{translation key=\"replication.entity.level.REFERENCE.placeholder\"/}}{{/warning}}");

        ////////////////////////////////////
        // STOP replication of page1 with all
        ////////////////////////////////////

        // Set replication configuration
        getUtil().switchExecutor(1);
        setConfiguration(page1Space, null);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertDoesNotExistWithTimeout(page1);
        assertDoesNotExistWithTimeout(page1_1);
        assertDoesNotExistWithTimeout(page1_1_1);
        assertDoesNotExistWithTimeout(page1_2);
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertDoesNotExistWithTimeout(page1);
        assertDoesNotExistWithTimeout(page1_1);
        assertDoesNotExistWithTimeout(page1_1_1);
        assertDoesNotExistWithTimeout(page1_2);

        ////////////////////////////////////
        // Start ALL replication of page1.page1 space on XWIKI 0
        ////////////////////////////////////

        // Set replication configuration
        getUtil().switchExecutor(1);
        setConfiguration(page1Space, this.proxyURI0, DocumentReplicationLevel.ALL);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertEqualsContentWithTimeout(page1, "content1");
        assertDoesNotExistWithTimeout(page1_1);
        assertEqualsContentWithTimeout(page1_1_1, "content1_1_1");
        assertEqualsContentWithTimeout(page1_2, "content1_2");
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertDoesNotExistWithTimeout(page1);
        assertDoesNotExistWithTimeout(page1_1);
        assertDoesNotExistWithTimeout(page1_1_1);
        assertDoesNotExistWithTimeout(page1_2);

        ////////////////////////////////////
        // Start ALL replication of page1.page1 space on XWIKI 2
        ////////////////////////////////////

        getUtil().switchExecutor(1);
        setConfiguration(page1Space, this.proxyURI2, DocumentReplicationLevel.ALL);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertEqualsContentWithTimeout(page1, "content1");
        assertDoesNotExistWithTimeout(page1_1);
        assertEqualsContentWithTimeout(page1_1_1, "content1_1_1");
        assertEqualsContentWithTimeout(page1_2, "content1_2");
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(page1, "content1");
        assertDoesNotExistWithTimeout(page1_1);
        assertEqualsContentWithTimeout(page1_1_1, "content1_1_1");
        assertEqualsContentWithTimeout(page1_2, "content1_2");

        ////////////////////////////////////
        // Stop replication of page1.page1 space on XWIKI 0
        ////////////////////////////////////

        getUtil().switchExecutor(1);
        setConfiguration(page1Space, this.proxyURI0, null);

        // ASSERT) The content in XWiki 0 should be the one set in XWiki 1
        getUtil().switchExecutor(0);
        assertDoesNotExistWithTimeout(page1);
        assertDoesNotExistWithTimeout(page1_1);
        assertDoesNotExistWithTimeout(page1_1_1);
        assertDoesNotExistWithTimeout(page1_2);
        // ASSERT) The content in XWiki 2 should be the one set in XWiki 0
        getUtil().switchExecutor(2);
        assertEqualsContentWithTimeout(page1, "content1");
        assertDoesNotExistWithTimeout(page1_1);
        assertEqualsContentWithTimeout(page1_1_1, "content1_1_1");
        assertEqualsContentWithTimeout(page1_2, "content1_2");
    }

    private void network() throws Exception
    {
        Page page = new Page();
        page.setSpace("Network");
        page.setName("WebHome");

        LocalDocumentReference documentReference = new LocalDocumentReference(page.getSpace(), page.getName());

        // Clean any pre-existing
        getUtil().rest().delete(documentReference);

        // Stop the proxy in font of XWIKI 1
        this.proxy1.removeStub(this.proxyStub1);

        // Create a new page on XWIKI 0
        getUtil().switchExecutor(0);
        setConfiguration(documentReference, DocumentReplicationLevel.ALL);
        getUtil().rest().savePage(documentReference, "content", "");

        // Make sure the page is not replicated on XWIKI 1
        getUtil().switchExecutor(1);
        assertDoesNotExistWithTimeout(documentReference);

        this.proxy1.stubFor(this.proxyStubBuilder1);
    }
}
