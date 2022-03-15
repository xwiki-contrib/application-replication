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
package org.xwiki.contrib.replication.entity.internal.index;

/**
 * @version $Id$
 */
public class HibernateReplicationDocument
{
    private long docId;

    private String owner;

    HibernateReplicationDocument()
    {

    }

    HibernateReplicationDocument(long docId)
    {
        this.docId = docId;
    }

    /**
     * @param docId the identifier of the document
     * @param owner the owner instance of the document
     */
    public HibernateReplicationDocument(long docId, String owner)
    {
        this(docId);

        this.owner = owner;
    }

    /**
     * @return the identifier of the document
     */
    public long getDocId()
    {
        return this.docId;
    }

    /**
     * @param docid the identifier of the document
     */
    public void setDocId(long docid)
    {
        this.docId = docid;
    }

    /**
     * @return the owner instance of the document
     */
    public String getOwner()
    {
        return this.owner;
    }

    /**
     * @param owner the owner instance of the document
     */
    public void setOwner(String owner)
    {
        this.owner = owner;
    }
}
