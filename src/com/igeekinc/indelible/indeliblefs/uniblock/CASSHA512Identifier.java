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
 
package com.igeekinc.indelible.indeliblefs.uniblock;

import java.io.Serializable;

import com.igeekinc.util.SHA512HashID;

public class CASSHA512Identifier implements Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -7950267721762173105L;
	private SHA512HashID hashID;
    private long segmentLength;
    private transient String stringVersion;
    /**
     * 
     */
    public CASSHA512Identifier(SHA512HashID inHashID, long inSegmentLength)
    {
        hashID = inHashID;
        segmentLength = inSegmentLength;
        stringVersion = null;
    }
    
    

    public long getSegmentLength()
    {
        return segmentLength;
    }
    public SHA512HashID getHashID()
    {
        return hashID;
    }

	public String toString()
    {
        if (stringVersion == null)
        {
            StringBuffer returnStringBuffer = new StringBuffer(128+4);
            returnStringBuffer.append(hashID.toString());
            stringVersion = returnStringBuffer.toString();
        }
        return stringVersion;
    }

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hashID == null) ? 0 : hashID.hashCode());
		result = prime * result
				+ (int) (segmentLength ^ (segmentLength >>> 32));
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
		CASSHA512Identifier other = (CASSHA512Identifier) obj;
		if (hashID == null)
		{
			if (other.hashID != null)
				return false;
		} else if (!hashID.equals(other.hashID))
			return false;
		if (segmentLength != other.segmentLength)
			return false;
		return true;
	}
}
