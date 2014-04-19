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
 
package com.igeekinc.indelible.indeliblefs.uniblock.exceptions;

import com.igeekinc.indelible.indeliblefs.exceptions.IndelibleFSException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;

public class SegmentNotFound extends IndelibleFSException
{
	private static final long	serialVersionUID	= 7915943462884514561L;
	private CASIdentifier segmentID;
	
	public SegmentNotFound(CASIdentifier segmentID)
	{
		this.segmentID = segmentID;
	}
	
	public CASIdentifier getSegmentID()
	{
		return segmentID;
	}

	@Override
	public String getMessage()
	{
		return "Could not find segment "+segmentID.getHashID()+", length = "+segmentID.getSegmentLength();
	}
}
