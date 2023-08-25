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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.entity.DocumentReplicationSenderMessageBuilder;
import org.xwiki.contrib.replication.entity.EntityReplication;
import org.xwiki.contrib.replication.entity.internal.AbstractDocumentReplicationMessage;
import org.xwiki.contrib.replication.entity.internal.EntityReplicationConfiguration;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.filter.xar.output.XAROutputProperties;
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
public class DocumentUpdateReplicationMessage extends AbstractDocumentReplicationMessage
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentRevisionProvider revisionProvider;

    @Inject
    private EntityReplicationConfiguration configuration;

    @Inject
    private EntityReplication entityReplication;

    @Inject
    private Logger logger;

    private String version;

    private boolean complete;

    private Set<String> attachments;

    /**
     * Initialize a message for a version replication.
     * 
     * @param builder the builder used to produce the message
     * @param document the the document to send
     * @param readonly true if the document update is readonly
     * @param attachments the attachments content to send
     * @param extraMetadata custom metadata to add to the message
     * @throws ReplicationException when failing to create the message
     * @since 2.0.0
     */
    public void initializePartial(DocumentReplicationSenderMessageBuilder builder, XWikiDocument document,
        Boolean readonly, Set<String> attachments, Map<String, Collection<String>> extraMetadata)
        throws ReplicationException
    {
        initialize(builder, document, readonly, false, extraMetadata);

        this.attachments = attachments;

        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            XWikiDocumentArchive archive = document.getDocumentArchive(xcontext);
            Collection<XWikiRCSNodeInfo> nodes = archive.getNodes();
            int maxCount = this.configuration.getDocumentAncestorMaxCount();
            List<DocumentAncestor> ancestors = new ArrayList<>(Integer.min(maxCount, nodes.size()));
            for (XWikiRCSNodeInfo node : nodes) {
                String nodeVersion = node.getVersion().toString();
                if (!nodeVersion.equals(document.getVersion())) {
                    ancestors.add(new DocumentAncestor(nodeVersion, node.getDate()));
                }
                if (ancestors.size() == maxCount) {
                    break;
                }
            }
            putCustomMetadata(METADATA_DOCUMENT_UPDATE_ANCESTORS, DocumentAncestorConverter.toStrings(ancestors));
        } catch (XWikiException e) {
            this.logger.error("Failed to get document ancestors", e);
        }
    }

    /**
     * Initialize a message for a complete replication.
     * 
     * @param builder the builder used to produce the message
     * @param document the document affected by this message
     * @param readonly true if the document update is readonly
     * @param extraMetadata custom metadata to add to the message
     * @throws ReplicationException when failing to create the message
     * @since 2.0.0
     */
    public void initializeComplete(DocumentReplicationSenderMessageBuilder builder, XWikiDocument document,
        Boolean readonly, Map<String, Collection<String>> extraMetadata) throws ReplicationException
    {
        initialize(builder, document, readonly, true, extraMetadata);
    }

    /**
     * Initialize a message for a complete replication.
     * 
     * @param builder the builder used to produce the message
     * @param document the document affected by this message
     * @param readonly true if the document update is readonly
     * @param complete true if it's a creation
     * @param extraMetadata custom metadata to add to the message
     * @throws ReplicationException when failing to create the message
     * @since 2.0.0
     */
    protected void initialize(DocumentReplicationSenderMessageBuilder builder, XWikiDocument document, Boolean readonly,
        boolean complete, Map<String, Collection<String>> extraMetadata) throws ReplicationException
    {
        super.initialize(builder, extraMetadata);

        if (readonly == Boolean.TRUE) {
            putCustomMetadata(METADATA_DOCUMENT_UPDATE_READONLY, readonly);
        }

        this.complete = complete;

        this.version = document.getVersion();
        putCustomMetadata(METADATA_DOCUMENT_UPDATE_VERSION, this.version);

        putCustomMetadata(METADATA_DOCUMENT_UPDATE_COMPLETE, this.complete);

        UserReference creator = document.getAuthors().getCreator();
        if (creator != null) {
            putCustomMetadata(METADATA_ENTITY_CREATOR, creator);
        }

        // Owner
        putCustomMetadata(METADATA_DOCUMENT_UPDATE_OWNER,
            this.entityReplication.getOwner(builder.getDocumentReference()));
    }

    @Override
    public String getType()
    {
        return TYPE_DOCUMENT_UPDATE;
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(this.entityReference, xcontext);
        } catch (XWikiException e) {
            throw new IOException(String.format("Failed to get document with reference [%s]", this.entityReference), e);
        }

        if (document.isNew()) {
            // TODO: try to find it in the recycle bin
        } else if (!document.getVersion().equals(this.version)) {
            // Get the right version from the history
            try {
                document = this.revisionProvider.getRevision(document, this.version);
            } catch (XWikiException e) {
                throw new IOException(String.format("Failed to get document with reference [%s] and version [%s]",
                    this.entityReference, this.version), e);
            }

            if (document == null) {
                throw new IOException(String.format("No document with reference [%s] and version [%s] could found",
                    this.entityReference, this.version));
            }
        }

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
                "Error serializing document to XML", e, null);
        }
    }
}
