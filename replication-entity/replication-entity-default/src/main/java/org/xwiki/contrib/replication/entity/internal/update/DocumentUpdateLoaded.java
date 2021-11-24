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
package org.xwiki.contrib.replication.entity.internal.update;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.xar.input.XARInputProperties;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;

/**
 * @version $Id$
 */
@Component(roles = DocumentUpdateLoaded.class)
@Singleton
public class DocumentUpdateLoaded
{
    @Inject
    // TODO: don't use internal tool
    private XWikiDocumentFilterUtils importer;

    /**
     * @param document the document to fill
     * @param stream the stream to parse
     * @throws FilterException when failing to import
     * @throws IOException when failing to import
     * @throws ComponentLookupException when failing to find a EntityOutputFilterStream corresponding to passed class
     */
    public void importDocument(XWikiDocument document, InputStream stream)
        throws FilterException, IOException, ComponentLookupException
    {
        // Output
        DocumentInstanceOutputProperties documentProperties = new DocumentInstanceOutputProperties();
        documentProperties.setDefaultReference(document.getDocumentReferenceWithLocale());

        // Input
        XARInputProperties xarProperties = new XARInputProperties();

        this.importer.importEntity(XWikiDocument.class, document, new DefaultInputStreamInputSource(stream),
            xarProperties, documentProperties);
    }
}
