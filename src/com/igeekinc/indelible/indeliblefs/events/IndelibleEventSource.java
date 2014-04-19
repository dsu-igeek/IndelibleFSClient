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

import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.EntityID;


public interface IndelibleEventSource
{
	/**
	 * Returns the most recent generated event ID
	 * @return
	 */
	public long getLastEventID();
	
	/**
	 * Returns the most recent replicated event from the specified source server
	 */
	public long getLastReplicatedEventID(EntityID sourceServerID, CASCollectionID collectionID);
	/**
	 * Returns events that we generated starting with (including) the specified event ID (enter 0 for all events)
	 * @param startingID - event to start with
	 * @return
	 */
	public IndelibleEventIterator eventsAfterID(long startingID);
	
	/**
	 * Returns events that we generated starting with (including) the specified timestamp (enter 0 for all events)
	 * @param timestamp
	 * @return
	 */
	public IndelibleEventIterator eventsAfterTime(long timestamp);
	
	/**
	 * Adds a listener that will receive new events
	 * @param listener
	 */
	public void addListener(IndelibleEventListener listener);
	
	/**
	 * Adds a listener that will receive all events starting with (including) the specified event ID (enter 0 for all events)
	 * The listener may be called on a different thread
	 * @param listener
	 * @param startingID
	 */
	public void addListenerAfterID(IndelibleEventListener listener, long startingID);
	
	/**
	 * Adds a listener that will receive all events starting with (including) the specified timestamp (enter 0 for all events)
	 * The listener may be called on a different thread
	 * @param listener
	 * @param timestamp
	 */
	public void addListenerAfterTime(IndelibleEventListener listener, long timestamp);
}
