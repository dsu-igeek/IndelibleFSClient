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
 */package com.igeekinc.indelible.indeliblefs.firehose.proxies;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleVersionIteratorHandle;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.NextVersionListItemsReply;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleVersionIteratorProxy implements IndelibleVersionIterator, AsyncCompletion<NextVersionListItemsReply, Void>
{
	private IndelibleFSFirehoseClient client;
	private IndelibleFSServerConnectionProxy connection;
	private IndelibleVersionIteratorHandle handle;
	private boolean hasNext, hasMore;
	private IndelibleVersion [] cached = new IndelibleVersion[0];
	private int nextCached = 0;
	private boolean fetching = false;
	
	public IndelibleVersionIteratorProxy(IndelibleFSFirehoseClient client, IndelibleFSServerConnectionProxy connection, IndelibleVersionIteratorHandle handle, IndelibleVersion [] initial, boolean hasMore)
	{
		this.client = client;
		this.connection = connection;
		this.handle = handle;
		cached = initial;
		this.hasMore = hasMore;
		this.hasNext = cached.length > 0;
	}

	@Override
	public synchronized boolean hasNext()
	{
		return (nextCached < cached.length)|| hasMore;
	}
	
	@Override
	public synchronized IndelibleVersion next()
	{
		checkFetching();
		if (!hasNext())
			throw new NoSuchElementException();
		IndelibleVersion returnVersion;

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
				client.nextVersionsListItem(connection, handle, this, null);
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
		connection.proxyFinalized(this);
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
	public synchronized void completed(NextVersionListItemsReply result, Void attachment)
	{
		cached = result.getVersions();
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
