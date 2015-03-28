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
package com.igeekinc.indelible.indeliblefs.events;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.igeekinc.util.logging.DebugLogMessage;

class QueueInfo
{
	private IndelibleEventQueue queue;
	private IndelibleEventListener listener;
	private ReentrantLock queueLock = new ReentrantLock();
	
	public QueueInfo(IndelibleEventListener listener)
	{
		this.listener = listener;
	}

	public IndelibleEventQueue getQueue()
	{
		return queue;
	}
	
	public void setQueue(IndelibleEventQueue queue)
	{
		this.queue = queue;
	}

	public IndelibleEventListener getListener()
	{
		return listener;
	}

	public boolean active()
	{
		return queueLock.isLocked();
	}
	
	public boolean tryLock()
	{
		return queueLock.tryLock();
	}
	
	public void unlock()
	{
		queueLock.unlock();
	}
}
public class MultipleQueueDispatcher implements IndelibleQueueListener
{
	private ArrayList<QueueInfo>queues = new ArrayList<QueueInfo>();
	private int lastServiced = 0;
	private int queueServiceMax;
	private ExecutorService threadPool;
	private Logger logger = Logger.getLogger(getClass());
	/**
	 * Creates a MultipleQueueDispatcher that will service events from a queue until it is empty and
	 * then moves on to the next queue.
	 * One thread will service all events
	 */
	public MultipleQueueDispatcher()
	{
		this(Integer.MAX_VALUE); // service all the events from a queue by default
	}
	
	/**
	 * Creates a MultipleQueueDispatcher that will service up to queueServiceMax events from a queue before
	 * then moves on to the next queue
	 * One thread will service all events
	 */
	public MultipleQueueDispatcher(int queueServiceMax)
	{
		this(queueServiceMax, 1);
	}
	/**
	 * Creates a MultipleQueueDispatcher that will service up to queueServiceMax events from a queue before
	 * then moves on to the next queue
	 * Up to maxThreads will be used to service events
	 */
	public MultipleQueueDispatcher(int queueServiceMax, int maxThreads)
	{
		if (queueServiceMax <= 0)
			throw new IllegalArgumentException("queueServiceMax must be >= 1");
		this.queueServiceMax = queueServiceMax;
		this.threadPool = Executors.newFixedThreadPool(maxThreads, new ThreadFactory()
		{
			
			@Override
			public Thread newThread(Runnable r)
			{
				return new Thread(r, "MultipleQueueDispatcher Thread");
			}
		});
		logger.debug("MultipleQueueDispatcher started with queueServiceMax = "+queueServiceMax+" maxThreads = "+maxThreads);
	}
	
	public IndelibleEventQueue createQueue(IndelibleEventListener listener, IndelibleEventIterator oldIterator)
	{
		synchronized(queues)
		{
			QueueInfo newQueueInfo = new QueueInfo(listener);
			// We don't pass the listener to the IndelibleEventQueue because that causes it to run its own
			// dispatch thread.
			// TODO - remove the listener from the IndelibleEventQueue
			IndelibleEventQueue addQueue = new IndelibleEventQueue(null, oldIterator, this, newQueueInfo);
			newQueueInfo.setQueue(addQueue);
			queues.add(newQueueInfo);
			logger.debug("MultipleQueueDispatch - queue created");
			return addQueue;
		}

	}
	
	public boolean removeQueue(IndelibleEventQueue removeQueue)
	{
		boolean removed = false;
		synchronized(queues)
		{
			// We look for all instances of the queue in the list
			for (int removeQueueNum = 0; removeQueueNum < queues.size(); removeQueueNum ++)
			{
				if (queues.get(removeQueueNum).getQueue() == removeQueue)
				{
					queues.remove(removeQueueNum);
					removed = true;
				}
			}
		}
		return removed;
	}
	
	public void dispatchEvents()
	{
		logger.debug("MultipleQueueDispatch - dispatchEvents running");
		boolean dispatched;
		do
		{
			dispatched = false;
			QueueInfo checkQueueInfo = null;
			synchronized(queues)
			{
				// Go once around the list of queues (max) looking for a queue to service
				for (int checkOffset = 0; checkOffset < queues.size(); checkOffset ++)
				{
					int checkQueueNum = (lastServiced + 1 + checkOffset) % queues.size();
					checkQueueInfo = queues.get(checkQueueNum);
					if (!checkQueueInfo.getQueue().isEmpty())
					{
						if (checkQueueInfo.tryLock())
						{
							lastServiced = checkQueueNum;	// We'll pick up here
							dispatched = true;	// We found something to do
							break;
						}
					}
				}
			}
			int eventsServicedFromQueue = 0;
			if (dispatched && checkQueueInfo != null)
			{
				IndelibleEventQueue serviceQueue = checkQueueInfo.getQueue();
				IndelibleEventListener listener = checkQueueInfo.getListener();
				try
				{
					IndelibleEvent serviceEvent;
					while ((serviceEvent = serviceQueue.poll()) != null && eventsServicedFromQueue < queueServiceMax)
					{
						eventsServicedFromQueue++;
						listener.indelibleEvent(serviceEvent);
					}
				}
				finally
				{
					checkQueueInfo.unlock();
				}
			}
		} while (dispatched);	// If we found something to do, check again to see if there's more work before exiting
	}

	@Override
	public void queueReady(IndelibleEventQueue queue, Object attachment)
	{
		logger.debug("queueReady for "+queue);
		if (!((QueueInfo)attachment).active())
		{
			logger.debug("queue is not being serviced, submitting to threadpool");
			try
			{
				threadPool.submit(new Runnable()
				{
					@Override
					public void run()
					{
						dispatchEvents();
					}
				});
			} catch (RejectedExecutionException e)
			{
				// It's OK if we get here.  It just means all of the threads are already busy
				Logger.getLogger(getClass()).debug(new DebugLogMessage("All threads busy for MultipleQueueDispatcher"), e);
			}
		}
		else
		{
			logger.debug("queue already being serviced");
		}
	}
}
