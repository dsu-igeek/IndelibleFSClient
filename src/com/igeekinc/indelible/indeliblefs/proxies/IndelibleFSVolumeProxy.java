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
import java.util.Properties;
import java.util.concurrent.Future;

import com.igeekinc.indelible.indeliblefs.CreateFileInfo;
import com.igeekinc.indelible.indeliblefs.DeleteFileInfo;
import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleSymlinkNodeIF;
import com.igeekinc.indelible.indeliblefs.MoveObjectInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotIterator;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.remote.CreateFileInfoRemote;
import com.igeekinc.indelible.indeliblefs.remote.DeleteFileInfoRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSVolumeRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote;
import com.igeekinc.indelible.indeliblefs.remote.MoveObjectInfoRemote;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.async.AsyncCompletion;

public class IndelibleFSVolumeProxy extends IndelibleFSObjectProxy implements IndelibleFSVolumeIF
{
	public IndelibleFSVolumeProxy(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeRemote remoteVolume)
	throws RemoteException
	{
		super(connection, remoteVolume);
	}
	
	protected IndelibleFSVolumeRemote getRemote()
	{
		return (IndelibleFSVolumeRemote)super.getRemote();
	}

	@Override
	protected IndelibleFileNodeIF getProxyForRemote(
			IndelibleFSObjectRemote remoteObject) throws RemoteException,
			InternalError
	{
		return (IndelibleFileNodeIF)super.getProxyForRemote(remoteObject);
	}

	@Override
	public IndelibleDirectoryNodeIF getRoot() throws PermissionDeniedException, IOException
	{
		return new IndelibleDirectoryNodeProxy(getConnection(), getRemote().getRoot());
	}

	@Override
	public IndelibleFileNodeIF getObjectByPath(FilePath path)
			throws ObjectNotFoundException, PermissionDeniedException,
			IOException
	{
		IndelibleFileNodeRemote remoteObject = getRemote().getObjectByPath(path);
		return getProxyForRemote(remoteObject);
	}

	@Override
	public IndelibleFileNodeIF createNewFile() throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleFileNodeIF createNewFile(IndelibleFileNodeIF sourceFile)
			throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleDirectoryNodeIF createNewDirectory() throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleSymlinkNodeIF createNewSymlink(String targetPath)
			throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleFileNodeIF getObjectByID(IndelibleFSObjectID id)
			throws IOException, ObjectNotFoundException
	{
		IndelibleFSObjectRemote remoteObject = getRemote().getObjectByID(id);
		if (!(remoteObject instanceof IndelibleFileNodeRemote))
			throw new ObjectNotFoundException();
		return getProxyForRemote(remoteObject);
	}

	@Override
	public IndelibleFileNodeIF getObjectByID(IndelibleFSObjectID id,
			IndelibleVersion version, RetrieveVersionFlags flags)
			throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteObjectByID(IndelibleFSObjectID id) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleFSObjectID getVolumeID()
	{
		return getObjectID();
	}

	@Override
	public void setVolumeName(String volumeName)
			throws PermissionDeniedException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getVolumeName() throws PermissionDeniedException, IOException
	{
		return getRemote().getVolumeName();
	}

	@Override
	public void setUserProperties(Properties propertiesToSet)
			throws PermissionDeniedException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleFSVolumeIF setMetaDataResource(String mdResourceName,
			HashMap<String, Object> resources)
			throws PermissionDeniedException, IOException
	{
		return new IndelibleFSVolumeProxy(getConnection(), (IndelibleFSVolumeRemote) getRemote().setMetaDataResource(mdResourceName, resources));
	}

	@Override
	public IndelibleVersion getVersion()
	{
		return super.getVersion();
	}

	@Override
	public IndelibleFSVolumeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		return (IndelibleFSVolumeIF)super.getVersion(version, flags);
	}


	
	@Override
	public IndelibleVersionIterator listVersionsForObject(IndelibleFSObjectID id)
			throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void addSnapshot(IndelibleSnapshotInfo snapshotInfo)
			throws PermissionDeniedException, IOException
	{
		getRemote().addSnapshot(snapshotInfo);
	}

	@Override
	public boolean releaseSnapshot(IndelibleVersion removeSnapshotVersion)
			throws PermissionDeniedException, IOException
	{
		return getRemote().releaseSnapshot(removeSnapshotVersion);
	}

	@Override
	public IndelibleSnapshotInfo getInfoForSnapshot(
			IndelibleVersion retrieveSnapshotVersion)
			throws PermissionDeniedException, IOException
	{
		return getRemote().getInfoForSnapshot(retrieveSnapshotVersion);
	}

	@Override
	public IndelibleSnapshotIterator listSnapshots()
			throws PermissionDeniedException, IOException
	{
		return new IndelibleSnapshotIteratorProxy(getRemote().listSnapshots());
	}

	@Override
	public <A> void getObjectByIDAsync(IndelibleFSObjectID id,
			IndelibleVersion version, RetrieveVersionFlags flags,
			AsyncCompletion<IndelibleFileNodeIF, A> completionHandler,
			A attachment) throws IOException, ObjectNotFoundException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Future<IndelibleFileNodeIF> getObjectByIDAsync(
			IndelibleFSObjectID id, IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException,
			ObjectNotFoundException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteFileInfo deleteObjectByPath(FilePath deletePath)
			throws IOException, ObjectNotFoundException,
			PermissionDeniedException, NotDirectoryException
	{
		DeleteFileInfoRemote remoteInfo = getRemote().deleteObjectByPath(deletePath);
		DeleteFileInfo returnInfo = new DeleteFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteInfo.getDirectoryNode()), 
				remoteInfo.deleteSucceeded());
		return returnInfo;
	}

	@Override
	public MoveObjectInfo moveObject(FilePath sourcePath,
			FilePath destinationPath) throws IOException,
			ObjectNotFoundException, PermissionDeniedException,
			FileExistsException, NotDirectoryException
	{
		MoveObjectInfoRemote remoteInfo = getRemote().moveObject(sourcePath, destinationPath);
		DeleteFileInfoRemote remoteSourceInfo = remoteInfo.getSourceInfo();
		DeleteFileInfo sourceInfo = new DeleteFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteSourceInfo.getDirectoryNode()), 
				remoteSourceInfo.deleteSucceeded());
		CreateFileInfoRemote remoteDestInfo = remoteInfo.getDestInfo();
		CreateFileInfo destInfo = new CreateFileInfo(new IndelibleDirectoryNodeProxy(getConnection(), remoteDestInfo.getDirectoryNode()), 
				new IndelibleFileNodeProxy(getConnection(), remoteDestInfo.getCreateNode()));
		
		MoveObjectInfo returnInfo = new MoveObjectInfo(sourceInfo, destInfo);
		return returnInfo;
	}
}
