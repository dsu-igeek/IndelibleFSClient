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

import java.io.IOException;

import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;

@Message
public class NextCollectionEventListItemsReply
{
	public static int	kMaxReturnEvents = CASCollectionEventIteratorReply.kMaxReturnEvents;
	public CASCollectionEventMsgPack [] events;
	public boolean hasMore;			// Has more after what's been returned in this message
	

	public NextCollectionEventListItemsReply()
	{
		// for message pack
	}
	
	public NextCollectionEventListItemsReply(IndelibleEvent [] versions, boolean hasMore) throws IOException
	{
		this.events = new CASCollectionEventMsgPack[versions.length];
		for (int eventNum = 0; eventNum < versions.length; eventNum++)
		{
			this.events[eventNum] = new CASCollectionEventMsgPack((CASCollectionEvent) versions[eventNum]);
		}
		this.hasMore = hasMore;
	}
	
	public IndelibleEvent [] getEvents()
	{
		IndelibleEvent[] returnEvents = new IndelibleEvent[events.length];
		for (int eventNum = 0; eventNum < events.length; eventNum++)
		{
			returnEvents[eventNum] = events[eventNum].getEvent();
		}
		return returnEvents;
	}
	
	public boolean hasMore()
	{
		return hasMore;
	}
}
