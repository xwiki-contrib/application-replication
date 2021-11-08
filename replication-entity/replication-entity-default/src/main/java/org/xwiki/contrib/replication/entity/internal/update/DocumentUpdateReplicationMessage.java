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
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.entity.internal.AbstractEntityReplicationMessage;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

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
    public static final String METADATA_PREVIOUSVERSION = METADATA_PREFIX + "PREVIOUSVERSION";

    /**
     * The name of the metadata containing the date of the previous version of the entity in the message.
     */
    public static final String METADATA_PREVIOUSVERSION_DATE = METADATA_PREFIX + "PREVIOUSVERSION_DATE";

    /**
     * The name of the metadata containing the previous version of the entity in the message.
     */
    public static final String METADATA_COMPLETE = METADATA_PREFIX + "COMPLETE";

    /**
     * The name of the metadata containing the creator of the document.
     */
    public static final String METADATA_CREATOR = METADATA_PREFIX + "CREATOR";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentRevisionProvider revisionProvider;

    private String version;

    private boolean complete;

    private Set<String> attachments;

    /**
     * Initialize a message for a version replication.
     * 
     * @param documentReference the reference of the document affected by this message
     * @param version the version of the document
     * @param previousVersion the previous version of the document
     * @param previousVersionDate the date of the previous version of the document
     * @param attachments the attachments content to send
     */
    public void initialize(DocumentReference documentReference, String version, String previousVersion,
        Date previousVersionDate, Set<String> attachments)
    {
        initialize(documentReference, version, false);

        this.attachments = attachments;

        putMetadata(METADATA_PREVIOUSVERSION, previousVersion);
        putMetadata(METADATA_PREVIOUSVERSION_DATE, previousVersionDate);

        this.metadata = Collections.unmodifiableMap(this.metadata);
    }

    /**
     * Initialize a message for a complete replication.
     * 
     * @param documentReference the reference of the document affected by this message
     * @param version the version of the document
     * @param creator the user who created the document
     */
    public void initialize(DocumentReference documentReference, DocumentReference creator, String version)
    {
        initialize(documentReference, version, true);

        putMetadata(METADATA_CREATOR, creator);

        this.metadata = Collections.unmodifiableMap(this.metadata);
    }

    private void initialize(DocumentReference documentReference, String version, boolean complete)
    {
        super.initialize(documentReference);

        this.complete = complete;

        this.version = version;

        putMetadata(METADATA_COMPLETE, this.complete);
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
