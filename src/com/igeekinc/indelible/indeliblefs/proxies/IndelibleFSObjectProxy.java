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
 
package com.igeekinc.indelible.indeliblefs.proxies;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSObjectIF;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleDirectoryNodeRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSVolumeRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleSymlinkNodeRemote;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSObjectProxy implements IndelibleFSObjectIF
{
	private IndelibleFSServerConnectionProxy connection;
	private final IndelibleFSObjectRemote remote;
	private final IndelibleFSObjectID objectID;

	
	protected IndelibleFSObjectProxy(IndelibleFSServerConnectionProxy connection, IndelibleFSObjectRemote remote) throws RemoteException
	{
		this.connection = connection;
		if (connection == null)
			throw new IllegalArgumentException("connection cannot be null");
		this.remote = remote;
		if (remote == null)
			throw new NoSuchObjectException("Object not available from server");	// If we got here with a null it's probably because we failed to recover from a GC'd object on the server side
		this.objectID  = remote.getObjectID();
	}
	
	protected IndelibleFSObjectRemote getRemote()
	{
		return remote;
	}
	
	@Override
	public IndelibleFSObjectID getObjectID()
	{
		return objectID;
	}

	public IndelibleFSServerConnectionProxy getConnection()
	{
		return connection;
	}

	@Override
	public String[] listMetaDataResources() throws PermissionDeniedException, IOException
	{
		return remote.listMetaDataResources();
	}

	@Override
	public HashMap<String, Object> getMetaDataResource(String mdResourceName)
			throws PermissionDeniedException, IOException
	{
		return remote.getMetaDataResource(mdResourceName);
	}

	@Override
	public IndelibleFSObjectIF setMetaDataResource(String mdResourceName,
			HashMap<String, Object> resources)
			throws PermissionDeniedException, IOException
	{
		return getProxyForRemote(remote.setMetaDataResource(mdResourceName, resources));
	}


	@Override
	public IndelibleVersionIterator listVersions() throws IOException
	{
		return remote.listVersions();
	}

	@Override
	public IndelibleVersion getVersion()
	{
		// TODO - this should be immutable - make sure it is and then have IndelibleFSVolumeProxy cache the version ID
		try
		{
			return remote.getVersion();
		} catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Could not get version");
		}
	}
	

	@Override
	public IndelibleFSObjectIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		IndelibleFSVolumeRemote oldVersionRemote = (IndelibleFSVolumeRemote)remote.getObjectForVersion(version, flags);
		return new IndelibleFSVolumeProxy(connection, oldVersionRemote);
	}

	protected IndelibleFSObjectIF getProxyForRemote(IndelibleFSObjectRemote remoteObject) throws RemoteException,
			InternalError
	{
		if (remoteObject == null)
			return null;
		if (remoteObject instanceof IndelibleFSVolumeRemote)
			return new IndelibleFSVolumeProxy(connection, (IndelibleFSVolumeRemote)remoteObject);
		if (remoteObject instanceof IndelibleSymlinkNodeRemote)
			return new IndelibleSymlinkNodeProxy(connection, (IndelibleSymlinkNodeRemote)remoteObject);
		if (remoteObject instanceof IndelibleDirectoryNodeRemote)
			return new IndelibleDirectoryNodeProxy(connection, (IndelibleDirectoryNodeRemote)remoteObject);
		if (remoteObject instanceof IndelibleFileNodeRemote)
			return new IndelibleFileNodeProxy(connection, (IndelibleFileNodeRemote)remoteObject);
		throw new InternalError("Unrecognized remote type "+remoteObject.getClass());
	}

	@Override
	protected void finalize() throws Throwable
	{
		if (connection != null)
			connection.proxyFinalized(this);
		super.finalize();
	}
	
	
}
