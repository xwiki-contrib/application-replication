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

import org.xwiki.contrib.replication.ReplicationMessage;

/**
 * Replication messages impacting entities.
 * 
 * @version $Id$
 * @since 2.0.0
 */
public interface EntityReplicationMessage extends ReplicationMessage
{
    /**
     * The type of message supported by this receiver.
     */
    String PREFIX_TYPE_ENTITY = "entity_";

    /**
     * The prefix in front of all entity metadata properties.
     */
    String PREFIX_METADATA_ENTITY = PREFIX_TYPE_ENTITY.toUpperCase();

    /**
     * The name of the metadata containing the reference of the entity in the message.
     */
    String METADATA_ENTITY_REFERENCE = PREFIX_METADATA_ENTITY + "REFERENCE";

    /**
     * The name of the metadata containing the locale of the entity in the message.
     */
    String METADATA_ENTITY_LOCALE = PREFIX_METADATA_ENTITY + "LOCALE";

    /**
     * The name of the metadata containing the reference of the user in the context.
     */
    String METADATA_ENTITY_CONTEXT_USER = PREFIX_METADATA_ENTITY + "CONTEXT_USER";

    /**
     * The name of the metadata containing the creator of the document.
     */
    String METADATA_ENTITY_CREATOR = PREFIX_METADATA_ENTITY + "CREATOR";

    /**
     * The name of the metadata used to group various types of messages for recovering needs.
     */
    String METADATA_ENTITY_RECOVER_TYPE = PREFIX_METADATA_ENTITY + "RECOVER_TYPE";

    /**
     * The value used to group all document related messages.
     */
    String VALUE_DOCUMENT_RECOVER_TYPE = PREFIX_TYPE_ENTITY + "document";

    /**
     * The message type for document update messages.
     */
    String TYPE_DOCUMENT_UPDATE = PREFIX_TYPE_ENTITY + "update";

    /**
     * The prefix in front of all entity metadata related to document update messages.
     */
    String PREFIX_METADATA_DOCUMENT_UPDATE = TYPE_DOCUMENT_UPDATE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the previous version of the entity in the message.
     */
    String METADATA_DOCUMENT_UPDATE_ANCESTORS = PREFIX_METADATA_DOCUMENT_UPDATE + "ANCESTORS";

    /**
     * The name of the metadata containing the previous version of the entity in the message.
     */
    String METADATA_DOCUMENT_UPDATE_COMPLETE = PREFIX_METADATA_DOCUMENT_UPDATE + "COMPLETE";

    /**
     * The name of the metadata containing the version of the entity in the message.
     */
    String METADATA_DOCUMENT_UPDATE_VERSION = PREFIX_METADATA_DOCUMENT_UPDATE + "VERSION";

    /**
     * The name of the metadata indicating of the document update is readonly.
     */
    String METADATA_DOCUMENT_UPDATE_READONLY = PREFIX_METADATA_DOCUMENT_UPDATE + "READONLY";

    /**
     * The name of the metadata containing the owner of the document.
     */
    String METADATA_DOCUMENT_UPDATE_OWNER = PREFIX_METADATA_DOCUMENT_UPDATE + "OWNER";

    /**
     * The message type for these messages.
     */
    String TYPE_DOCUMENT_DELETE = PREFIX_TYPE_ENTITY + "delete";

    /**
     * The prefix in front of all entity metadata properties.
     */
    String PREFIX_METADATA_DOCUMENT_DELETE = TYPE_DOCUMENT_DELETE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the identifier of the batch the delete is part of.
     */
    String METADATA_DOCUMENT_DELETE_BATCH = PREFIX_METADATA_DOCUMENT_DELETE + "BATCH";

    /**
     * The message type for these messages.
     */
    String TYPE_DOCUMENT_HISTORYDELETE = PREFIX_TYPE_ENTITY + "_history";

    /**
     * The prefix in front of all entity metadata properties.
     */
    String PREFIX_METADATA_DOCUMENT_HISTORYDELETE = TYPE_DOCUMENT_HISTORYDELETE.toUpperCase() + '_';

    /**
     * The name of the metadata containing the lowest version to delete from the document history.
     */
    String METADATA_DOCUMENT_HISTORYDELETE_VERSION_FROM = PREFIX_METADATA_DOCUMENT_HISTORYDELETE + "VERSION_FROM";

    /**
     * The name of the metadata containing the highest version to delete from the document history.
     */
    String METADATA_DOCUMENT_HISTORYDELETE_VERSION_TO = PREFIX_METADATA_DOCUMENT_HISTORYDELETE + "VERSION_TO";

    /**
     * The message type for these messages.
     */
    String TYPE_DOCUMENT_REFERENCE = PREFIX_TYPE_ENTITY + "reference";

    /**
     * The prefix in front of all entity metadata properties.
     */
    String PREFIX_METADATA_DOCUMENT_REFERENCE = TYPE_DOCUMENT_REFERENCE.toUpperCase() + '_';

    /**
     * The name of the metadata indicating if the document just started to be replicated.
     */
    String METADATA_DOCUMENT_REFERENCE_CREATE = PREFIX_METADATA_DOCUMENT_REFERENCE + "CREATE";

    /**
     * The message type for repair request messages.
     */
    String TYPE_DOCUMENT_REPAIRREQUEST = PREFIX_TYPE_ENTITY + "repairrequest";

    /**
     * The prefix in front of all the document repair request metadata properties.
     */
    String PREFIX_METADATA_DOCUMENT_REPAIRREQUEST = TYPE_DOCUMENT_REPAIRREQUEST.toUpperCase() + '_';

    /**
     * The name of the metadata indicating if the repair should been sent back to the source only or all instances.
     */
    String METADATA_DOCUMENT_REPAIRREQUEST_SOURCE = PREFIX_METADATA_DOCUMENT_REPAIRREQUEST + "SOURCE";

    /**
     * The message type for these messages.
     */
    String TYPE_DOCUMENT_CONFLICT = PREFIX_TYPE_ENTITY + "conflict";

    /**
     * The prefix in front of all entity metadata properties.
     */
    String PREFIX_METADATA_DOCUMENT_CONFLICT = TYPE_DOCUMENT_CONFLICT.toUpperCase() + '_';

    /**
     * The name of the metadata indicating if the conflict marker should be set or removed.
     */
    String METADATA_DOCUMENT_CONFLICT = PREFIX_METADATA_DOCUMENT_CONFLICT + "CONFLICT";

    /**
     * The name of the metadata containing the authors involved in the conflict.
     */
    String METADATA_DOCUMENT_CONFLICT_AUTHORS = PREFIX_METADATA_DOCUMENT_CONFLICT + "AUTHORS";

    /**
     * The message type for these messages.
     */
    String TYPE_DOCUMENT_UNREPLICATE = PREFIX_TYPE_ENTITY + "unreplicate";

    /**
     * The message type for these messages.
     */
    String TYPE_LIKE = PREFIX_TYPE_ENTITY + "like";

    /**
     * The prefix in front of all entity metadata properties.
     */
    String PREFIX_METADATA_LIKE = TYPE_LIKE.toUpperCase() + '_';

    /**
     * The name of the metadata indicating if it's a like or an unlike.
     */
    String METADATA_LIKE = PREFIX_METADATA_LIKE + "LIKE";
}
