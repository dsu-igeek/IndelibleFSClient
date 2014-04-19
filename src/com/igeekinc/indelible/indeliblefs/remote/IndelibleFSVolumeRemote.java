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
 
package com.igeekinc.indelible.indeliblefs.remote;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.igeekinc.indelible.indeliblefs.DeleteFileInfo;
import com.igeekinc.indelible.indeliblefs.MoveObjectInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.FilePath;

public interface IndelibleFSVolumeRemote extends IndelibleFSObjectRemote, Remote
{
    public abstract IndelibleDirectoryNodeRemote getRoot()
            throws PermissionDeniedException, RemoteException, IOException;

    public abstract IndelibleFileNodeRemote getObjectByPath(FilePath path)
            throws ObjectNotFoundException, PermissionDeniedException, RemoteException, IOException;
    public abstract IndelibleFSObjectRemote getObjectByID(IndelibleFSObjectID id)
    	throws RemoteException, IOException, ObjectNotFoundException;
    public abstract IndelibleFSObjectID getVolumeID() throws RemoteException;
    
    public void setVolumeName(String volumeName) throws RemoteException, PermissionDeniedException, IOException;
    public String getVolumeName() throws RemoteException, PermissionDeniedException, IOException;
    
	/**
	 * Adds a snapshot for a particular version(time).  Snapshots can be added for any time and the
	 * system will retain the nearest version (before) the snapshot.  
	 * @param snapshotVersion
	 * @param snapshotMetadata
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
    public void addSnapshot(IndelibleSnapshotInfo snapshotInfo) throws PermissionDeniedException, IOException, RemoteException;

    public boolean releaseSnapshot(IndelibleVersion removeSnapshotVersion) throws PermissionDeniedException, IOException, RemoteException;
    public IndelibleSnapshotIteratorRemote listSnapshots() throws PermissionDeniedException, IOException, RemoteException;
    public IndelibleSnapshotInfo getInfoForSnapshot(IndelibleVersion retrieveSnapshotVersion) throws PermissionDeniedException, IOException, RemoteException;
	public MoveObjectInfoRemote moveObject(FilePath sourcePath, FilePath destinationPath) throws RemoteException, IOException, ObjectNotFoundException, 
	PermissionDeniedException, FileExistsException, NotDirectoryException;
	public DeleteFileInfoRemote deleteObjectByPath(FilePath deletePath) throws RemoteException, IOException, ObjectNotFoundException, PermissionDeniedException, NotDirectoryException;

}