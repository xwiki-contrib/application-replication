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
package org.xwiki.contrib.replication.entity;

import java.util.Collection;

import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Helper to asynchronously build a document related message to send.
 * 
 * @version $Id$
 * @since 2.0.0
 */
public interface DocumentReplicationSenderMessageBuilder extends EntityReplicationSenderMessageBuilder
{
    @Override
    DocumentReplicationSenderMessageBuilder id(String id);

    @Override
    DocumentReplicationSenderMessageBuilder receivers(Collection<String> receivers);

    @Override
    DocumentReplicationSenderMessageBuilder receivers(String... receivers);

    @Override
    DocumentReplicationSenderMessageBuilder minimumLevel(DocumentReplicationLevel minimumLevel);

    /**
     * @param owner the owner of the document
     * @return this builder
     */
    DocumentReplicationSenderMessageBuilder owner(String owner);

    /**
     * @param conflict true if the document should be marked in conflict
     * @return this builder
     */
    DocumentReplicationSenderMessageBuilder conflict(boolean conflict);

    /**
     * @param conflictAuthors the users involved in the conflict
     * @return this builder
     */
    DocumentReplicationSenderMessageBuilder conflictAuthors(Collection<String> conflictAuthors);

    /**
     * @return the document for which to send a message
     */
    XWikiDocument getDocument();

    /**
     * @return the reference of the existing document for which to send a message
     */
    DocumentReference getDocumentReference();

    /**
     * @return the owner of the document
     */
    String getOwner();

    /**
     * @return true if the document should be considered in conflict
     */
    Boolean getConflict();

    /**
     * @return the users involved in the conflict
     */
    Collection<String> getConflictAuthors();
}
