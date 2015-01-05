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
 
package com.igeekinc.indelible.indeliblefs.events;

import java.io.Serializable;
import java.util.Date;

import com.igeekinc.indelible.oid.EntityID;

/**
 * This is the base class for Indelible FS events.  All Indelible FS events are identified
 * by the EntityID (server) that generated the event, a 64 bit event number and a 64 bit timestamp
 *
 */
public abstract class IndelibleEvent implements Serializable
{
	private static final long	serialVersionUID	= 3214244008481514727L;
	private EntityID	source;
	private long		eventID;
	private long		timestamp;
	
	public IndelibleEvent(EntityID source, long eventID, long timestamp)
	{
		this.source = source;
		this.eventID = eventID;
		this.timestamp = timestamp;
	}

	protected IndelibleEvent(EntityID source)
	{
		this.source = source;
		this.eventID = -1L;
		this.timestamp = -1L;
	}
	
	public EntityID getSource()
	{
		return source;
	}

	public long getEventID()
	{
		return eventID;
	}

	public long getTimestamp()
	{
		return timestamp;
	}
	
	public void setTimestamp(long timestamp)
	{
		if (this.timestamp >= 0)
			throw new IllegalArgumentException("Cannot reset timestamp");
		this.timestamp = timestamp;
	}
	/**
	 * Convenience method to return the timestamp wrapped as a Date
	 * @return
	 */
	public Date getDate()
	{
		return new Date(timestamp);
	}
	
	public void setEventID(long eventID)
	{
		if (this.eventID >= 0)
			throw new IllegalArgumentException("Cannot reset event ID");
		this.eventID = eventID;
	}
}
