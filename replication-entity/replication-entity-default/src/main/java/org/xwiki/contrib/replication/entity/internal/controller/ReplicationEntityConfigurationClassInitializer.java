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
package org.xwiki.contrib.replication.entity.internal.controller;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.internal.ReplicationConstants;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 */
@Component
@Named(ReplicationEntityConfigurationClassInitializer.CLASS_FULLNAME)
@Singleton
public class ReplicationEntityConfigurationClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the class defining the object which contains a Replication Entity configuration.
     */
    public static final String CLASS_NAME = "EntityConfigurationClass";

    /**
     * The String reference of the class defining the object which contains a Replication Instance metadata.
     */
    public static final String CLASS_FULLNAME = ReplicationConstants.REPLICATION_HOME_STRING + '.' + CLASS_NAME;

    /**
     * The reference of the class defining the object which contains a Replication Instance metadata.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(CLASS_NAME, ReplicationConstants.REPLICATION_HOME);

    /**
     * The name of the property containing the Replication Entity controller to use.
     */
    public static final String FIELD_CONTROLLER = "controller";

    /**
     * Default constructor.
     */
    public ReplicationEntityConfigurationClassInitializer()
    {
        super(CLASS_REFERENCE, "Replication Entity Configuration Class");
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(FIELD_CONTROLLER, "Controller", 30);
    }
}
