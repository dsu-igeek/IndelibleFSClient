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
package com.igeekinc.indelible.indeliblefs.firehose.proxies;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSObjectIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFileHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFileNodeProxy extends IndelibleFSObjectProxy implements
		IndelibleFileNodeIF
{
	private IndelibleFSVolumeProxy volume;
	
	public IndelibleFileNodeProxy(IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeProxy volume,
			IndelibleFSObjectHandle remote)
	{
		super(client, connection, remote);
		this.volume = volume;
	}

	@Override
	public IndelibleFSFileHandle getHandle()
	{
		return (IndelibleFSFileHandle)super.getHandle();
	}

	@Override
	public long lastModified() throws IOException
	{
		ComboFutureBase<Long>lastModifiedFuture = new ComboFutureBase<Long>();
		getClient().lastModifiedAsync(getConnection(), getHandle(), lastModifiedFuture, null);

		try
		{
			return lastModifiedFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public boolean isDirectory()
	{
		return false;
	}

	@Override
	public boolean isFile()
	{
		return true;
	}

	@Override
	public long length() throws IOException
	{
		return totalLength();	
	}

	@Override
	public long totalLength() throws IOException
	{
		ComboFutureBase<Long>totalLengthFuture = new ComboFutureBase<Long>();
		getClient().totalLengthAsync(getConnection(), getHandle(), totalLengthFuture, null);

		try
		{
			return totalLengthFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public long lengthWithChildren() throws IOException
	{
		return length();
	}

	@Override
	public IndelibleFSForkIF getFork(String name, boolean createIfNecessary)
			throws IOException, ForkNotFoundException,
			PermissionDeniedException
	{
		ComboFutureBase<IndelibleFSForkIF>getForkFuture = new ComboFutureBase<IndelibleFSForkIF>();
		getClient().getForkAsync(getConnection(), getHandle(), name, createIfNecessary, getForkFuture, null);
		try
		{
			return getForkFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof ForkNotFoundException)
				throw (ForkNotFoundException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public void deleteFork(String forkName) throws IOException,
			ForkNotFoundException, PermissionDeniedException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String[] listForkNames() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndelibleFSVolumeIF getVolume()
	{
		return volume;
	}

	@Override
	public IndelibleFileNodeIF setMetaDataResource(String mdResourceName,
			Map<String, Object> resources)
			throws PermissionDeniedException, IOException
	{
		return (IndelibleFileNodeIF)super.setMetaDataResource(mdResourceName, resources);
	}

	@Override
	public IndelibleFileNodeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		return (IndelibleFileNodeIF)super.getVersion(version, flags);
	}

	@Override
	protected IndelibleFSObjectIF getProxyForRemote(
			IndelibleFSObjectHandle handle)
	{
		return super.getProxyForRemote((IndelibleFSVolumeProxy) getVolume(), handle);
	}
}
