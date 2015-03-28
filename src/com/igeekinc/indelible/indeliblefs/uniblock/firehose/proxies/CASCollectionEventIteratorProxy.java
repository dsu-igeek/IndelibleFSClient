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
package com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.IndelibleEventIteratorHandle;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.NextCollectionEventListItemsReply;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;

public class CASCollectionEventIteratorProxy implements IndelibleEventIterator, AsyncCompletion<NextCollectionEventListItemsReply, Void>
{
	private CASServerFirehoseClient client;
	private CASCollectionConnectionProxy collectionConnection;
	private IndelibleEventIteratorHandle handle;
	private boolean hasMore;
	private IndelibleEvent [] cached = new IndelibleEvent[0];
	private int nextCached = 0;
	private boolean fetching = false;
	
	public CASCollectionEventIteratorProxy(CASServerFirehoseClient client, CASCollectionConnectionProxy collectionConnection, IndelibleEventIteratorHandle handle, IndelibleEvent [] initial, boolean hasMore)
	{
		this.client = client;
		this.collectionConnection = collectionConnection;
		this.handle = handle;
		cached = initial;
		this.hasMore = hasMore;
	}

	@Override
	public synchronized boolean hasNext()
	{
		return (nextCached < cached.length)|| hasMore;
	}
	
	@Override
	public synchronized IndelibleEvent next()
	{
		checkFetching();
		if (!hasNext())
			throw new NoSuchElementException();
		IndelibleEvent returnVersion;

		returnVersion = cached[nextCached];
		nextCached++;
		if (nextCached >= cached.length)
		{
			synchronized(this)
			{
				fetching = true;
			}
			try
			{
				client.nextCASCollectionEventListItemAsync(collectionConnection, handle, this, null);
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				fetching = false;
			}
		}
		return returnVersion;
	}
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void close()
	{
		collectionConnection.proxyFinalized(this);
	}
	
	public void finalize()
	{
		close();
	}
	public void checkFetching()
	{
		while(fetching)
		{
			try
			{
				this.wait(1000);
			} catch (InterruptedException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
	}

	@Override
	public synchronized void completed(NextCollectionEventListItemsReply result, Void attachment)
	{
		cached = result.getEvents();
		nextCached = 0;
		hasMore = result.hasMore();
		fetching = false;
		this.notifyAll();
	}

	@Override
	public void failed(Throwable exc, Void attachment)
	{
		// TODO Auto-generated method stub
		
	}
	
}
