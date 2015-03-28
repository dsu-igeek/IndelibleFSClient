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
 
package com.igeekinc.indelible.indeliblefs.firehose.proxies;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSObjectIF;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSDirectoryHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle.ObjectHandleType;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerConnectionHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSObjectProxy implements IndelibleFSObjectIF
{
	private final IndelibleFSFirehoseClient client;
	private final IndelibleFSServerConnectionProxy connection;
	private final IndelibleFSObjectHandle handle;
	private final IndelibleFSObjectID objectID;

	
	protected IndelibleFSObjectProxy(IndelibleFSFirehoseClient client, IndelibleFSServerConnectionProxy connection, IndelibleFSObjectHandle remote)
	{
		this.client = client;
		this.connection = connection;
		if (connection == null)
			throw new IllegalArgumentException("connection cannot be null");
		this.handle = remote;
		if (remote == null)
			throw new IllegalArgumentException("remote cannot be null");	// If we got here with a null it's probably because we failed to recover from a GC'd object on the server side
		this.objectID  = remote.getObjectID();
	}
	
	protected IndelibleFSObjectHandle getHandle()
	{
		return handle;
	}
	
	@Override
	public IndelibleFSObjectID getObjectID()
	{
		return objectID;
	}

	protected IndelibleFSFirehoseClient getClient()
	{
		return client;
	}
	
	protected IndelibleFSServerConnectionHandle getConnectionHandle()
	{
		return connection.getHandle();
	}

	protected IndelibleFSServerConnectionProxy getConnection()
	{
		return connection;
	}
	
	@Override
	public String[] listMetaDataResources() throws PermissionDeniedException, IOException
	{
		ComboFutureBase<String []>listMetaDataResourcesFuture = new ComboFutureBase<String[]>();
		client.listMetaDataResourcesAsync(getConnection(), handle, listMetaDataResourcesFuture, null);
		try
		{
			return listMetaDataResourcesFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public Map<String, Object> getMetaDataResource(String mdResourceName)
			throws PermissionDeniedException, IOException
	{
		ComboFutureBase<Map<String, Object>>getMetaDataFuture = new ComboFutureBase<Map<String, Object>>();
		client.getMetaDataResourceAsync(getConnection(), handle, mdResourceName, getMetaDataFuture, null);
		try
		{
			return getMetaDataFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public IndelibleFSObjectIF setMetaDataResource(String mdResourceName,
			Map<String, Object> resources)
			throws PermissionDeniedException, IOException
	{
		ComboFutureBase<IndelibleFSObjectHandle>setMetaDataFuture = new ComboFutureBase<IndelibleFSObjectHandle>();
		client.setMetaDataResourceAsync(getConnection(), handle, mdResourceName, resources, setMetaDataFuture, null);
		try
		{
			return getProxyForRemote(setMetaDataFuture.get());
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}


	@Override
	public IndelibleVersionIterator listVersions() throws IOException
	{
		ComboFutureBase<IndelibleVersionIterator>setMetaDataFuture = new ComboFutureBase<IndelibleVersionIterator>();
		client.listVersionsAsync(getConnection(), handle, setMetaDataFuture, null);
		try
		{
			return setMetaDataFuture.get();
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
	public IndelibleVersion getCurrentVersion()
	{
		// This should probably be set when we create the object...though, there are handles that are
		// supposed to just track the latest object.  More thinking needed.
		try
		{
			ComboFutureBase<IndelibleVersion>getCurrentVersionFuture = new ComboFutureBase<IndelibleVersion>();
			client.getCurrentVersionAsync(getConnection(), handle, getCurrentVersionFuture, null);

			return getCurrentVersionFuture.get();
		} catch (Throwable e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}

	}
	

	@Override
	public IndelibleFSObjectIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		ComboFutureBase<IndelibleFSObjectHandle>getVersionFuture = new ComboFutureBase<IndelibleFSObjectHandle>();
		client.getVersionAsync(getConnection(), handle, version, flags, getVersionFuture, null);
		try
		{
			return getProxyForRemote(getVersionFuture.get());
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
	
	protected IndelibleFSObjectIF getProxyForRemote(IndelibleFSObjectHandle handle)
	{
		return getProxyForRemote(null, handle);		// Overriden in IndelibleFSVolumeProxy and IndelibleFileNodeProxy to use the correct volume
	}

	protected IndelibleFSObjectIF getProxyForRemote(IndelibleFSVolumeProxy volume, IndelibleFSObjectHandle handle)
	{
		if (handle == null)
			return null;
		if (handle.getHandleType() == ObjectHandleType.kVolume)
			return new IndelibleFSVolumeProxy(client, connection, (IndelibleFSVolumeHandle)handle);
		if (volume == null)
			throw new IllegalArgumentException("Volume must not be null for file/directory objects");
		if (handle.getHandleType() == ObjectHandleType.kSymbolicLink)
			return new IndelibleSymlinkNodeProxy(client, connection, volume, handle);
		if (handle.getHandleType() == ObjectHandleType.kDirectory)
			return new IndelibleDirectoryNodeProxy(client, connection, volume, (IndelibleFSDirectoryHandle)handle);
		if (handle.getHandleType() == ObjectHandleType.kFile)
			return new IndelibleFileNodeProxy(client, connection, volume, handle);
		throw new InternalError("Unrecognized remote type "+handle.getClass());
	}

	@Override
	protected void finalize() throws Throwable
	{
		getConnection().proxyFinalized(this);
		super.finalize();
	}
	
	
}
