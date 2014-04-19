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
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.igeekinc.indelible.indeliblefs.CreateDirectoryInfo;
import com.igeekinc.indelible.indeliblefs.CreateFileInfo;
import com.igeekinc.indelible.indeliblefs.CreateSymlinkInfo;
import com.igeekinc.indelible.indeliblefs.DeleteFileInfo;
import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleNodeInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.exceptions.CannotDeleteDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotFileException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.remote.CreateDirectoryInfoRemote;
import com.igeekinc.indelible.indeliblefs.remote.CreateFileInfoRemote;
import com.igeekinc.indelible.indeliblefs.remote.CreateSymlinkInfoRemote;
import com.igeekinc.indelible.indeliblefs.remote.DeleteFileInfoRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleDirectoryNodeRemote;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.util.async.AsyncCompletion;

public class IndelibleDirectoryNodeProxy extends IndelibleFileNodeProxy implements IndelibleDirectoryNodeIF
{
	public IndelibleDirectoryNodeProxy(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeRemote remote) throws RemoteException
	{
		super(connection, remote);
	}
	
	protected IndelibleDirectoryNodeRemote getRemote()
	{
		return (IndelibleDirectoryNodeRemote)super.getRemote();
	}
	
	@Override
	public boolean isFile()
	{
		return false;
	}

