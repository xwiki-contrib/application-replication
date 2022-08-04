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
package org.xwiki.contrib.replication.internal.instance;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.replication.ReplicationInstance.Status;
import org.xwiki.contrib.replication.ReplicationInstanceClassInitializer;
import org.xwiki.contrib.replication.internal.ReplicationConstants;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 */
@Component
@Named(StandardReplicationInstanceClassInitializer.CLASS_FULLNAME)
@Singleton
public class StandardReplicationInstanceClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the class defining the object which contains a Replication Instance metadata.
     */
    public static final String CLASS_NAME = "InstanceClass";

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
     * The name of the property containing the Replication Instance URI.
     */
    public static final String FIELD_URI = "uri";

    /**
     * The name of the property containing the Replication Instance display name.
     */
    public static final String FIELD_NAME = "name";

    /**
     * The name of the property containing the Replication Instance status.
     */
    public static final String FIELD_STATUS = "status";

    /**
     * The name of the property containing the Replication Instance public key.
     */
    public static final String FIELD_PUBLICKEY = "publicKey";

    @Inject
    private List<ReplicationInstanceClassInitializer> initializers;

    /**
     * Default constructor.
     */
    public StandardReplicationInstanceClassInitializer()
    {
        super(CLASS_REFERENCE, "Replication Instance Class");
    }

    @Override
    public boolean isMainWikiOnly()
    {
        // Initialize it only for the main wiki.
        return true;
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(FIELD_URI, "URI", 30);
        xclass.addTextField(FIELD_NAME, "Name", 30);
        xclass.addTextAreaField(FIELD_PUBLICKEY, "Public key", 30, 1);

        xclass.addStaticListField(FIELD_STATUS, "Status",
            Status.REGISTERED.name() + '|' + Status.REQUESTED.name() + '|' + Status.REQUESTING.name());

        // Extends the class
        this.initializers.forEach(i -> i.extendClass(xclass));
    }
}
