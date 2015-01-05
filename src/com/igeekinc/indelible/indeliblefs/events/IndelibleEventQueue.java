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

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.pauseabort.AbortReason;
import com.igeekinc.util.pauseabort.AbortedException;
import com.igeekinc.util.pauseabort.PauseAbort;

/**
 * Takes new events (via IndelibleEventListener) and a historical event source (via IndelibleEventListener) and
 * presents a unified queue of events.  Events are taken from the historical source until they catch up with the new events.
 * @author David L. Smith-Uchida
 *
 */
public class IndelibleEventQueue implements Runnable, BlockingQueue<IndelibleEvent>
{
	protected Logger logger;
	protected LinkedBlockingQueue<IndelibleEvent> historicalQueue;
	protected boolean historicalQueueFinished = false;
	protected LinkedBlockingQueue<IndelibleEvent> currentQueue;
	protected IndelibleEventListener listener;
	protected IndelibleEventIterator oldEventIterator;
	protected Thread oldEventThread, dispatchThread;
	protected PauseAbort pauser;
	protected IndelibleQueueListener queueListener;
	protected Object queueListenerAttachment;
	
	public IndelibleEventQueue(IndelibleEventIterator oldEventIterator)
	{
		this(null, oldEventIterator, null, null);
	}
	
	public IndelibleEventQueue(IndelibleEventListener listener, IndelibleEventIterator oldEventIterator, IndelibleQueueListener queueListener, Object queueListenerAttachment)
	{
		this.listener = listener;
		logger = Logger.getLogger(getClass());
		pauser = new PauseAbort(logger);
		currentQueue = new LinkedBlockingQueue<IndelibleEvent>();
		historicalQueue = new LinkedBlockingQueue<IndelibleEvent>(1024);
		this.oldEventIterator = oldEventIterator;
		this.queueListener = queueListener;
		this.queueListenerAttachment = queueListenerAttachment;
		if (listener != null)
		{
			dispatchThread = new Thread(this, "IndelibleEventQueue");
			dispatchThread.start();
		}
		oldEventThread = new Thread(new Runnable()
		{
			
			@Override
			public void run()
			{
				try
				{
					queueOldEvents(IndelibleEventQueue.this.oldEventIterator);
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
			}
		}, "Old Event Thread");
		oldEventThread.start();
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
				processQueuedEvents();
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

	private void processQueuedEvents() throws AbortedException
	{
		while(true)
		{
			pauser.checkAbort();
			try
			{
				IndelibleEvent curEvent = take(); // Wait until we have something
				dispatchEvent(curEvent);
			} catch (InterruptedException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}	
		}
	}
	/*
	{
		while(true)
		{
			pauser.checkAbort();
			while (!historicalQueueFinished)
			{
				try
				{
					IndelibleEvent curEvent = historicalQueue.poll(1, TimeUnit.SECONDS);
					if (curEvent != null)
						dispatchEvent(curEvent);
				} catch (InterruptedException e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				}	

			}
			synchronized(currentQueue)
			{
				while (currentQueue.size() == 0)
				{
					try
					{
						currentQueue.wait();
					} catch (InterruptedException e)
					{
						// The pauser should abort us out of here
					}
					pauser.checkAbort();
				}
				IndelibleEvent curEvent = currentQueue.remove();
				dispatchEvent(curEvent);
			}

		}
	}*/

	/*
	 * Takes old events from the oldEventIterator and puts them on to the historical queue.
	 */
	private void queueOldEvents(IndelibleEventIterator oldEventIterator) throws AbortedException, InterruptedException
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
						if (currentQueue.size() > 0)
						{
							IndelibleEvent firstEvent = currentQueue.peek();
							firstQueuedEvent = firstEvent.getEventID();
						}
					}
					if (oldEvent.getEventID() >= firstQueuedEvent)
						break;	// We've gotten all the way to where events were queued to us, so bail and start processing the queue
					historicalQueue.put(oldEvent);
					if (queueListener != null)
						queueListener.queueReady(this, queueListenerAttachment);
				}
			}
			finally
			{
				oldEventIterator.close();
				historicalQueueFinished = true;
			}
		}
	}

	private void dispatchEvent(IndelibleEvent oldEvent)
	{
		if (listener != null)
			listener.indelibleEvent(oldEvent);
	}
	
	public void queueEvent(IndelibleEvent eventToQueue)
	{
		synchronized(currentQueue)
		{
			currentQueue.add(eventToQueue);
			currentQueue.notifyAll();
		}
		if (queueListener != null)
			queueListener.queueReady(this, queueListenerAttachment);
	}
	
	public void close()
	{
		pauser.abort(AbortReason.kUserInitiatedAbort);
		dispatchThread.interrupt();
	}

	@Override
	public IndelibleEvent remove()
	{
		IndelibleEvent returnEvent = poll();
		if (returnEvent == null)
			throw new NoSuchElementException();
		return returnEvent;
	}

	@Override
	public IndelibleEvent poll()
	{
		if (useHistoricalQueue())
			return historicalQueue.poll();
		return currentQueue.poll();
	}

	@Override
	public IndelibleEvent element()
	{
		IndelibleEvent returnEvent = peek();
		if (returnEvent == null)
			throw new NoSuchElementException();
		return returnEvent;
	}

	@Override
	/**
	 * Kind of an idiosyncratic peek - if the historical queue is active will return
	 * the first element of historical queue or NULL if the historical queue is empty even if
	 * there is something on the current queue.
	 */
	public IndelibleEvent peek()
	{
		if (useHistoricalQueue())
			return historicalQueue.peek();
		return currentQueue.peek();
	}

	private boolean useHistoricalQueue()
	{
		return !historicalQueueFinished || !historicalQueue.isEmpty();
	}

	@Override
	public int size()
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean isEmpty()
	{
		return peek() == null;
	}

	@Override
	public Iterator<IndelibleEvent> iterator()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] a)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends IndelibleEvent> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(IndelibleEvent e)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean offer(IndelibleEvent e)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void put(IndelibleEvent e) throws InterruptedException
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean offer(IndelibleEvent e, long timeout, TimeUnit unit) throws InterruptedException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleEvent take() throws InterruptedException
	{
		while (useHistoricalQueue())
		{
			// We poll here so that if the historical queue finishes while we're in here we'll
			// wake up and figure it out
			IndelibleEvent historicalEvent = historicalQueue.poll(1, TimeUnit.SECONDS);
			if (historicalEvent != null)
				return historicalEvent;
		}
		return currentQueue.take();
	}

	@Override
	public IndelibleEvent poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		IndelibleEvent returnEvent = null;
		if (useHistoricalQueue())
		{
			returnEvent = historicalQueue.poll(timeout, unit);
			timeout = 0;	// Set timeout to zero so if we need to hit the currentQueue (historicalQueueFinished
							// changed while we were in poll) it will just check the
							// head of queue and not wait
		}
		// If the historical queue state is now finished (it was either that way all along or it changed during the historicalQueue poll and we didn't get anything from the historical
		// queue we want to go to the current queue
		if (historicalQueueFinished && returnEvent == null)
			returnEvent = currentQueue.poll(timeout, unit);
		return returnEvent;
	}

	@Override
	public int remainingCapacity()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int drainTo(Collection<? super IndelibleEvent> c)
	{
		return drainTo(c, Integer.MAX_VALUE);
	}

	@Override
	public int drainTo(Collection<? super IndelibleEvent> c, int maxElements)
	{
		if (c == this)
			throw new IllegalArgumentException("Can't drain to the same queue");
		IndelibleEvent drainEvent;
		int eventsDrained = 0;
		while((drainEvent = poll()) != null && eventsDrained < maxElements)
		{
			c.add(drainEvent);
			eventsDrained++;
		}
		return eventsDrained;
	}
}