/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.indelible.indeliblefs.core;

import java.io.Serializable;

import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;

/**
 * IndelibleVersion uniquely identifies a version within an Indelible FS.
 * The IndelibleVersion is externally referred to by the 64 bit timestamp + 32 bit uniquifier.
 * Internally, the version is identified by the 64 bit transaction ID
 * The transaction ID is assigned by the IndelibleFSManager at the start of a transaction.  Timestamp & uniquifier are assigned when the transaction
 * completes.
 * Do not use the transaction ID to identify a version outside of the server internals!
 * 
 * When a transaction is started, a transaction ID is allocated.  An IndelibleVersion object is created with a transaction ID only.  The
 * versionTime and uniquifier are both set to -1.  If an attempt is made to retrieve the versionTime or uniquifier before the transaction
 * completes an error will be thrown.  After the transaction has been completed, the versionTime and uniquifier will be set.  Generally, when
 * the IndelibleVersion is stored to disk the versionTime and uniquifier will still be -1.  These are not marked transient because the object can
 * be serialized and sent across the network after the versionTime and uniquifier have been set.  The versionTime and uniquifier are loaded on demand
 * from the version manager if they are not set but the transaction ID has been committed.
 * @author David L. Smith-Uchida
 */
public class IndelibleVersion implements Serializable
{
    private static final long serialVersionUID = 7310059182792116750L;
    private transient CASServer versionManager;
    private transient CASServerConnectionIF connection;
    private transient long versionID;
    private long versionTime;
    private int uniquifier;
    
    public static final IndelibleVersion kLatestVersion = new IndelibleVersion(-1, Long.MAX_VALUE, Integer.MAX_VALUE);
    
    public IndelibleVersion(CASServer versionManager, CASServerConnectionIF connection, long versionID)
    {
        this.versionManager = versionManager;
        this.connection = connection;
        this.versionID = versionID;
        versionTime = Long.MAX_VALUE;
        uniquifier = Integer.MAX_VALUE;
    }
    
    public IndelibleVersion(long versionID, long versionTime, int uniquifier)
    {
        this.versionID = versionID;
        this.versionTime = versionTime;
        this.uniquifier = uniquifier;
    }

    public long getVersionTime()
    {
        if (versionTime < 0)
            loadTime();
        return versionTime;
    }

    public int getUniquifier()
    {
        if (versionTime < 0)
            loadTime();
        return uniquifier;
    }

    public long getVersionID()
    {
        return versionID;
    }

    private void loadTime()
    {
        versionManager.loadTime(connection, this);
    }
    
    // TODO - these should be private/protected eventually
    public void setVersionTime(long versionTime)
    {
        this.versionTime = versionTime;
    }

    public void setUniquifier(int uniquifier)
    {
        this.uniquifier = uniquifier;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + uniquifier;
        result = prime * result + (int) (versionTime ^ (versionTime >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IndelibleVersion other = (IndelibleVersion) obj;
        if (uniquifier != other.uniquifier)
            return false;
        if (versionTime != other.versionTime)
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "IndelibleVersion [uniquifier=" + uniquifier + ", versionTime="
                + versionTime + "]";
    }
    
    public boolean isFinal()
    {
    	return versionID >= 0 && versionTime != Long.MAX_VALUE;
    }
}
