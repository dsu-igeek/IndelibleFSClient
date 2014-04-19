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

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;

public class SegmentInfo implements Serializable
{
	private static final long	serialVersionUID	= 2280231254105311527L;
	private CASIdentifier casID;
	private IndelibleVersion version;
	
	public SegmentInfo(CASIdentifier casID, IndelibleVersion version)
	{
		this.casID = casID;
		this.version = version;
	}

	public CASIdentifier getCasID()
	{
		return casID;
	}

	public IndelibleVersion getVersion()
	{
		return version;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((casID == null) ? 0 : casID.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		SegmentInfo other = (SegmentInfo) obj;
		if (casID == null)
		{
			if (other.casID != null)
				return false;
		} else if (!casID.equals(other.casID))
			return false;
		if (version == null)
		{
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

}
