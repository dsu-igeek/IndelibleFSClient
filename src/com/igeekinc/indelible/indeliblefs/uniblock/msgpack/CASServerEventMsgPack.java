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

import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.EntityIDMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerEventType;
import com.igeekinc.indelible.indeliblefs.uniblock.CollectionCreatedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CollectionDestroyedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;

@Message
public class CASServerEventMsgPack
{
	public EntityIDMsgPack	source;
	public long	eventID;
	public long	timestamp;
	public byte eventType;
	public ObjectIDMsgPack collectionID;
	public IndelibleFSTransactionMsgPack transaction;

	public CASServerEventMsgPack()
	{
		// for message pack
	}
	
	public CASServerEventMsgPack(CASServerEvent event)
	{
		switch(event.getEventType())
		{
		case kCollectionCreated:
			initFromCollectionCreatedEvent((CollectionCreatedEvent)event);
			break;
		case kCollectionDestroyed:
			initFromCollectionDestroyedEvent((CollectionDestroyedEvent)event);
			break;
		default:
			break;

		}
	}

	public CASServerEventMsgPack(CollectionCreatedEvent event)
	{
		initFromCollectionCreatedEvent(event);
	}

	private void initFromCollectionCreatedEvent(CollectionCreatedEvent event)
	{
		this.eventType = (byte) event.getEventType().getEventType();
		this.source = new EntityIDMsgPack(event.getSource());
		this.eventID = event.getEventID();
		this.timestamp = event.getTimestamp();
		this.collectionID = new ObjectIDMsgPack(event.getCollectionID());
	}
	
	public CASServerEventMsgPack(CollectionDestroyedEvent event)
	{
		initFromCollectionDestroyedEvent(event);
	}

	private void initFromCollectionDestroyedEvent(CollectionDestroyedEvent event)
	{
		this.eventType = (byte) event.getEventType().getEventType();
		this.source = new EntityIDMsgPack(event.getSource());
		this.eventID = event.getEventID();
		this.timestamp = event.getTimestamp();
		this.collectionID = new ObjectIDMsgPack(event.getCollectionID());
	}
	
	public CASServerEventMsgPack(TransactionCommittedEvent event)
	{
		initFromTransactionCommittedEvent(event);
	}

	private void initFromTransactionCommittedEvent(TransactionCommittedEvent event)
	{
		this.eventType = (byte) event.getEventType().getEventType();
		this.source = new EntityIDMsgPack(event.getSource());
		this.eventID = event.getEventID();
		this.timestamp = event.getTimestamp();
		this.transaction = new IndelibleFSTransactionMsgPack(event.getTransaction());
	}
	
	public CASServerEvent getEvent()
	{
		CASServerEventType csEventType = CASServerEventType.eventTypeForChar((char)eventType);
		switch(csEventType)
		{
		case kCollectionCreated:
			return new CollectionCreatedEvent(source.getEntityID(), eventID, timestamp, (CASCollectionID)collectionID.getObjectID());
		case kCollectionDestroyed:
			return new CollectionDestroyedEvent(source.getEntityID(), eventID, timestamp, (CASCollectionID)collectionID.getObjectID());
		default:
			break;

		}
		throw new IllegalArgumentException("Unrecognized event type "+eventType);
	}
}
