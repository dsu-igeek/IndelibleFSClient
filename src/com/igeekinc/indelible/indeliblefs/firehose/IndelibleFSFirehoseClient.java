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
package com.igeekinc.indelible.indeliblefs.firehose;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.CreateDirectoryInfo;
import com.igeekinc.indelible.indeliblefs.CreateFileInfo;
import com.igeekinc.indelible.indeliblefs.CreateSymlinkInfo;
import com.igeekinc.indelible.indeliblefs.DeleteFileInfo;
import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.MoveObjectInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotIterator;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.NoMoverPathException;
import com.igeekinc.indelible.indeliblefs.exceptions.CannotDeleteDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.DirectoryNotEmptyException;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.InTransactionException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotFileException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.AddClientSessionAuthenticationMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.AddSnapshotMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.AppendDataDescriptorMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CloseConnectionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CommitTransactionAndSnapshotMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CommitTransactionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CreateChildDirectoryMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CreateChildFileFromExistingFileMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CreateChildFileMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CreateChildFileWithInitialDataMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CreateChildSymlinkMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.CreateVolumeMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.DeleteChildDirectoryMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.DeleteChildFileMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.DeleteObjectByPathMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.DeleteVolumeMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.ExtendMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.FlushMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetCASServerPortMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetChildNodeInfoMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetChildNodeMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetClientEntityAuthenticationMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetCurrentVersionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetDataDescriptorMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetForkLengthMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetForkMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetForkNameMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetInfoForSnapshotMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetMetaDataResourceMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetMoverAddressesMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetNumChildrenMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetObjectByIDAndVersionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetObjectByIDMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetObjectByPathMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetObjectLastModified;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetObjectLength;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetRootMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetSegmentIDsMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetServerIDMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetSessionAuthenticationMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetVersionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.GetVolumeNameMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.InTransactionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.IndelibleFSCommandMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.ListDirectoryMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.ListMetaDataResourcesMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.ListSnapshotsMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.ListVersionsMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.ListVolumesMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.MoveObjectMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.NextSnapshotListItemsMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.NextSnapshotListItemsReply;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.NextVersionListItemsMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.NextVersionListItemsReply;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.OpenConnectionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.ReleaseHandleMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.RetrieveVolumeMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.RollbackTransactionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.SetMetaDataResource;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.StartTransactionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.TestReverseConnectionMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.TruncateMessage;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.WriteDataDescriptorMessage;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleDirectoryNodeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSVolumeProxy;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

/**
 * Handles the communications with the IndelibleFS server.  Should be accessed via the proxy objects.  Most calls are
 * asynchronous
 * @author David L. Smith-Uchida
 *
 */
public class IndelibleFSFirehoseClient extends AuthenticatedFirehoseClient
{
	
	private Timer finalizeTimer = null;
	private ArrayList<IndelibleFSServerConnectionProxy>connectionsWithFinalizedObjects = new ArrayList<IndelibleFSServerConnectionProxy>();
	public IndelibleFSFirehoseClient(SocketAddress address) throws IOException
	{
		super(address);
	}
	
	/*
	 * Server operations
	 */
	
	public <A> void openConnectionAsync(AsyncCompletion<IndelibleFSServerConnectionHandle, A>completionHandler, A attachment) throws PermissionDeniedException, IOException
	{
		OpenConnectionMessage<A> openConnectionMessage = new OpenConnectionMessage<A>(completionHandler, attachment);
		sendMessage(openConnectionMessage);
	}
	
	public <A>void getServerIDAsync(AsyncCompletion<EntityID, A>completionHandler, A attachment) throws IOException
	{
		GetServerIDMessage<A> getServerIDMessage = new GetServerIDMessage<A>(completionHandler, attachment);
		sendMessage(getServerIDMessage);
	}

    // These two calls support testing the server to client mover connection (pulls for writes) and setting
    // up a reverse connection
    public <A> void getMoverAddressesAsync(EntityID securityServerID, AsyncCompletion<InetSocketAddress [], A>completionHandler, A attachment) throws IOException
    {
    	GetMoverAddressesMessage<A> getMoverAddressesMessage = new GetMoverAddressesMessage<A>(securityServerID, completionHandler, attachment);
		sendMessage(getMoverAddressesMessage);
    }
    
	public <A>void testReverseConnectionAsync(NetworkDataDescriptor testNetworkDescriptor, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		TestReverseConnectionMessage<A> testReverseConnectionMessage = new TestReverseConnectionMessage<A>(testNetworkDescriptor, completionHandler, attachment);
		sendMessage(testReverseConnectionMessage);
	}
	
	/*
	 * Connection operations
	 */

	
	public <A> void closeConnectionAsync(IndelibleFSServerConnectionProxy connection, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		CloseConnectionMessage<A>closeConnectionMessage = new CloseConnectionMessage<A>(connection.getHandle(), completionHandler, attachment);
		sendMessage(closeConnectionMessage);
	}
	
	public <A> void createVolumeAsync(IndelibleFSServerConnectionProxy connection, Properties volumeProperties, AsyncCompletion<IndelibleFSVolumeIF, A>completionHandler, A attachment)
			throws IOException, PermissionDeniedException
	{
		CreateVolumeMessage<A> createVolumeMessage = new CreateVolumeMessage<A>(this, connection, volumeProperties, completionHandler, attachment);
		sendMessage(createVolumeMessage);
	}

