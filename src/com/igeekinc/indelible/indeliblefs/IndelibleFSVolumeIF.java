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
 
package com.igeekinc.indelible.indeliblefs;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotIterator;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.async.AsyncCompletion;

public interface IndelibleFSVolumeIF extends IndelibleFSObjectIF
{
	public static final String kVolumeResourcesName = "com.igeekinc.indelible.indeliblefs.core.volumeResources";
	public static final String kVolumeNamePropertyName = "com.igeekinc.indelible.indeliblefs.core.volumeName";
	public static final String kVolumeSnapshotsPropertyName = "com.igeekinc.indelible.indeliblefs.core.volumeSnapshots";

	public abstract IndelibleFSVolumeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException;
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSVolumeREmote#getRoot()
	 */
	public abstract IndelibleDirectoryNodeIF getRoot()
			throws PermissionDeniedException, IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSVolumeREmote#getObjectByPath(com.igeekinc.util.FilePath)
	 */
	public abstract IndelibleFileNodeIF getObjectByPath(FilePath path)
			throws ObjectNotFoundException, PermissionDeniedException,
			IOException;

	public abstract IndelibleFileNodeIF createNewFile() throws IOException;

	public abstract IndelibleFileNodeIF createNewFile(IndelibleFileNodeIF sourceFile)
			throws IOException;

	public abstract IndelibleDirectoryNodeIF createNewDirectory()
			throws IOException;

	public abstract IndelibleSymlinkNodeIF createNewSymlink(String targetPath)
			throws IOException;

	/**
	 * Returns the latest version of the object
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws ObjectNotFoundException 
	 */
	public abstract IndelibleFileNodeIF getObjectByID(IndelibleFSObjectID id)
			throws IOException, ObjectNotFoundException;

	/**
	 * Retrieves a file node by version
	 * @param id
	 * @param version - The version to retrieve (null = latest)
	 * @param flags - Get the exact version specified if kExact is set, otherwise the one where version <= requestedVersion
	 * @return The found object & version or null if the if or exact version/id cannot be found
	 * @throws IOException
	 * @throws ObjectNotFoundException 
	 */
	public abstract IndelibleFileNodeIF getObjectByID(IndelibleFSObjectID id,
			IndelibleVersion version, RetrieveVersionFlags flags)
			throws IOException, ObjectNotFoundException;

	public abstract <A> void getObjectByIDAsync(IndelibleFSObjectID id, IndelibleVersion version, RetrieveVersionFlags flags,
			AsyncCompletion<IndelibleFileNodeIF, A>completionHandler, A attachment) throws IOException, ObjectNotFoundException;
	
	public abstract Future<IndelibleFileNodeIF> getObjectByIDAsync(IndelibleFSObjectID id, IndelibleVersion version, RetrieveVersionFlags flags) 
			throws IOException, ObjectNotFoundException;
	public abstract void deleteObjectByID(IndelibleFSObjectID id)
			throws IOException;

	public abstract DeleteFileInfo deleteObjectByPath(FilePath deletePath) throws IOException, ObjectNotFoundException, PermissionDeniedException, NotDirectoryException;
	
	/**
	 * Moves a file from one path to another.  If the existing path exists, the call will fail with a FileExistsException.
	 * @param sourcePath
	 * @param destinationPath
	 * @return
	 * @throws IOException
	 * @throws ObjectNotFoundException
	 * @throws PermissionDeniedException
	 * @throws FileExistsException
	 * @throws NotDirectoryException 
	 */
	public abstract MoveObjectInfo moveObject(FilePath sourcePath, FilePath destinationPath) throws IOException, ObjectNotFoundException, 
	PermissionDeniedException, FileExistsException, NotDirectoryException;
	
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSVolumeREmote#getVolumeID()
	 */
	public abstract IndelibleFSObjectID getVolumeID();

	public abstract void setVolumeName(String volumeName)
			throws PermissionDeniedException, IOException;

	public abstract String getVolumeName() throws PermissionDeniedException, IOException;

	/**
	 * Sets all user properties in VolumeResources for the volume based on the properties in propertiesToSet. Existing values
	 * which are not in propertiesToSet are retained.  See kUserSettableProperties for properties that can be
	 * set.
	 * @param propertiesToSet
	 * @throws PermissionDeniedException
	 */
	public abstract void setUserProperties(Properties propertiesToSet)
			throws PermissionDeniedException, IOException;

	public abstract IndelibleFSVolumeIF setMetaDataResource(
			String mdResourceName, Map<String, Object> resources)
			throws PermissionDeniedException, IOException;

	public abstract IndelibleVersionIterator listVersionsForObject(
			IndelibleFSObjectID id) throws IOException;

	public abstract void addSnapshot(IndelibleSnapshotInfo snapshotInfo) throws PermissionDeniedException, IOException;
	
	public abstract boolean releaseSnapshot(
			IndelibleVersion removeSnapshotVersion)
			throws PermissionDeniedException, IOException;

	public abstract IndelibleSnapshotInfo getInfoForSnapshot(
			IndelibleVersion retrieveSnapshotVersion)
			throws PermissionDeniedException, IOException;

	public abstract IndelibleSnapshotIterator listSnapshots()
			throws PermissionDeniedException, IOException;

}