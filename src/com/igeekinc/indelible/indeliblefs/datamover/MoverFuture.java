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
 
package com.igeekinc.indelible.indeliblefs.datamover;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;

public class MoverFuture implements Future<Void>, AsyncCompletion<Void, MoverFuture>
{
	private AsyncCompletion<Void, Object>completionHandler;
	private Object attachment;
	private boolean done = false;
	Throwable error;
	
	public MoverFuture()
	{
	}
	
	@SuppressWarnings("unchecked")
	public <A> MoverFuture(AsyncCompletion<Void, ? super A>completionHandler, A attachment)
	{
		this.completionHandler = (AsyncCompletion<Void, Object>) completionHandler;
		this.attachment = attachment;
	}
	@Override
	public boolean cancel(boolean paramBoolean)
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isCancelled()
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isDone()
	{
		return done;
	}
	
	private synchronized void setDone()
	{
		done = true;
		notifyAll();
		if (completionHandler != null)
		{
			try
			{
				completionHandler.completed(null, attachment);
			}
			catch (Throwable t)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), t);
			}
		}
	}
	
	@Override
	public Void get() throws InterruptedException, ExecutionException
	{
		try
		{
			get(0, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		return null;
	}
	
	@Override
	public synchronized Void get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException
	{
		long timeoutMS = TimeUnit.MILLISECONDS.convert(timeout, unit);
    	long timeStartedMS = System.currentTimeMillis();
    	while (!done && (timeoutMS == 0 || timeoutMS > (System.currentTimeMillis() - timeStartedMS)))
    	{
    		this.wait(timeoutMS);
    	}
    	if (error != null)
    		throw new ExecutionException(error);
    	if (!done)
    		throw new TimeoutException();
    	return null;
	}
	@Override
	public void completed(Void result, MoverFuture attachment)
	{
		setDone();
	}
	
	@Override
	public void failed(Throwable exc, MoverFuture attachment)
	{
		error = exc;
		setDone();
	}
}
