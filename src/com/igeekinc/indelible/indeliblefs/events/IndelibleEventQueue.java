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

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.pauseabort.AbortReason;
import com.igeekinc.util.pauseabort.AbortedException;
import com.igeekinc.util.pauseabort.PauseAbort;

public class IndelibleEventQueue implements Runnable
{
	Logger logger;
	LinkedList<IndelibleEvent> queue;
	IndelibleEventListener listener;
	IndelibleEventIterator oldEventIterator;
	Thread executionThread;
	PauseAbort pauser;
	
	public IndelibleEventQueue(IndelibleEventListener listener, IndelibleEventIterator oldEventIterator)
	{
		this.listener = listener;
		logger = Logger.getLogger(getClass());
		pauser = new PauseAbort(logger);
		queue = new LinkedList<IndelibleEvent>();
		this.oldEventIterator = oldEventIterator;
		executionThread = new Thread(this, "IndelibleEventQueue");
		executionThread.start();
	}
	
	public IndelibleEventListener getListener()
	{
		return listener;
	}
	
	public void run()
	{
		while(true)
		{
			try
			{
				long firstQueuedEvent = Long.MAX_VALUE;
				if (oldEventIterator != null)
				{
					try
					{
						while(oldEventIterator.hasNext())
						{
							pauser.checkAbort();
							IndelibleEvent oldEvent = oldEventIterator.next();

							if (firstQueuedEvent == Long.MAX_VALUE)
							{
								synchronized(queue)
								{
									if (queue.size() > 0)
									{
										IndelibleEvent firstEvent = queue.peekFirst();
										firstQueuedEvent = firstEvent.getEventID();
									}
								}
							}
							if (oldEvent.getEventID() >= firstQueuedEvent)
								break;	// We've gotten all the way to where events were queued to us, so bail and start processing the queue
							listener.indelibleEvent(oldEvent);
						}
					}
					finally
					{
						oldEventIterator.close();
					}
				}
			}
			catch(AbortedException e)
			{
				logger.debug("Event queue terminated, exiting");
				return;
			}
			catch (Throwable t)
			{
				logger.error(new ErrorLogMessage("Caught unexpected error in IndelibleEventQueue old event loop - continuing"), t);
			}
			try
			{
				while(true)
				{
					pauser.checkAbort();
					synchronized(queue)
					{
						while (queue.size() == 0)
						{
							try
							{
								queue.wait();
							} catch (InterruptedException e)
							{
								// The pauser should abort us out of here
							}
							pauser.checkAbort();
						}
						IndelibleEvent curEvent = queue.remove();
						listener.indelibleEvent(curEvent);
					}
				}
			}
			catch(AbortedException e)
			{
				logger.debug("Event queue terminated, exiting");
				return;
			}
			catch (Throwable t)
			{
				logger.error(new ErrorLogMessage("Caught unexpected error in IndelibleEventQueue run loop - continuing"), t);
			}
		}
	}
	
	public void queueEvent(IndelibleEvent eventToQueue)
	{
		synchronized(queue)
		{
			queue.add(eventToQueue);
			queue.notifyAll();
		}
	}
	
	public void close()
	{
		pauser.abort(AbortReason.kUserInitiatedAbort);
		executionThread.interrupt();
	}
}