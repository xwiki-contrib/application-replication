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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationMessageReader;
import org.xwiki.contrib.replication.message.log.ReplicationMessageEventInitializer;
import org.xwiki.eventstream.Event;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * @version $Id$
 */
@Component
@Named("entity")
@Singleton
public class EntityReplicationMessageEventInitializer implements ReplicationMessageEventInitializer
{
    @Inject
    private DocumentReplicationMessageReader documentMessageTool;

    @Inject
    private Logger logger;

    @Override
    public void initialize(ReplicationMessage message, Event event)
    {
        setRelatedEntity(message, event);
        setUser(message, event);
    }

    private void setUser(ReplicationMessage message, Event event)
    {
        try {
            event.setUser(this.documentMessageTool.getContextUser(message));
        } catch (InvalidReplicationMessageException e) {
            // Should never happen since it's not mandatory
            this.logger.error("Failed to extract the context user from the message with id [{}] and type [{}]",
                message.getId(), message.getType(), e);
        }
    }

    private void setRelatedEntity(ReplicationMessage message, Event event)
    {
        try {
            EntityReference entityReference = this.documentMessageTool.getEntityReference(message, false);

            if (entityReference != null) {
                event.setRelatedEntity(entityReference);

                setWiki(message, entityReference, event);
            }
        } catch (InvalidReplicationMessageException e) {
            // Should never happen since it's not mandatory
            this.logger.error("Failed to extract the entity from the message with id [{}] and type [{}]",
                message.getId(), message.getType(), e);
        }
    }

    private void setWiki(ReplicationMessage message, EntityReference entityReference, Event event)
        throws InvalidReplicationMessageException
    {
        EntityReference wikiReference = entityReference.extractReference(EntityType.WIKI);
        if (wikiReference != null) {
            event.setWiki(new WikiReference(wikiReference));

            EntityReference spaceReference = entityReference.extractReference(EntityType.SPACE);
            if (spaceReference != null) {
                event.setSpace(new SpaceReference(spaceReference));

                setDocument(message, entityReference, event);
            }
        }
    }

    private void setDocument(ReplicationMessage message, EntityReference entityReference, Event event)
        throws InvalidReplicationMessageException
    {
        EntityReference documentReference = entityReference.extractReference(EntityType.DOCUMENT);
        if (documentReference != null) {
            event.setDocument(new DocumentReference(documentReference));

            String version = this.documentMessageTool.getDocumentVersion(message);
            if (version != null) {
                event.setDocumentVersion(version);
            }
        }
    }
}
