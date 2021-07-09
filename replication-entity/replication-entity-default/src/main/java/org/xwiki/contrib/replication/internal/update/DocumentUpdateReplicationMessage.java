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
package org.xwiki.contrib.replication.internal.update;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.internal.AbstractDocumentReplicationMessage;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = DocumentUpdateReplicationMessage.class)
public class DocumentUpdateReplicationMessage extends AbstractDocumentReplicationMessage
{
    /**
     * The message type for these messages.
     */
    public static final String TYPE = TYPE_PREFIX + "update";

    /**
     * The prefix in front of all entity metadata properties.
     */
    public static final String METADATA_PREFIX = TYPE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the version of the entity in the message.
     */
    public static final String METADATA_VERSION = METADATA_PREFIX + "VERSION";

    /**
     * The name of the metadata containing the previous version of the entity in the message.
     */
    public static final String METADATA_PREVIOUSVERSION = METADATA_PREFIX + "PREVIOUSVERSION";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private String version;

    private String id;

    /**
     * @param documentReference the reference of the document affected by this message
     * @param version the version of the document
     * @param previousVersion the previous version of the document
     */
    public void initialize(DocumentReference documentReference, String version, String previousVersion)
    {
        initialize(documentReference);

        this.version = version;

        this.metadata.put(METADATA_VERSION, Collections.singleton(version));
        this.metadata.put(METADATA_PREVIOUSVERSION, Collections.singleton(previousVersion));

        this.id += '/' + this.version;

        this.metadata = Collections.unmodifiableMap(this.metadata);
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(this.documentReference, xcontext);
        } catch (XWikiException e) {
            throw new IOException("Failed to get document to write", e);
        }

        if (document.isNew()) {
            // TODO: try to find it in the archives
        } else if (!document.getVersion().equals(this.version)) {
            // TODO: get the right version
        }

        try {
            document.toXML(stream, true, false, false, false, xcontext);
        } catch (Exception e) {
            throw new IOException("Failed to write document to write", e);
        }
    }
}