	@Override
	public IndelibleFSForkIF getFork(String name, boolean createIfNecessary)
			throws IOException, ForkNotFoundException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] listForkNames()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CreateFileInfo createChildFile(String name, boolean exclusive)
			throws IOException, PermissionDeniedException, FileExistsException
	{
		CreateFileInfoRemote remoteInfo = getRemote().createChildFile(name, exclusive);
		CreateFileInfo returnInfo = new CreateFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()), 
				new IndelibleFileNodeProxy(getConnection(), remoteInfo.getCreateNode()));
		return returnInfo;
	}

	@Override
	public CreateFileInfo createChildFile(String name,
			HashMap<String, CASIDDataDescriptor> initialForkDataLocal,
			boolean exclusive) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException
	{
		HashMap<String, CASIDDataDescriptor> initialForkDataRemote = getRemoteForkData(initialForkDataLocal);
		try
		{
			CreateFileInfoRemote remoteInfo = getRemote().createChildFile(name, initialForkDataRemote, exclusive);
			CreateFileInfo returnInfo = new CreateFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()), 
					new IndelibleFileNodeProxy(getConnection(), remoteInfo.getCreateNode()));
			return returnInfo;
		}
		finally
		{
			releaseRemoteForkData(initialForkDataRemote);
		}
	}

	@Override
	public CreateFileInfo createChildFile(String name,
			IndelibleFileNodeIF sourceFile, boolean exclusive)
			throws PermissionDeniedException, FileExistsException, IOException,
			NotFileException, ObjectNotFoundException
	{
		CreateFileInfoRemote remoteInfo = getRemote().createChildFile(name, sourceFile.getObjectID(), exclusive);
		CreateFileInfo returnInfo = new CreateFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()), 
				new IndelibleFileNodeProxy(getConnection(), remoteInfo.getCreateNode()));
		return returnInfo;
	}

	@Override
	public CreateSymlinkInfo createChildSymlink(String name,
			String targetPath, boolean exclusive)
			throws PermissionDeniedException, FileExistsException, IOException, ObjectNotFoundException
	{
		CreateSymlinkInfoRemote remoteInfo = getRemote().createChildSymlink(name, targetPath, exclusive);
		CreateSymlinkInfo returnInfo = new CreateSymlinkInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()),
				new IndelibleSymlinkNodeProxy(getConnection(), remoteInfo.getCreateNode()));
		return returnInfo;
	}

	@Override
	public CreateFileInfo createChildLink(String name,
			IndelibleFileNodeIF sourceFile)
			throws PermissionDeniedException, FileExistsException, IOException,
			NotFileException, ObjectNotFoundException
	{
		CreateFileInfoRemote remoteInfo = getRemote().createChildLink(name, sourceFile.getObjectID());
		CreateFileInfo returnInfo = new CreateFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()), 
				new IndelibleFileNodeProxy(getConnection(), remoteInfo.getCreateNode()));
		return returnInfo;
	}

	@Override
	public CreateDirectoryInfo createChildDirectory(String name)
			throws IOException, PermissionDeniedException, FileExistsException
	{
		CreateDirectoryInfoRemote remoteInfo = getRemote().createChildDirectory(name);
		CreateDirectoryInfo returnInfo = new CreateDirectoryInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()),
				new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getCreateNode()));
		return returnInfo;
	}

	@Override
	public DeleteFileInfo deleteChild(String name) throws IOException,
			PermissionDeniedException, CannotDeleteDirectoryException
	{
		DeleteFileInfoRemote remoteInfo = getRemote().deleteChild(name);
		DeleteFileInfo returnInfo = new DeleteFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()),
				remoteInfo.deleteSucceeded());
		return returnInfo;
	}

	@Override
	public DeleteFileInfo deleteChildDirectory(String name) throws IOException,
			PermissionDeniedException, NotDirectoryException
	{
		DeleteFileInfoRemote remoteInfo = getRemote().deleteChildDirectory(name);
		DeleteFileInfo returnInfo = new DeleteFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()),
				remoteInfo.deleteSucceeded());
		return returnInfo;
	}

	@Override
	public String[] list() throws IOException, PermissionDeniedException
	{
		return getRemote().list();
	}

	@Override
	public int getNumChildren() throws IOException
	{
		return getRemote().getNumChildren();
	}

	@Override
	public IndelibleFileNodeIF getChildNode(String name) throws IOException, PermissionDeniedException, ObjectNotFoundException
	{
		return (IndelibleFileNodeIF) getProxyForRemote(getRemote().getChildNode(name));
	}

	@Override
	public boolean isDirectory()
	{
		return true;
	}

	@Override
	public IndelibleNodeInfo[] getChildNodeInfo(String[] mdToRetrieve)
			throws IOException, PermissionDeniedException, RemoteException
	{
		return getRemote().getChildNodeInfo(mdToRetrieve);
	}

	protected HashMap<String, CASIDDataDescriptor>getRemoteForkData(HashMap<String, CASIDDataDescriptor>localForkData)
	{
		HashMap<String, CASIDDataDescriptor> remoteForkData = new HashMap<String, CASIDDataDescriptor>();
		for (Entry<String, CASIDDataDescriptor>curLocalEntry:localForkData.entrySet())
		{
			CASIDDataDescriptor localDescriptor = curLocalEntry.getValue();
			NetworkDataDescriptor remoteDescriptor = getConnection().registerDataDescriptor(localDescriptor);
			remoteForkData.put(curLocalEntry.getKey(), remoteDescriptor);
		}
		return remoteForkData;
	}
	
	protected void releaseRemoteForkData(HashMap<String, CASIDDataDescriptor>remoteForkData) throws IOException
	{
		for (Entry<String, CASIDDataDescriptor>curRemoteEntry:remoteForkData.entrySet())
		{
			CASIDDataDescriptor removeDescriptor = curRemoteEntry.getValue();
			getConnection().removeDataDescriptor(removeDescriptor);
			removeDescriptor.close();
		}
	}
	
	public IndelibleDirectoryNodeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		return (IndelibleDirectoryNodeIF)super.getVersion(version, flags);
	}

	@Override
	public Future<CreateFileInfo> createChildFileAsync(String name,
			HashMap<String, CASIDDataDescriptor> initialForkData,
			boolean exclusive) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A> void createChildFileAsync(String name,
			HashMap<String, CASIDDataDescriptor> initialForkData,
			boolean exclusive,
			AsyncCompletion<CreateFileInfo, ? super A> completionHandler,
			A attachment) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException
	{
		// TODO Auto-generated method stub
		
	}
}
