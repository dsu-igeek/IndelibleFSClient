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
import java.rmi.RemoteException;
import java.util.HashMap;

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSForkRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public class IndelibleFileNodeProxy extends IndelibleFSObjectProxy implements IndelibleFileNodeIF
{
	public IndelibleFileNodeProxy(IndelibleFSServerConnectionProxy connection, IndelibleFileNodeRemote remote) throws RemoteException
	{
		super(connection, remote);
	}

	@Override
	protected IndelibleFileNodeRemote getRemote()
	{
		return (IndelibleFileNodeRemote)super.getRemote();
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
	public IndelibleFSForkIF getFork(String name, boolean createIfNecessary)
			throws IOException, ForkNotFoundException
	{
		IndelibleFileNodeRemote remote = getRemote();
		Object fork = remote.getFork(name, createIfNecessary);
		return new IndelibleFSForkProxy(getConnection(), (IndelibleFSForkRemote)fork);
	}

	@Override
	public void deleteFork(String forkName) throws IOException, ForkNotFoundException, PermissionDeniedException
	{
		IndelibleFileNodeRemote remote = getRemote();
		remote.deleteFork(forkName);
	}
	
	@Override
	public String [] listForkNames() throws IOException
	{
		return getRemote().listForkNames();
	}

	@Override
	public IndelibleFSVolumeIF getVolume() throws IOException
	{
		IndelibleFSObjectID volumeID = null;
		try
		{
			volumeID = getRemote().getVolumeID();
			return getConnection().retrieveVolume(volumeID);
		} catch (RemoteException e)
		{
			throw new IOException("Could not retrieve volume ID from remote");
		} catch (VolumeNotFoundException e)
		{
			throw new IOException("Could not find volume ID "+volumeID);
		}
	}

	@Override
	public IndelibleFileNodeIF setMetaDataResource(String mdResourceName,
			HashMap<String, Object> resources)
			throws PermissionDeniedException, IOException
	{
		return (IndelibleFileNodeIF)getProxyForRemote(getRemote().setMetaDataResource(mdResourceName, resources));
	}

	@Override
	public IndelibleVersionIterator listVersions() throws IOException
	{
		return getRemote().listVersions();
	}

	@Override
	public IndelibleFileNodeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		return (IndelibleFileNodeIF)getProxyForRemote(getRemote().getObjectForVersion(version, flags));
	}

	@Override
	public long lastModified() throws IOException
	{
		return getRemote().lastModified();
	}

	@Override
	public long length() throws IOException
	{
		return getRemote().totalLength();
	}

	@Override
	public long totalLength() throws IOException
	{
		return getRemote().totalLength();
	}

	@Override
	public long lengthWithChildren() throws IOException
	{
		return getRemote().lengthWithChildren();
	}
}
