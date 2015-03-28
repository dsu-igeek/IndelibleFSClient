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
package com.igeekinc.indelible.indeliblefs.firehose.msgpack;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.msgpack.NetworkDataDescriptorMsgPack;
import com.igeekinc.indelible.indeliblefs.exceptions.CannotDeleteDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotFileException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSDirectoryHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFileHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSForkHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerConnectionHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleSnapshotIteratorHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleVersionIteratorHandle;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.FilePath;

public interface IndelibleFSFirehoseServerIF
{

	public CommandResult addClientSessionAuthentication(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connection,
			SessionAuthentication sessionAuthenticationToAdd)
			throws IOException;

	public CommandResult getSessionAuthentication(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle)
			throws IOException;

	public CommandResult openConnection(IndelibleFSClientInfoIF clientInfo)
			throws IOException;

	public CommandResult closeConnection(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle handle) throws IOException;

	public CommandResult getMoverAddresses(IndelibleFSClientInfoIF clientInfo,
			EntityID securityServerID);

	public CommandResult getClientEntityAuthentication(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle)
			throws IOException;

	public CommandResult listVolumes(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle)
			throws IOException;

	public CommandResult testReverseConnection(IndelibleFSClientInfoIF clientInfo,
			NetworkDataDescriptorMsgPack descriptor) throws IOException;

	public CommandResult retrieveVolume(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectIDMsgPack volumeID) throws IOException, VolumeNotFoundException;

	public CommandResult getMetaDataResource(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectHandle objectHandle, String resourceName)
			throws IOException, PermissionDeniedException;

	public CommandResult listMetaDataResources(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectHandle objectHandle) throws IOException,
			PermissionDeniedException;

	public CommandResult releaseHandle(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connection,
			IndelibleFSObjectHandle [] handle);

	public CommandResult getRoot(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSVolumeHandle volume) throws IOException, PermissionDeniedException;

	public CommandResult getObjectByPath(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle, IndelibleFSVolumeHandle volumeHandle, FilePath path)
					throws IOException, PermissionDeniedException, ObjectNotFoundException;

	public CommandResult getObjectByID(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSVolumeHandle volume, IndelibleFSObjectIDMsgPack id) throws IOException, PermissionDeniedException, ObjectNotFoundException;

	public CommandResult getObjectByVersionAndID(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSVolumeHandle volume, IndelibleFSObjectIDMsgPack id,
			IndelibleVersionMsgPack version, RetrieveVersionFlags flags) throws IOException, PermissionDeniedException, ObjectNotFoundException;

	public CommandResult createChildFile(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle parent, String name, boolean exclusive) throws IOException, PermissionDeniedException, FileExistsException;

	public CommandResult createChildFileWithInitialData(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle objectHandle, String name,
			Map<String, CASIDDataDescriptor> initialDataMap,
			boolean exclusive) throws IOException, PermissionDeniedException, FileExistsException;
	
	public CommandResult createChildDirectory(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle objectHandle, String name) throws IOException, PermissionDeniedException, FileExistsException;
	
	public CommandResult createChildFileFromExistingFile(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle objectHandle, String name,
			IndelibleFSFileHandle source, boolean exclusive) throws IOException, PermissionDeniedException, FileExistsException, NotFileException, ObjectNotFoundException;
	
	public CommandResult listDirectory(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle directoryHandle) throws IOException, PermissionDeniedException;

	public CommandResult getNumChildren(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle directoryHandle) throws IOException, PermissionDeniedException;

	public CommandResult getChildNode(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle objectHandle, String name) throws IOException, PermissionDeniedException, ObjectNotFoundException;

	public CommandResult totalLength(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSFileHandle fileHandle) throws IOException;

	public CommandResult getFork(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSFileHandle fileHandle, String forkName, boolean createIfNecessary) throws IOException, PermissionDeniedException, ForkNotFoundException;

	public CommandResult appendDataDescriptor(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle,
			NetworkDataDescriptor networkDataDescriptor) throws IOException;

	public CommandResult writeDataDescriptor(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle, long offset,
			NetworkDataDescriptor networkDataDescriptor) throws IOException;

