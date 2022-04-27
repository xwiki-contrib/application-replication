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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.internal.AbstractEntityReplicationMessage;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * @version $Id$
 */
@Component(roles = DocumentUpdateReplicationMessage.class)
public class DocumentUpdateReplicationMessage extends AbstractEntityReplicationMessage<DocumentReference>
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
     * The name of the metadata containing the previous version of the entity in the message.
     */
    public static final String METADATA_ANCESTORS = METADATA_PREFIX + "ANCESTORS";

    /**
     * The name of the metadata containing the previous version of the entity in the message.
     */
    public static final String METADATA_COMPLETE = METADATA_PREFIX + "COMPLETE";

    /**
     * The name of the metadata containing the version of the entity in the message.
     */
    public static final String METADATA_VERSION = METADATA_PREFIX + "VERSION";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentRevisionProvider revisionProvider;

    @Inject
    private Logger logger;

    private String version;

    private boolean complete;

    private Set<String> attachments;

    /**
     * Initialize a message for a version replication.
     * 
     * @param document the the document to send
     * @param attachments the attachments content to send
     * @param metadata custom metadata to add to the message
     */
    public void initializeUpdate(XWikiDocument document, Set<String> attachments,
        Map<String, Collection<String>> metadata)
    {
        initialize(document.getDocumentReferenceWithLocale(), document.getVersion(), false, null, metadata);

        this.attachments = attachments;

        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            XWikiDocumentArchive archive = document.getDocumentArchive(xcontext);
            Collection<XWikiRCSNodeInfo> nodes = archive.getNodes();
            List<DocumentAncestor> ancestors = new ArrayList<>(nodes.size());
            for (XWikiRCSNodeInfo node : nodes) {
                String nodeVersion = node.getVersion().toString();
                if (!nodeVersion.equals(document.getVersion())) {
                    ancestors.add(new DocumentAncestor(nodeVersion, node.getDate()));
                }
            }
            this.metadata.put(METADATA_ANCESTORS, DocumentAncestorConverter.toStrings(ancestors));
        } catch (XWikiException e) {
            this.logger.error("Failed to get document ancestors", e);
        }

        this.metadata = Collections.unmodifiableMap(this.metadata);
    }

    /**
     * Initialize a message for a complete replication.
     * 
     * @param documentReference the reference of the document affected by this message
     * @param creator the user who created the document
     * @param version the version of the document
     * @param metadata custom metadata to add to the message
     */
    public void initializeComplete(DocumentReference documentReference, UserReference creator, String version,
        Map<String, Collection<String>> metadata)
    {
        initialize(documentReference, version, true, creator, metadata);

        this.metadata = Collections.unmodifiableMap(this.metadata);
    }

    protected void initialize(DocumentReference documentReference, String version, boolean complete,
        UserReference creator, Map<String, Collection<String>> metadata)
    {
        super.initialize(documentReference, metadata);

        this.complete = complete;

        this.version = version;
        putMetadata(METADATA_VERSION, this.version);

        putMetadata(METADATA_COMPLETE, this.complete);

        if (creator != null) {
            putMetadata(METADATA_CREATOR, creator);
        }
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(this.entityReference, xcontext);
        } catch (XWikiException e) {
            throw new IOException("Failed to get document to write with reference [" + this.entityReference + "]", e);
        }

        if (document.isNew()) {
            // TODO: try to find it in the recycle bin
        } else if (!document.getVersion().equals(this.version)) {
            // Get the right version from the history
            try {
                document = this.revisionProvider.getRevision(document, this.version);
            } catch (XWikiException e) {
                throw new IOException("Failed to get document with reference [" + this.entityReference
                    + "] and version [" + this.version + "]", e);
            }
        }

        // TODO: find out which attachments should be sent (which attachments versions are new compared to the previous
        // version)

        try {
            toXML(document, stream, this.complete);
        } catch (Exception e) {
            throw new IOException(String.format("Failed to serialize the document with reference [%s] and version [%s]",
                this.entityReference, this.version), e);
        }
    }

    private void toXML(XWikiDocument document, OutputStream output, boolean complete) throws XWikiException
    {
        // Input
        DocumentInstanceInputProperties documentProperties = new DocumentInstanceInputProperties();
        // XAR XML format only support JRCS for the document
        documentProperties.setWithRevisions(false);
        documentProperties.setWithJRCSRevisions(complete);
        // Indicate which attachment content to serialize
        documentProperties.setAttachmentsContent(this.attachments);
        // Use the revision format for attachment history since it's better from memory point of view
        documentProperties.setWithWikiAttachmentJRCSRevisions(false);
        documentProperties.setWithWikiAttachmentsRevisions(complete);

        // Output
        XAROutputProperties xarProperties = new XAROutputProperties();
        // Indicate the stream where you write the XAR XML
        xarProperties.setTarget(new DefaultOutputStreamOutputTarget(output));

        try {
            document.toXML(documentProperties, xarProperties);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_XWIKI_DOC_EXPORT,
                "Error serializin document to xml", e, null);
        }
    }
}
