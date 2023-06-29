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
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 * @since 1.1
 */
public abstract class AbstractDocumentReplicationMessage extends AbstractEntityReplicationMessage<DocumentReference>
{
    @Override
    protected void initialize(DocumentReference documentReference, Collection<String> receivers,
        Map<String, Collection<String>> extraMetadata)
    {
        super.initialize(documentReference, receivers, extraMetadata);

        putCustomMetadata(METADATA_ENTITY_RECOVER_TYPE, VALUE_DOCUMENT_RECOVER_TYPE);
    }
}
