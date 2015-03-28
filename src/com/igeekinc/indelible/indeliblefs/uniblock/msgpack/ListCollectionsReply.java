/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igeekinc.indelible.indeliblefs.uniblock.msgpack;

import org.msgpack.annotation.Message;

import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;

@Message
public class ListCollectionsReply
{
	public ObjectIDMsgPack [] collectionIDs;
	
	public ListCollectionsReply()
	{
		// for message pack
	}
	
	public ListCollectionsReply(CASCollectionID [] collectionIDs)
	{
		this.collectionIDs = new ObjectIDMsgPack[collectionIDs.length];
		for (int curCollectionIDNum = 0; curCollectionIDNum < collectionIDs.length; curCollectionIDNum++)
		{
			this.collectionIDs[curCollectionIDNum] = new ObjectIDMsgPack(collectionIDs[curCollectionIDNum]);
		}
	}
	
	public CASCollectionID [] getCollectionIDs()
	{
		CASCollectionID [] returnIDs = new CASCollectionID[collectionIDs.length];
		for (int curCollectionIDNum = 0; curCollectionIDNum < collectionIDs.length; curCollectionIDNum++)
		{
			returnIDs[curCollectionIDNum] = (CASCollectionID) collectionIDs[curCollectionIDNum].getObjectID();
		}
		return returnIDs;
	}
}