	public <A> void deleteVolumeAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSObjectID deleteVolumeID,
			AsyncCompletion<Void, A> completionHandler, A attachment) throws VolumeNotFoundException, PermissionDeniedException, IOException
	{
		DeleteVolumeMessage<A> deleteVolumeMessage = new DeleteVolumeMessage<A>(this, connection, deleteVolumeID, completionHandler, attachment);
		sendMessage(deleteVolumeMessage);
	}
	
	public <A> void retrieveVolumeAsync(IndelibleFSServerConnectionProxy connection,
			IndelibleFSObjectID retrieveVolumeID, AsyncCompletion<IndelibleFSVolumeHandle, A>completionHandler, A attachment)
			throws VolumeNotFoundException, IOException
	{
		RetrieveVolumeMessage<A>retrieveVolumeMessage = new RetrieveVolumeMessage<A>(this, connection, retrieveVolumeID, completionHandler, attachment);
		sendMessage(retrieveVolumeMessage);
	}

	
	public <A> void listVolumesAsync(IndelibleFSServerConnectionProxy connection, AsyncCompletion<IndelibleFSObjectID[], A>completionHandler, A attachment) throws IOException
	{
		ListVolumesMessage<A> listVolumesMessage = new ListVolumesMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(listVolumesMessage);
	}

	
	public <A> void startTransactionAsync(IndelibleFSServerConnectionProxy connection, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		StartTransactionMessage<A>startTransactionMessage = new StartTransactionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(startTransactionMessage);
	}
	
	public <A>void inTransaction(IndelibleFSServerConnectionProxy connection,
			AsyncCompletion<Boolean, A> completionHandler, A attachment) throws IOException
	{
		InTransactionMessage<A>inTransactionMessage = new InTransactionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(inTransactionMessage);
	}

	public <A> void commitAsync(IndelibleFSServerConnectionProxy connection, AsyncCompletion<IndelibleVersion, A>completionHandler, A attachment) throws IOException
	{
		CommitTransactionMessage<A>commitTransactionMessage = new CommitTransactionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(commitTransactionMessage);
	}

	
	public <A> void commitAndSnapshot(IndelibleFSServerConnectionProxy connection, Map<String, Serializable>snapshotMetadata, AsyncCompletion<IndelibleVersion, A>completionHandler, A attachment) throws IOException, PermissionDeniedException
	{
		CommitTransactionAndSnapshotMessage<A>commitTransactionMessage = new CommitTransactionAndSnapshotMessage<A>(this, connection, snapshotMetadata, completionHandler, attachment);
		sendMessage(commitTransactionMessage);
	}

	
	public <A>void rollbackAsync(IndelibleFSServerConnectionProxy connection, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		RollbackTransactionMessage<A>rollbackTransactionMessage = new RollbackTransactionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(rollbackTransactionMessage);
	}

	
	public <A>void closeAsync(AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	
	public <A> void getClientEntityAuthenticationAsync(IndelibleFSServerConnectionProxy connection, AsyncCompletion<EntityAuthentication, A>completionHandler, A attachment) throws IOException
	{
		GetClientEntityAuthenticationMessage<A> getClientEntityAuthenticationMessage = new GetClientEntityAuthenticationMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(getClientEntityAuthenticationMessage);
	}

	
	public <A> void getServerEntityAuthenticationAsync(AsyncCompletion<EntityAuthentication, A>completionHandler, A attachment) throws IOException
	{
		throw new UnsupportedOperationException();
	}
	
	
	public <A> void addClientSessionAuthenticationAsync(IndelibleFSServerConnectionProxy connection, SessionAuthentication sessionAuthentication, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		AddClientSessionAuthenticationMessage<A> addClientSessionAuthenticationMessage = new AddClientSessionAuthenticationMessage<A>(this, connection, sessionAuthentication, completionHandler, attachment);
		sendMessage(addClientSessionAuthenticationMessage);
	}

	
	public <A> void getSessionAuthenticationAsync(IndelibleFSServerConnectionProxy connection, AsyncCompletion<SessionAuthentication, A>completionHandler, A attachment) throws IOException
	{
		GetSessionAuthenticationMessage<A> getSessionAuthenticationMessage = new GetSessionAuthenticationMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(getSessionAuthenticationMessage);
	}

	/*
	 * Generic file object operations
	 */
	public <A> void listMetaDataResourcesAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSObjectHandle objectHandle, AsyncCompletion<String [], A>completionHandler, A attachment) throws PermissionDeniedException, IOException
	{
		ListMetaDataResourcesMessage<A> listMetaDataResourcesMessage = new ListMetaDataResourcesMessage<A>(this, connection, objectHandle, completionHandler, attachment);
		sendMessage(listMetaDataResourcesMessage);
	}

	public <A> void getMetaDataResourceAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSObjectHandle objectID, String mdResourceName, AsyncCompletion<Map<String, Object>, A>completionHandler, A attachment)
			throws PermissionDeniedException, IOException
	{
		GetMetaDataResourceMessage<A> getMetaDataResourceMessage = new GetMetaDataResourceMessage<A>(this, connection, objectID, mdResourceName, completionHandler, attachment);
		sendMessage(getMetaDataResourceMessage);
	}

	public <A> void setMetaDataResourceAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSObjectHandle objectHandle, String mdResourceName, Map<String, Object> resource, AsyncCompletion<IndelibleFSObjectHandle, A>completionHandler, A attachment)
			throws PermissionDeniedException, IOException
	{
		SetMetaDataResource<A> setMetaDataResourceMessage = new SetMetaDataResource<A>(this, connection, objectHandle, mdResourceName, resource, completionHandler, attachment);
		sendMessage(setMetaDataResourceMessage);
	}


	public <A> void listVersionsAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSObjectHandle object, AsyncCompletion<IndelibleVersionIterator, A>completionHandler, A attachment) throws IOException
	{
		ListVersionsMessage<A>listVersionsMessage = new ListVersionsMessage<A>(this, connection, object, completionHandler, attachment);
		sendMessage(listVersionsMessage);
	}

	public <A> void nextVersionsListItem(IndelibleFSServerConnectionProxy connection, IndelibleVersionIteratorHandle iterator, AsyncCompletion<NextVersionListItemsReply, A>completionHandler, A attachment) throws IOException
	{
		NextVersionListItemsMessage<A> nextVersionsMessage = new NextVersionListItemsMessage<A>(this, connection, iterator, completionHandler, attachment);
		sendMessage(nextVersionsMessage);
	}
	public <A> void getCurrentVersionAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSObjectHandle objectHandle, AsyncCompletion<IndelibleVersion, A>completionHandler, A attachment) throws IOException
	{
		GetCurrentVersionMessage<A>getCurrentVersionMessage = new GetCurrentVersionMessage<A>(this, connection, objectHandle, completionHandler, attachment);
		sendMessage(getCurrentVersionMessage);
	}
	

	public <A> void getVersionAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSObjectHandle object, IndelibleVersion version,
			RetrieveVersionFlags flags, AsyncCompletion<IndelibleFSObjectHandle, A>completionHandler, A attachment) throws IOException
	{
		GetVersionMessage<A>getVersionMessage = new GetVersionMessage<A>(this, connection, object, version, flags, completionHandler, attachment);
		sendMessage(getVersionMessage);
	}
	
	/*
	 * Volume operations
	 */
	
	public <A> void getRootAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, AsyncCompletion<IndelibleFSDirectoryHandle, A>completionHandler, A attachment) throws PermissionDeniedException, IOException
	{
		GetRootMessage<A> getRootMessage = new GetRootMessage<A>(this, connection, volume, completionHandler, attachment);
		sendMessage(getRootMessage);
	}
	
	public <A> void getObjectByPathAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, FilePath path, AsyncCompletion<IndelibleFSFileHandle, A>completionHandler, A attachment)
			throws ObjectNotFoundException, PermissionDeniedException,
			IOException
	{
		GetObjectByPathMessage<A> getObjectByPathMessage = new GetObjectByPathMessage<A>(this, connection, volume, path, completionHandler, attachment);
		sendMessage(getObjectByPathMessage);
	}

	public <A> void getObjectByIDAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, IndelibleFSObjectID id, AsyncCompletion<IndelibleFSFileHandle, A>completionHandler, A attachment)
			throws IOException, ObjectNotFoundException
	{
		throw new UnsupportedOperationException();
	}

	public <A> void getObjectByIDAndVersionAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, IndelibleFSObjectID id,
			IndelibleVersion version, RetrieveVersionFlags flags, AsyncCompletion<IndelibleFSFileHandle, A>completionHandler, A attachment)
			throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public <A> void deleteObjectByIDAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, IndelibleFSObjectID id, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public <A> void setVolumeNameAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, String volumeName, AsyncCompletion<Void, A>completionHandler, A attachment)
			throws PermissionDeniedException
	{
		throw new UnsupportedOperationException();
	}

	public <A> void getVolumeNameAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, AsyncCompletion<String, A>completionHandler, A attachment) throws PermissionDeniedException, IOException
	{
		GetVolumeNameMessage<A>getVolumeNameMessage = new GetVolumeNameMessage<A>(this, connection, volume, completionHandler, attachment);
		sendMessage(getVolumeNameMessage);
	}

	public <A> void setUserPropertiesAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, Properties propertiesToSet, AsyncCompletion<Void, A>completionHandler, A attachment)
			throws PermissionDeniedException
	{
		throw new UnsupportedOperationException();

	}

	public <A> void setMetaDataResourceAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, String mdResourceName,
		Map<String, Object> resources, AsyncCompletion<IndelibleFSVolumeHandle, A>completionHandler, A attachment)
			throws PermissionDeniedException, IOException
	{
		throw new UnsupportedOperationException();
	}

	public <A> void addSnapshotAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, IndelibleSnapshotInfo snapshotInfo, AsyncCompletion<Void, A>completionHandler, A attachment)
			throws PermissionDeniedException, IOException
	{
		AddSnapshotMessage<A> addSnapshotMessage = new AddSnapshotMessage<A>(this, connection, volume, snapshotInfo, completionHandler, attachment);
		sendMessage(addSnapshotMessage);
	}

	public <A> void releaseSnapshotAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, IndelibleVersion removeSnapshotVersion, AsyncCompletion<Boolean, A>completionHandler, A attachment)
			throws PermissionDeniedException, IOException
	{
		throw new UnsupportedOperationException();
	}

	public <A> void getInfoForSnapshotAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume,
			IndelibleVersion retrieveSnapshotVersion, AsyncCompletion<IndelibleSnapshotInfo, A>completionHandler, A attachment)
			throws PermissionDeniedException, IOException
	{
		GetInfoForSnapshotMessage<A> getInfoForSnapshotMessage = new GetInfoForSnapshotMessage<A>(this, connection, volume, retrieveSnapshotVersion, completionHandler, attachment);
		sendMessage(getInfoForSnapshotMessage);
	}

	public <A> void listSnapshotsAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeHandle volume, AsyncCompletion<IndelibleSnapshotIterator, A>completionHandler, A attachment)
			throws PermissionDeniedException, IOException
	{
		ListSnapshotsMessage<A> listSnapshotsMessage = new ListSnapshotsMessage<A>(this, connection, volume, completionHandler, attachment);
		sendMessage(listSnapshotsMessage);
	}

	public <A> void deleteObjectByPathAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeProxy volume, FilePath deletePath, AsyncCompletion<DeleteFileInfo, A>completionHandler, A attachment)
			throws IOException, ObjectNotFoundException,
			PermissionDeniedException, NotDirectoryException
	{
		DeleteObjectByPathMessage<A>deleteObjectByPathMessage = new DeleteObjectByPathMessage<A>(this, connection, volume, deletePath, completionHandler, attachment);
		sendMessage(deleteObjectByPathMessage);
	}

	public <A> void moveObjectAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSVolumeProxy volume, FilePath sourcePath,
			FilePath destinationPath, AsyncCompletion<MoveObjectInfo, A>completionHandler, A attachment) throws IOException,
			ObjectNotFoundException, PermissionDeniedException,
			FileExistsException, NotDirectoryException
	{
		MoveObjectMessage<A>moveObjectMessage = new MoveObjectMessage<A>(this, connection, volume, sourcePath, destinationPath, completionHandler, attachment);
		sendMessage(moveObjectMessage);
	}
	
	/*
	 * File operations
	 */
	public <A> void isDirectoryAsync(IndelibleFSServerConnectionProxy connection, DeleteFileInfo file, AsyncCompletion<Boolean, A>completionHandler, A attachment)
	{
		throw new UnsupportedOperationException();
	}

	public <A> void isFileAsync(IndelibleFSServerConnectionProxy connection, DeleteFileInfo file, AsyncCompletion<Boolean, A>completionHandler, A attachment)
	{
		throw new UnsupportedOperationException();
	}

	public <A> void getForkAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSFileHandle file, String name, boolean createIfNecessary, AsyncCompletion<IndelibleFSForkIF, A>completionHandler, A attachment)
			throws IOException, ForkNotFoundException
	{
		GetForkMessage<A>getForkMessage = new GetForkMessage<A>(this, connection, file, name, createIfNecessary, completionHandler, attachment);
		sendMessage(getForkMessage);
	}

	public <A> void deleteForkAsync(IndelibleFSServerConnectionProxy connection, DeleteFileInfo file, String forkName, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException, ForkNotFoundException, PermissionDeniedException
	{
		throw new UnsupportedOperationException();

	}
	
	public <A> void listForkNamesAsync(IndelibleFSServerConnectionProxy connection, DeleteFileInfo file, AsyncCompletion<String [], A>completionHandler, A attachment) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public <A> void getVolumeForFileAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSFileHandle file,  AsyncCompletion<IndelibleFSVolumeHandle, A>completionHandler, A attachment) throws IOException
	{
		throw new UnsupportedOperationException();

	}

	public <A> void lastModifiedAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSFileHandle fileHandle, AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		GetObjectLastModified<A>getLastModifiedMessage = new GetObjectLastModified<A>(this, connection, fileHandle, completionHandler, attachment);
		sendMessage(getLastModifiedMessage);
	}

	public <A> void lengthAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSFileHandle fileHandle, AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		GetObjectLength<A>getLengthMessage = new GetObjectLength<A>(this, connection, fileHandle, GetObjectLength.kDataForkLength, completionHandler, attachment);
		sendMessage(getLengthMessage);	}

	public <A> void totalLengthAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSFileHandle fileHandle, AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		GetObjectLength<A>getTotalLengthMessage = new GetObjectLength<A>(this, connection, fileHandle, GetObjectLength.kTotalLength, completionHandler, attachment);
		sendMessage(getTotalLengthMessage);
	}

	public <A> void lengthWithChildrenAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSFileHandle fileHandle, AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		GetObjectLength<A>getLengthWithChildrenMessage = new GetObjectLength<A>(this, connection, fileHandle, GetObjectLength.kTotalLengthWithChildren, completionHandler, attachment);
		sendMessage(getLengthWithChildrenMessage);
	}
	
	/*
	 * Directory routines
	 */
	
	public <A> void createChildFileAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy parent, String name, boolean exclusive, AsyncCompletion<CreateFileInfo, A>completionHandler, A attachment)
			throws IOException, PermissionDeniedException, FileExistsException
	{
		CreateChildFileMessage<A> createChildFileMessage = new CreateChildFileMessage<A>(this, connection, parent, name, exclusive, completionHandler, attachment);
		sendMessage(createChildFileMessage);
	}

	
	public <A> void createChildFileAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy parent, String name,
			Map<String, NetworkDataDescriptor> initialData,
			boolean exclusive, AsyncCompletion<CreateFileInfo, A>completionHandler, A attachment) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException
	{
		
		CreateChildFileWithInitialDataMessage<A>createChildFileWithInitialDataMessage = new CreateChildFileWithInitialDataMessage<A>(this, connection, parent, name, initialData, exclusive, completionHandler, attachment);
		sendMessage(createChildFileWithInitialDataMessage);
	}

	
	public <A> void createChildFileAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy parent, String name,
			IndelibleFSFileHandle sourceFile, boolean exclusive, AsyncCompletion<CreateFileInfo, A>completionHandler, A attachment)
			throws PermissionDeniedException, FileExistsException, IOException,
			NotFileException, ObjectNotFoundException
	{
		CreateChildFileFromExistingFileMessage<A>creatChildFromExistingFileMessage = new CreateChildFileFromExistingFileMessage<A>(this, connection, parent, name,
				sourceFile, exclusive, completionHandler, attachment);
		sendMessage(creatChildFromExistingFileMessage);
	}

	
	public <A> void createChildSymlinkAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy parent, String name,
			String targetPath, boolean exclusive, AsyncCompletion<CreateSymlinkInfo, A>completionHandler, A attachment)
			throws PermissionDeniedException, FileExistsException, IOException, ObjectNotFoundException
	{
		CreateChildSymlinkMessage<A>createChildSymlinkMessage = new CreateChildSymlinkMessage<A>(this, connection, parent, name, targetPath, exclusive, completionHandler, attachment);
		sendMessage(createChildSymlinkMessage);
	}

	
	public <A> void createChildLinkAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSDirectoryHandle directory, String name,
			DeleteFileInfo sourceFile, AsyncCompletion<CreateFileInfo, A>completionHandler, A attachment)
			throws PermissionDeniedException, FileExistsException, IOException,
			NotFileException, ObjectNotFoundException
	{
		throw new UnsupportedOperationException();

	}

	
	public <A> void createChildDirectoryAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy parent, String name, AsyncCompletion<CreateDirectoryInfo, A>completionHandler, A attachment)
			throws IOException, PermissionDeniedException, FileExistsException
	{
		CreateChildDirectoryMessage<A> createChildFileMessage = new CreateChildDirectoryMessage<A>(this, connection, parent, name, completionHandler, attachment);
		sendMessage(createChildFileMessage);
	}

	
	public <A> void deleteChildAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy parent, String name, AsyncCompletion<DeleteFileInfo, A>completionHandler, A attachment) throws IOException,
			PermissionDeniedException, CannotDeleteDirectoryException
	{
		DeleteChildFileMessage<A> deleteChildFileMessage = new DeleteChildFileMessage<A>(this, connection, parent, name, completionHandler, attachment);
		sendMessage(deleteChildFileMessage);
	}

	
	public <A> void deleteChildDirectoryAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy parent, String name, AsyncCompletion<DeleteFileInfo, A>completionHandler, A attachment) throws IOException,
			PermissionDeniedException, NotDirectoryException
	{
		DeleteChildDirectoryMessage<A> deleteChildDirectoryMessage = new DeleteChildDirectoryMessage<A>(this, connection, parent, name, completionHandler, attachment);
		sendMessage(deleteChildDirectoryMessage);
	}

	
	public <A> void listAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy directory, AsyncCompletion<NetworkDataDescriptor, A>completionHandler, A attachment) throws IOException, PermissionDeniedException
	{
		ListDirectoryMessage<A> listDirectoryMessage = new ListDirectoryMessage<A>(this, connection, directory, completionHandler, attachment);
		sendMessage(listDirectoryMessage);
	}

	
	public <A> void getNumChildrenAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy directory, AsyncCompletion<Integer, A>completionHandler, A attachment) throws IOException
	{
		GetNumChildrenMessage<A>getNumChildrenMessage = new GetNumChildrenMessage<A>(this, connection, directory, completionHandler, attachment);
		sendMessage(getNumChildrenMessage);
	}
	
	public <A> void getChildNodeAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy directory, String name, AsyncCompletion<IndelibleFSFileHandle, A>completionHandler, A attachment) throws IOException, PermissionDeniedException, ObjectNotFoundException
	{
		GetChildNodeMessage<A> getChildNodeMessage = new GetChildNodeMessage<A>(this, connection, directory, name, completionHandler, attachment);
		sendMessage(getChildNodeMessage);
	}

	
	public <A> void getChildNodeInfoAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy directory, String[] mdToRetrieve, AsyncCompletion<NetworkDataDescriptor, A>completionHandler, A attachment)
			throws IOException, PermissionDeniedException, RemoteException
	{
		GetChildNodeInfoMessage<A>getChildNodeInfoMessage = new GetChildNodeInfoMessage<A>(this, connection, directory, mdToRetrieve, completionHandler, attachment);
		sendMessage(getChildNodeInfoMessage);
	}

	protected <A> void getRemoteForkDataAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy directory, Map<String, CASIDDataDescriptor>localForkData, AsyncCompletion<Map<String, CASIDDataDescriptor>, A>completionHandler, A attachment)
	{
		throw new UnsupportedOperationException();

	}
	
	protected <A> void releaseRemoteForkDataAsync(IndelibleFSServerConnectionProxy connection, IndelibleDirectoryNodeProxy directory, Map<String, CASIDDataDescriptor>remoteForkData, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		throw new UnsupportedOperationException();

	}
	
	/*
	 * Fork operations
	 */
	public <A> void getDataDescriptorAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, long offset, long length, AsyncCompletion<CASIDDataDescriptor, A>completionHandler, A attachment)
			throws IOException
	{
		GetDataDescriptorMessage<A> getDataDescriptorMessage = new GetDataDescriptorMessage<A>(this, connection, fork, offset, length, completionHandler, attachment);
		sendMessage(getDataDescriptorMessage);
	}

	public <A> void writeDataDescriptorAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, long offset, NetworkDataDescriptor source, AsyncCompletion<Void, A>completionHandler, A attachment)
			throws IOException
	{
		WriteDataDescriptorMessage<A>writeMessage = new WriteDataDescriptorMessage<A>(this, connection, fork, offset, source, completionHandler, attachment);
		sendMessage(writeMessage);
	}
	
	public <A> void appendDataDescriptorAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, NetworkDataDescriptor source, AsyncCompletion<Void, A>completionHandler, A attachment)
			throws IOException
	{
		AppendDataDescriptorMessage<A>appendMessage = new AppendDataDescriptorMessage<A>(this, connection, fork, source, completionHandler, attachment);
		sendMessage(appendMessage);

	}
	
	public <A> void flushAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		FlushMessage<A>flushMessage = new FlushMessage<A>(this, connection, fork, completionHandler, attachment);
		sendMessage(flushMessage);
	}

	
	public <A> void truncateAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, long truncateLength, AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		TruncateMessage<A> truncateMessage = new TruncateMessage<A>(this, connection, fork, truncateLength, completionHandler, attachment);
		sendMessage(truncateMessage);
	}

	
	public <A> void extendAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, long extendLength, AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		ExtendMessage<A> extendMessage = new ExtendMessage<A>(this, connection, fork, extendLength, completionHandler, attachment);
		sendMessage(extendMessage);
	}

	
	public <A> void lengthAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		GetForkLengthMessage<A>getForkLengthMessage = new GetForkLengthMessage<A>(this, connection, fork, completionHandler, attachment);
		sendMessage(getForkLengthMessage);
	}

	
	public <A> void getSegmentIDsAsync(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, AsyncCompletion<CASIdentifier[], A>completionHandler, A attachment) throws IOException
	{
		GetSegmentIDsMessage<A>getSegmentIDsMessage = new GetSegmentIDsMessage<A>(this, connection, fork, completionHandler, attachment);
		sendMessage(getSegmentIDsMessage);
	}

	
	public <A> void getName(IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle fork, AsyncCompletion<String, A>completionHandler, A attachment) throws IOException
	{
		GetForkNameMessage<A>getForkNameMessage = new GetForkNameMessage<A>(this, connection, fork, completionHandler, attachment);
		sendMessage(getForkNameMessage);
	}

	public <A>void proxyFinalizedAsync(
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSObjectHandle [] handles, AsyncCompletion<Void, A> completionHandler,
			A attachment) throws IOException
	{
		ReleaseHandleMessage<A> releaseHandleMessage = new ReleaseHandleMessage<A>(connection.getHandle(), handles);
		sendMessage(releaseHandleMessage, completionHandler, attachment);
	}
	

	public <A> void nextSnapshotListItem(IndelibleFSServerConnectionProxy connection, IndelibleSnapshotIteratorHandle handle,
			AsyncCompletion<NextSnapshotListItemsReply, A>completionHandler, A attachment) throws IOException
	{
		NextSnapshotListItemsMessage<A> nextSnapshotListItemsMessage = new NextSnapshotListItemsMessage<A>(this, connection, handle, completionHandler, attachment);
		sendMessage(nextSnapshotListItemsMessage);
	}
	
	public <A> void getCASServerPort(AsyncCompletion<Integer, A>completionHandler, A attachment) throws IOException
	{
		GetCASServerPortMessage<A> openConnectionMessage = new GetCASServerPortMessage<A>(completionHandler, attachment);
		sendMessage(openConnectionMessage);
	}
	@Override
	protected Class<? extends Object> getReturnClassForCommandCode(int commandCode)
	{
		return IndelibleFSFirehoseClient.getReturnClassForCommandCodeStatic(commandCode);
	}
	private static HashMap<Integer, Class<? extends Object>>returnClassMap = new HashMap<Integer, Class<? extends Object>>();

	static
	{
		// Load the map and fail early if there's a mistake
		for (IndelibleFSServerCommand checkCommand:IndelibleFSServerCommand.values())
		{
			if (checkCommand != IndelibleFSServerCommand.kIllegalCommand)
				getReturnClassForCommandCodeStatic(checkCommand.commandNum);	// This exercises both getReturnClassForCommandCodeStatic and getClassForCommandCode
		}
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })	// Warnings?  We don't need no stinkin' warnings.
	public static Class<? extends Object> getReturnClassForCommandCodeStatic(int commandCode)
	{
		synchronized(returnClassMap)
		{
			Class<? extends Object> returnClass = returnClassMap.get(commandCode);
			if (returnClass == null)
			{
				Class<? extends IndelibleFSCommandMessage>commandClass = getClassForCommandCode(commandCode);
				try
				{
					returnClass = commandClass.getConstructor().newInstance().getResultClass();
					returnClassMap.put(commandCode, returnClass);
				} catch (Throwable e)
				{
					Logger.getLogger(IndelibleFSFirehoseClient.class).error(new ErrorLogMessage("Caught exception"), e);
					throw new InternalError(IndelibleFSServerCommand.getCommandForNum(commandCode)+" not configured");
				} 
			}
			return returnClass;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static Class<? extends IndelibleFSCommandMessage> getClassForCommandCode(int commandCode)
	{
		switch (IndelibleFSServerCommand.getCommandForNum(commandCode))
		{
		case kIllegalCommand:
			throw new IllegalArgumentException("Illegal command 0");
		case kCloseConnection:
			return CloseConnectionMessage.class;
		case kCreateVolume:
			return CreateVolumeMessage.class;
		case kListVolumes:
			return ListVolumesMessage.class;
		case kOpenConnection:
			return OpenConnectionMessage.class;
		case kRetrieveVolume:
			return RetrieveVolumeMessage.class;
		case kAddClientSessionAuthentication:
			return AddClientSessionAuthenticationMessage.class;
		case kGetSessionAuthentication:
			return GetSessionAuthenticationMessage.class;
		case kTestReverseConnection:
			return TestReverseConnectionMessage.class;
		case kGetMoverAddresses:
			return GetMoverAddressesMessage.class;
		case kGetClientEntityAuthentication:
			return GetClientEntityAuthenticationMessage.class;
		case kGetMetaDataResource:
			return GetMetaDataResourceMessage.class;
		case kListMetaDataResources:
			return ListMetaDataResourcesMessage.class;
		case kReleaseHandle:
			return ReleaseHandleMessage.class;
		case kGetObjectByID:
			return GetObjectByIDMessage.class;
		case kGetObjectByIDAndVersion:
			return GetObjectByIDAndVersionMessage.class;
		case kGetObjectByPath:
			return GetObjectByPathMessage.class;
		case kGetRoot:
			return GetRootMessage.class;
		case kCreateChildFile:
			return CreateChildFileMessage.class;
		case kListDirectory:
			return ListDirectoryMessage.class;
		case kGetChildNode:
			return GetChildNodeMessage.class;
		case kGetTotalLength:
			return GetObjectLength.class;
		case kGetFork:
			return GetForkMessage.class;
		case kAppendDataDescriptor:
			return AppendDataDescriptorMessage.class;
		case kWriteDataDescriptor:
			return WriteDataDescriptorMessage.class;
		case kGetDataDescriptor:
			return GetDataDescriptorMessage.class;
		case kFlush:
			return FlushMessage.class;
		case kExtend:
			return ExtendMessage.class;
		case kTruncate:
			return TruncateMessage.class;
		case kStartTransaction:
			return StartTransactionMessage.class;
		case kCommitTransaction:
			return CommitTransactionMessage.class;
		case kCommitTransactionAndSnapshot:
			return CommitTransactionAndSnapshotMessage.class;
		case kRollbackTransaction:
			return RollbackTransactionMessage.class;
		case kInTransaction:
			return InTransactionMessage.class;
		case kCreateChildFileWithInitialData:
			return CreateChildFileWithInitialDataMessage.class;
		case kCreateChildFileFromExistingFile:
			return CreateChildFileFromExistingFileMessage.class;
		case kCreateDirectory:
			return CreateChildDirectoryMessage.class;
		case kDeleteChildFile:
			return DeleteChildFileMessage.class;
		case kDeleteChildDirectory:
			return DeleteChildDirectoryMessage.class;
		case kDeleteObjectByPath:
			return DeleteObjectByPathMessage.class;
		case kGetCurrentVersion:
			return GetCurrentVersionMessage.class;
		case kGetInfoForSnapshot:
			return GetInfoForSnapshotMessage.class;
		case kAddSnapshot:
			return AddSnapshotMessage.class;
		case kGetVersion:
			return GetVersionMessage.class;
		case kGetLength:
			return GetForkLengthMessage.class;
		case kMoveObject:
			return MoveObjectMessage.class;
		case kListVersions:
			return ListVersionsMessage.class;
		case kNextVersionListItems:
			return NextVersionListItemsMessage.class;
		case kCreateChildSymlink:
			return CreateChildSymlinkMessage.class;
		case kGetLastModified:
			return GetObjectLastModified.class;
		case kGetVolumeName:
			return GetVolumeNameMessage.class;
		case kGetChildNodeInfo:
			return GetChildNodeInfoMessage.class;
		case kSetMetaDataResource:
			return SetMetaDataResource.class;
		case kNextSnapshotListItems:
			return NextSnapshotListItemsMessage.class;
		case kListSnapshots:
			return ListSnapshotsMessage.class;
		case kGetCASServerPort:
			return GetCASServerPortMessage.class;
		case kGetNumChildren:
			return GetNumChildrenMessage.class;
		case kGetServerID:
			return GetServerIDMessage.class;
		case kGetSegmentIDs:
			return GetSegmentIDsMessage.class;
		case kGetForkName:
			return GetForkNameMessage.class;
		case kDeleteVolume:
			return DeleteVolumeMessage.class;
		default:
			break;
		
		}
		throw new InternalError(IndelibleFSServerCommand.getCommandForNum(commandCode)+" not configured");
	}

	private void sendMessage(IndelibleFSCommandMessage<?, ?, ?> message) throws IOException
	{
		// IndelibleFSCommandMessage is the completion handler, a dessert topping and a floor wax!
		sendMessage(message, message, null);
	}

	@Override
	public int getExtendedErrorCodeForThrowable(Throwable t)
	{
		return IndelibleFSFirehoseClient.getExtendedErrorCodeForThrowableStatic(t);
	}

	@Override
	public Throwable getExtendedThrowableForErrorCode(int errorCode)
	{
		return IndelibleFSFirehoseClient.getExtendedThrowableForErrorCodeStatic(errorCode);
	}
	
	private static final int	kNoMoverPathException	= kExtendedErrorStart;
	private static final int	kObjectNotFoundException = kExtendedErrorStart + 1;
	private static final int 	kPermissionDeniedException = kObjectNotFoundException + 1;
	private static final int	kCannotDeleteDirectoryException = kPermissionDeniedException + 1;
	private static final int	kNotDirectoryException = kCannotDeleteDirectoryException + 1;
	private static final int	kFileExistsException = kNotDirectoryException + 1;
	private static final int	kForkNotFoundException = kFileExistsException + 1;
	private static final int    kCollectionNotFoundException = kForkNotFoundException + 1;
	private static final int	kVolumeNotFoundException = kCollectionNotFoundException + 1;
	private static final int	kDirectoryNotEmptyException = kVolumeNotFoundException + 1;
	private static final int	kForkExistsException = kDirectoryNotEmptyException + 1;
	private static final int	kInTransactionException = kForkExistsException + 1;
	private static final int	kNotFileException = kInTransactionException + 1;
	
	public static int getExtendedErrorCodeForThrowableStatic(Throwable t)
	{
		Class<? extends Throwable> throwableClass = t.getClass();
		if (throwableClass.equals(NoMoverPathException.class))
			return kNoMoverPathException;
		if (throwableClass.equals(ObjectNotFoundException.class))
			return kObjectNotFoundException;
		if (throwableClass.equals(PermissionDeniedException.class))
			return kPermissionDeniedException;
		if (throwableClass.equals(CannotDeleteDirectoryException.class))
			return kCannotDeleteDirectoryException;
		if (throwableClass.equals(NotDirectoryException.class))
			return kNotDirectoryException;
		if (throwableClass.equals(FileExistsException.class))
			return kFileExistsException;
		if (throwableClass.equals(ForkNotFoundException.class))
			return kForkNotFoundException;
		if (throwableClass.equals(CollectionNotFoundException.class))
			return kCollectionNotFoundException;
		if (throwableClass.equals(VolumeNotFoundException.class))
			return kVolumeNotFoundException;
		if (throwableClass.equals(DirectoryNotEmptyException.class))
			return kDirectoryNotEmptyException;
		if (throwableClass.equals(ForkExistsException.class))
			return kForkExistsException;
		if (throwableClass.equals(InTransactionException.class))
			return kInTransactionException;
		if (throwableClass.equals(NotFileException.class))
			return kNotFileException;
		return -1;
	}
	
	public static Throwable getExtendedThrowableForErrorCodeStatic(int errorCode)
	{
		switch(errorCode)
		{
		case kNoMoverPathException:
			return new NoMoverPathException();
		case kObjectNotFoundException:
			return new ObjectNotFoundException();
		case kPermissionDeniedException:
			return new PermissionDeniedException();
		case kCannotDeleteDirectoryException:
			return new CannotDeleteDirectoryException();
		case kNotDirectoryException:
			return new NotDirectoryException();
		case kFileExistsException:
			return new FileExistsException();
		case kForkNotFoundException:
			return new ForkNotFoundException();
		case kCollectionNotFoundException:
			return new CollectionNotFoundException();
		case kVolumeNotFoundException:
			return new VolumeNotFoundException();
		case kDirectoryNotEmptyException:
			return new DirectoryNotEmptyException();
		case kForkExistsException:
			return new ForkExistsException("");
		case kInTransactionException:
			return new InTransactionException();
		case kNotFileException:
			return new NotFileException();
		default:
			return null;
		}
	}

	public synchronized void proxyFinalized(
			IndelibleFSServerConnectionProxy indelibleFSServerConnectionProxy)
	{
		// Release objects can get a little busy so we try to batch the releases together so as not to
		// overwhelm the server with release messages
		if (!connectionsWithFinalizedObjects.contains(indelibleFSServerConnectionProxy))
			connectionsWithFinalizedObjects.add(indelibleFSServerConnectionProxy);
		if (finalizeTimer == null)
		{
			finalizeTimer = new Timer("IndelibleFSFirehoseClient finalizer", true);
			scheduleRelease();
		}
	}

	private void scheduleRelease()
	{
		finalizeTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				releaseFinalizedProxies();
			}
		}, 100);
	}

	/*
	 * Runs through the list of connections with finalized objects and notifies the server they've been released
	 */
	private void releaseFinalizedProxies()
	{
		boolean keepRunning;

		do
		{
			keepRunning = false;
			ArrayList<ReleaseHandleMessage<Void>>releaseMessages = new ArrayList<ReleaseHandleMessage<Void>>();
			synchronized(this)
			{
				Iterator<IndelibleFSServerConnectionProxy>connectionIterator = connectionsWithFinalizedObjects.iterator();
				while(connectionIterator.hasNext())
				{
					IndelibleFSServerConnectionProxy curConnection = connectionIterator.next();
					while (curConnection.getNumObjectsToRelease() > 0)
					{
						IndelibleFSObjectHandle [] releaseHandles = curConnection.popObjectsToRelease(64);
						if (releaseHandles.length > 0)
						{
							ReleaseHandleMessage<Void> releaseHandleMessage = new ReleaseHandleMessage<Void>(curConnection.getHandle(), releaseHandles);
							releaseMessages.add(releaseHandleMessage);
						}
					}
					connectionIterator.remove();
				}
			}
			for (ReleaseHandleMessage<Void>curMessage:releaseMessages)
			{
				ComboFutureBase<Void>releaseFuture = new ComboFutureBase<Void>();
				try
				{
					sendMessage(curMessage, releaseFuture, null);
					releaseFuture.get(0, TimeUnit.MILLISECONDS);	// Don't let this timeout
				} catch (IOException e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				} catch (InterruptedException e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				} catch (ExecutionException e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				} catch (TimeoutException e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				}
			}
			synchronized(this)
			{
				for (IndelibleFSServerConnectionProxy curConnection:connectionsWithFinalizedObjects)
				{
					IndelibleFSObjectHandle [] releaseHandles = curConnection.getObjectsToRelease();
					if (releaseHandles.length > 0)
					{
						keepRunning = true;
						break;
					}
				}
				if (!keepRunning)
				{
					finalizeTimer.cancel();
					finalizeTimer = null;	// No work and we'll recreate the timer when there are more objects to finalize
				}
			}
		} while (keepRunning);
	}
	
	public String toString()
	{
		try
		{
			return "IndelibleFSFirehoseClient " + getServerEntityID() + "  (" + getServerAddress()+")";
		} catch (IOException e)
		{
			return "IndelibleFSFirehoseClient - cannot connect to server";
		}
	}
}
