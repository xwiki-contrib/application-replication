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

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.util.Util;

/**
 * Register the entity replication mapping.
 *
 * @version $Id: f9ed06c92322e4b3ba6505fc7b87ea7a142d246d $
 */
@Component
@Named("EntityReplicationInitializer")
@Singleton
public class EntityReplicationInitializer extends AbstractEventListener
{
    @Inject
    private HibernateSessionFactory sessionFactory;

    /**
     * Setup the listener.
     */
    public EntityReplicationInitializer()
    {
        super("EntityReplicationInitializer", new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.sessionFactory.getConfiguration().addInputStream(getMappingFile("entityreplication.hbm.xml"));
    }

    private InputStream getMappingFile(String mappingFileName)
    {
        InputStream resource = Util.getResourceAsStream(mappingFileName);

        // It could happen that the resource is not found in the file system, in the Servlet Context or in the current
        // Thread Context Classloader. In this case try to get it from the CL used to load this class since the default
        // mapping file is located in the same JAR that contains this code.
        // This can happen in the case when this JAR is installed as a root extension (and thus in a ClassLoader not
        // visible from the current Thread Context Classloader at the time when the ApplicationStartedEvent event
        // is sent (the thread context CL is set to the current wiki CL later on).
        if (resource == null) {
            resource = getClass().getClassLoader().getResourceAsStream(mappingFileName);
        }
        return resource;
    }
}
