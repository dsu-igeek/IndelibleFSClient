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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Handles event support and dispatch for historical and non-historical event sources
 * @author David L. Smith-Uchida
 *
 */
public class IndelibleEventSupport
{
	private IndelibleEventSource source;
	private ArrayList<IndelibleEventQueue>queues = new ArrayList<IndelibleEventQueue>();
	private MultipleQueueDispatcher dispatcher;
    
    public IndelibleEventSupport(IndelibleEventSource source, MultipleQueueDispatcher dispatcher)
    {
    	this.source = source;
    	this.dispatcher = dispatcher;
    }
    
    public synchronized void addListener(IndelibleEventListener newListener)
    {
    	IndelibleEventQueue newQueue = dispatcher.createQueue(newListener, null);
    	queues.add(newQueue);
    }
    
	/**
	 * Adds a listener that will receive all events starting with (including) the specified event ID (enter 0 for all events)
	 * The listener may be called on a different thread
	 * @param listener
	 * @param startingID
	 */
	public synchronized void addListenerAfterID(IndelibleEventListener listener, long startingID)
	{
		IndelibleEventIterator oldEventIterator = source.eventsAfterID(startingID);
    	IndelibleEventQueue newQueue = dispatcher.createQueue(listener, oldEventIterator);
    	queues.add(newQueue);
	}
	
	/**
	 * Adds a listener that will receive all events starting with (including) the specified timestamp (enter 0 for all events)
	 * The listener may be called on a different thread
	 * @param listener
	 * @param timestamp
	 */
	public synchronized void addListenerAfterTime(IndelibleEventListener listener, long timestamp)
	{
		IndelibleEventIterator oldEventIterator = source.eventsAfterTime(timestamp);
    	IndelibleEventQueue newQueue = dispatcher.createQueue(listener, oldEventIterator);
    	queues.add(newQueue);
	}
	
    public synchronized void removeListener(IndelibleEventListener removeListener)
    {
    	Iterator<IndelibleEventQueue> queuesIterator = queues.iterator();
    	while(queuesIterator.hasNext())
    	{
    		IndelibleEventQueue checkQueue = queuesIterator.next();
			if (checkQueue.getListener() == removeListener)
    		{
    			queuesIterator.remove();
    			checkQueue.close();
    		}
    	}
    }
    
    public synchronized void fireIndelibleEvent(IndelibleEvent event)
    {
    	Iterator<IndelibleEventQueue> queuesIterator = queues.iterator();
    	while(queuesIterator.hasNext())
    	{
    		IndelibleEventQueue curQueue = queuesIterator.next();
    		curQueue.queueEvent(event);
    	}
    }
}
