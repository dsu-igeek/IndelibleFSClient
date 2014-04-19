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
 
package com.igeekinc.indelible.indeliblefs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public class IndelibleNodeInfo implements Serializable
{
	private static final long	serialVersionUID	= 4907924554264809265L;
	private IndelibleFSObjectID nodeID;
	private String name;
	private HashMap<String, HashMap<String, Object>> metaData;
	
	public IndelibleNodeInfo(IndelibleFSObjectID nodeID, String name, HashMap<String, HashMap<String, Object>> metaData)
	{
		this.nodeID = nodeID;
		this.name = name;
		this.metaData = metaData;
	}

	public IndelibleFSObjectID getNodeID()
	{
		return nodeID;
	}
	
	public String getName()
	{
		return name;
	}
	
	public HashMap<String, Object>getMetaData(String mdName)
	{
		return metaData.get(mdName);
	}
	
	public String [] listMetaData()
	{
		Set<String>keySet = metaData.keySet();
		String [] returnNames = new String[keySet.size()];
		returnNames = keySet.toArray(returnNames);
		return returnNames;
	}
}
