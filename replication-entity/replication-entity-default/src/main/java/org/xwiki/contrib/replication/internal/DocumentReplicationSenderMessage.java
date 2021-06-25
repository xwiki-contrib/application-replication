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
package org.xwiki.contrib.replication.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = DocumentReplicationSenderMessage.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentReplicationSenderMessage implements ReplicationSenderMessage
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    @Named("uid")
    private EntityReferenceSerializer<String> uidSerializer;

    private DocumentReference documentReference;

    private String version;

    private Date date;

    private Map<String, Collection<String>> metadata;

    private String id;

    /**
     * @param documentReference the reference of the document affected by this message
     * @param version the version of the document
     * @param previousVersion the previous version of the document
     * @param date the date of the modification
     */
    public void initialize(DocumentReference documentReference, String version, String previousVersion, Date date)
    {
        this.documentReference = documentReference;
        this.version = version;
        this.date = date;

        this.metadata = new HashMap<>();
        this.metadata.put(DocumentReplicationReceiver.METADATA_TYPE,
            Collections.singleton(DocumentReplicationReceiver.TYPE_DOCUMENT));
        this.metadata.put(DocumentReplicationReceiver.METADATA_REFERENCE,
            Collections.singleton(this.localSerializer.serialize(documentReference)));
        this.metadata.put(DocumentReplicationReceiver.METADATA_LOCALE,
            Collections.singleton(documentReference.getLocale().toString()));
        this.metadata.put(DocumentReplicationReceiver.METADATA_VERSION, Collections.singleton(version));
        this.metadata.put(DocumentReplicationReceiver.METADATA_PREVIOUSVERSION, Collections.singleton(previousVersion));

        this.id = getDate().getTime() + '/' + this.version + '/' + this.uidSerializer.serialize(documentReference);

        this.metadata = Collections.unmodifiableMap(this.metadata);
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public Date getDate()
    {
        return this.date;
    }

    @Override
    public ReplicationInstance getSource()
    {
        // Will be filled by the sender
        return null;
    }

    @Override
    public String getType()
    {
        return DocumentReplicationReceiver.TYPE;
    }

    @Override
    public Map<String, Collection<String>> getCustomMetadata()
    {
        return this.metadata;
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
