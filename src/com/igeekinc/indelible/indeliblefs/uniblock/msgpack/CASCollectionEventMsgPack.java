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
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEventType;
import com.igeekinc.indelible.indeliblefs.uniblock.MetadataModifiedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.SegmentCreatedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.SegmentReleasedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;

@Message
public class CASCollectionEventMsgPack
{
	public EntityIDMsgPack	source;
	public long	eventID;
	public long	timestamp;
	public byte eventType;
	public ObjectIDMsgPack segmentID;
	public IndelibleFSTransactionMsgPack transaction;
	
	public CASCollectionEventMsgPack()
	{
		// for message pack
	}
	
	public CASCollectionEventMsgPack(CASCollectionEvent replicateEvent)
	{
		switch(replicateEvent.getEventType())
		{
		case kMetadataModified:
			initFromMetadataModifiedEvent((MetadataModifiedEvent)replicateEvent);
			break;
		case kSegmentCreated:
			initFromSegmentCreatedEvent((SegmentCreatedEvent)replicateEvent);
			break;
		case kSegmentReleased:
			initFromSegmentReleasedEvent((SegmentReleasedEvent)replicateEvent);
			break;
		case kTransactionCommited:
			initFromTransactionCommittedEvent((TransactionCommittedEvent)replicateEvent);
			break;
		default:
			break;
		
		}
	}

	public CASCollectionEventMsgPack(SegmentCreatedEvent event)
	{
		initFromSegmentCreatedEvent(event);
	}

	private void initFromSegmentCreatedEvent(SegmentCreatedEvent event)
	{
		this.eventType = (byte) event.getEventType().getEventType();
		this.source = new EntityIDMsgPack(event.getSource());
		this.eventID = event.getEventID();
		this.timestamp = event.getTimestamp();
		this.segmentID = new ObjectIDMsgPack(event.getSegmentID());
	}
	
	public CASCollectionEventMsgPack(SegmentReleasedEvent event)
	{
		initFromSegmentReleasedEvent(event);
	}

	private void initFromSegmentReleasedEvent(SegmentReleasedEvent event)
	{
		this.eventType = (byte) event.getEventType().getEventType();
		this.source = new EntityIDMsgPack(event.getSource());
		this.eventID = event.getEventID();
		this.timestamp = event.getTimestamp();
		this.segmentID = new ObjectIDMsgPack(event.getSegmentID());
	}
	
	public CASCollectionEventMsgPack(MetadataModifiedEvent event)
	{
		initFromMetadataModifiedEvent(event);
	}

	private void initFromMetadataModifiedEvent(MetadataModifiedEvent event)
	{
		this.eventType = (byte) event.getEventType().getEventType();
		this.source = new EntityIDMsgPack(event.getSource());
		this.eventID = event.getEventID();
		this.timestamp = event.getTimestamp();
	}
	
	public CASCollectionEventMsgPack(TransactionCommittedEvent event)
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
	
	public CASCollectionEvent getEvent()
	{
		CASCollectionEventType ccEventType = CASCollectionEventType.eventTypeForChar((char)eventType);
		switch(ccEventType)
		{
		case kMetadataModified:
			return new MetadataModifiedEvent(source.getEntityID(), eventID, timestamp);
		case kSegmentCreated:
			return new SegmentCreatedEvent(segmentID.getObjectID(), source.getEntityID(), eventID, timestamp);
		case kSegmentReleased:
			return new SegmentReleasedEvent(segmentID.getObjectID(), source.getEntityID(), eventID, timestamp);
		case kTransactionCommited:
			return new TransactionCommittedEvent(transaction.getTransaction(), source.getEntityID(), eventID, timestamp);
		}
		throw new IllegalArgumentException("Unrecognized event type "+eventType);
	}
}