	public CommandResult readDataDescriptor(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle, long offset, long length) throws IOException;

	public CommandResult startTransaction(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle) throws IOException;

	public CommandResult commitTransaction(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle) throws IOException;

	public CommandResult commitTransactionAndSnapshot(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			HashMap<String, Serializable> snapshotMetadata) throws IOException, PermissionDeniedException;

	public CommandResult rollbackTransaction(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle) throws IOException;

	public CommandResult inTransaction(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle) throws IOException;

	public CommandResult createVolume(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			Properties volumeProperties) throws IOException, PermissionDeniedException;

	public CommandResult deleteVolume(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectIDMsgPack volumeID) throws VolumeNotFoundException, IOException, PermissionDeniedException;
	
	public CommandResult truncate(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle, long truncateLength) throws IOException;

	public CommandResult flush(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle) throws IOException;

	public CommandResult deleteChildFile(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle objectHandle, String name) throws IOException,
			PermissionDeniedException, CannotDeleteDirectoryException;

	public CommandResult deleteChildDirectory(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle objectHandle, String name) throws IOException,
			PermissionDeniedException, NotDirectoryException;

	public CommandResult deleteObjectByPath(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSVolumeHandle volumeHandle, FilePath deletePath) throws IOException, ObjectNotFoundException,
			PermissionDeniedException, NotDirectoryException;

	public CommandResult extendFork(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle, long extendLength) throws IOException;

	public CommandResult getCurrentVersion(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectHandle objectHandle) throws IOException;

	public CommandResult getSnapshotInfo(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSVolumeHandle volume, IndelibleVersionMsgPack retrieveSnapshotVersion) throws IOException, PermissionDeniedException;

	public CommandResult addSnapshot(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connection,
			IndelibleFSVolumeHandle volume,
			IndelibleSnapshotInfoMsgPack snapshotInfo) throws IOException, PermissionDeniedException;

	public CommandResult getVersion(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectHandle objectHandle,
			IndelibleVersion indelibleVersion, RetrieveVersionFlags flags) throws IOException;

	public CommandResult length(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle) throws IOException;

	public CommandResult moveObject(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectHandle objectHandle, FilePath sourcePath,
			FilePath destinationPath) throws IOException, ObjectNotFoundException, 
			PermissionDeniedException, FileExistsException, NotDirectoryException;
	
	public CommandResult listVersions(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectHandle fileHandle) throws IOException;
	
	public CommandResult nextVersionListItems(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleVersionIteratorHandle handle) throws IOException;

	public CommandResult createChildSymlink(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle objectHandle, String name,
			String targetPath, boolean exclusive) throws IOException, PermissionDeniedException, FileExistsException, ObjectNotFoundException;

	public CommandResult lastModified(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSFileHandle fileHandle) throws IOException;

	public CommandResult getVolumeName(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connection,
			IndelibleFSVolumeHandle volumeHandle) throws IOException, PermissionDeniedException;

	public CommandResult getChildNodeInfo(IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSDirectoryHandle directoryHandle, String[] mdToRetrieve) throws IOException, PermissionDeniedException;

	public CommandResult setMetaDataResource(
			IndelibleFSClientInfoIF clientInfo,
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSObjectHandle fileHandle, String mdResourceName,
			Map<String, Object> resource) throws IOException, PermissionDeniedException;

	public CommandResult listSnapshots(IndelibleFSClientInfoIF clientInfo, IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSVolumeHandle volumeHandle) throws IOException, PermissionDeniedException;
	
	public CommandResult nextSnapshotListItems(IndelibleFSClientInfoIF clientInfo, 
			IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleSnapshotIteratorHandle handle) throws IOException, PermissionDeniedException;

	public CommandResult getCASServerPort(IndelibleFSClientInfoIF clientInfo);

	public CommandResult getServerID(IndelibleFSClientInfoIF clientInfo);

	public CommandResult getSegmentIDs(IndelibleFSClientInfoIF clientInfo, IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle) throws IOException;

	public CommandResult getName(IndelibleFSClientInfoIF clientInfo, IndelibleFSServerConnectionHandle connectionHandle,
			IndelibleFSForkHandle forkHandle) throws IOException;
}