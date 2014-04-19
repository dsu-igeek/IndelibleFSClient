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
import com.igeekinc.indelible.indeliblefs.IndelibleSymlinkNodeIF;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleSymlinkNodeRemote;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public class IndelibleSymlinkNodeProxy extends IndelibleFileNodeProxy implements IndelibleSymlinkNodeIF
{
	public IndelibleSymlinkNodeProxy(IndelibleFSServerConnectionProxy connection, IndelibleSymlinkNodeRemote remote) throws RemoteException
	{
		super(connection, remote);
	}

	
	@Override
	protected IndelibleSymlinkNodeRemote getRemote()
	{
		return (IndelibleSymlinkNodeRemote)super.getRemote();
	}


	@Override
	public IndelibleFSForkIF getFork(String name, boolean createIfNecessary)
			throws IOException, ForkNotFoundException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] listForkNames() throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTargetPath() throws IOException
	{
		return getRemote().getTargetPath();
	}
}
