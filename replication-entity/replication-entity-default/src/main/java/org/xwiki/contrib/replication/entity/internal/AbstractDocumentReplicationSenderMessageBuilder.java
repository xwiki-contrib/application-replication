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
package org.xwiki.contrib.replication.entity.internal;

import java.util.Collection;

import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.DocumentReplicationSenderMessageBuilder;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Helper to asynchronously build a document related message to send.
 * 
 * @version $Id$
 * @since 2.0.0
 */
public abstract class AbstractDocumentReplicationSenderMessageBuilder
    extends AbstractEntityReplicationSenderMessageBuilder implements DocumentReplicationSenderMessageBuilder
{
    protected final XWikiDocument document;

    protected final DocumentReference documentReference;

    protected String owner;

    protected Boolean conflict;

    protected Collection<String> conflictAuthors;

    /**
     * @param documentReference the reference of the existing document for which to send a message
     */
    AbstractDocumentReplicationSenderMessageBuilder(DocumentReference documentReference)
    {
        super(documentReference);

        this.documentReference = documentReference;
        this.document = null;
    }

    /**
     * @param document the document for which to send a message
     */
    AbstractDocumentReplicationSenderMessageBuilder(XWikiDocument document)
    {
        super(document.getDocumentReferenceWithLocale());

        this.documentReference = document.getDocumentReferenceWithLocale();
        this.document = document;
    }

    @Override
    public XWikiDocument getDocument()
    {
        return this.document;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public String getOwner()
    {
        return this.owner;
    }

    @Override
    public Boolean getConflict()
    {
        return this.conflict;
    }

    @Override
    public Collection<String> getConflictAuthors()
    {
        return this.conflictAuthors;
    }

    @Override
    public AbstractDocumentReplicationSenderMessageBuilder id(String id)
    {
        return (AbstractDocumentReplicationSenderMessageBuilder) super.id(id);
    }

    @Override
    public AbstractDocumentReplicationSenderMessageBuilder receivers(Collection<String> receivers)
    {
        return (AbstractDocumentReplicationSenderMessageBuilder) super.receivers(receivers);
    }

    @Override
    public AbstractDocumentReplicationSenderMessageBuilder receivers(String... receivers)
    {
        return (AbstractDocumentReplicationSenderMessageBuilder) super.receivers(receivers);
    }

    @Override
    public AbstractDocumentReplicationSenderMessageBuilder minimumLevel(DocumentReplicationLevel minimumLevel)
    {
        return (AbstractDocumentReplicationSenderMessageBuilder) super.minimumLevel(minimumLevel);
    }

    @Override
    public DocumentReplicationSenderMessageBuilder owner(String owner)
    {
        this.owner = owner;

        return this;
    }

    @Override
    public DocumentReplicationSenderMessageBuilder conflict(boolean conflict)
    {
        this.conflict = conflict;

        return this;
    }

    @Override
    public DocumentReplicationSenderMessageBuilder conflictAuthors(Collection<String> conflictAuthors)
    {
        conflict(true);

        this.conflictAuthors = conflictAuthors;

        return this;
    }
}
