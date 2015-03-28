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
package com.igeekinc.indelible.indeliblefs.firehose.proxies;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.DeleteFileInfo;
import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSObjectIF;
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
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSDirectoryHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFileHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSVolumeProxy extends IndelibleFSObjectProxy implements IndelibleFSVolumeIF
{
	public IndelibleFSVolumeProxy(IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeHandle remote)
	{
		super(client, connection, remote);
	}


	@Override
	public IndelibleFSVolumeHandle getHandle()
	{
		return (IndelibleFSVolumeHandle)super.getHandle();
	}


	@Override
	public IndelibleFSVolumeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		return (IndelibleFSVolumeIF)super.getVersion(version, flags);
	}


	@Override
	public IndelibleDirectoryNodeIF getRoot() throws PermissionDeniedException,
			IOException
	{
		ComboFutureBase<IndelibleFSDirectoryHandle>getRootFuture = new ComboFutureBase<IndelibleFSDirectoryHandle>();
		getClient().getRootAsync(getConnection(), getHandle(), getRootFuture, null);

		try
		{
			return (IndelibleDirectoryNodeIF) getProxyForRemote(this, getRootFuture.get());
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
	public IndelibleFileNodeIF getObjectByPath(FilePath path)
			throws ObjectNotFoundException, PermissionDeniedException,
			IOException
	{
		IndelibleFileNodeIF returnNode = null;
		if (returnNode == null)
		{
			ComboFutureBase<IndelibleFSFileHandle>getObjectFuture = new ComboFutureBase<IndelibleFSFileHandle>();
			getClient().getObjectByPathAsync(getConnection(), getHandle(), path, getObjectFuture, null);

			try
			{
				IndelibleFSFileHandle returnObjectHandle = getObjectFuture.get();
				returnNode = (IndelibleFileNodeIF) getProxyForRemote(this, returnObjectHandle);
			} catch (InterruptedException e)
			{
				throw new IOException("Operation was interrupted");
			} catch (ExecutionException e)
			{
				if (e.getCause() instanceof IOException)
					throw (IOException)e.getCause();
				if (e.getCause() instanceof ObjectNotFoundException)
					throw (ObjectNotFoundException)e.getCause();
				if (e.getCause() instanceof PermissionDeniedException)
					throw (PermissionDeniedException)e.getCause();
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new InternalError("Got unexpected error "+e.getCause().toString());
			}
		}
		return returnNode;
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
		ComboFutureBase<IndelibleFSFileHandle>getObjectFuture = new ComboFutureBase<IndelibleFSFileHandle>();
		getClient().getObjectByIDAsync(getConnection(), getHandle(), id, getObjectFuture, null);

		try
		{
			return (IndelibleFileNodeIF) getProxyForRemote(this, getObjectFuture.get());
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
	public IndelibleFileNodeIF getObjectByID(IndelibleFSObjectID id,
			IndelibleVersion version, RetrieveVersionFlags flags)
			throws IOException, ObjectNotFoundException
	{
		ComboFutureBase<IndelibleFSFileHandle>getObjectFuture = new ComboFutureBase<IndelibleFSFileHandle>();
		getClient().getObjectByIDAndVersionAsync(getConnection(), getHandle(), id, version, flags, getObjectFuture, null);

		try
		{
			return (IndelibleFileNodeIF) getProxyForRemote(this, getObjectFuture.get());
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

	/**
	 * Adapts between an completion handler that returns an IndelibleFileNodeIF and an async routine that
	 * returns an IndelibleFSFileHandle
	 * @author David L. Smith-Uchida
	 *
	 * @param <A>
	 */
	class IndelibleFileNodeCompletion<A> implements AsyncCompletion<IndelibleFSFileHandle, A>
	{
		AsyncCompletion<IndelibleFileNodeIF, A>completionHandler;
		A attachment;
		public IndelibleFileNodeCompletion(AsyncCompletion<IndelibleFileNodeIF, A>completionHandler, A attachment)
		{
			this.completionHandler = completionHandler;
			this.attachment = attachment;
		}
		@Override
		public void completed(IndelibleFSFileHandle result,
				Object ignored)
		{
			try
			{
				completionHandler.completed((IndelibleFileNodeIF)getProxyForRemote(IndelibleFSVolumeProxy.this, result), attachment);
			} catch (Throwable e)
			{
				// TODO Auto-generated catch block
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}

		@Override
		public void failed(Throwable exc,
				Object ignored)
		{
			completionHandler.failed(exc, attachment);
		}
	}
	
	@Override
	public <A> void getObjectByIDAsync(IndelibleFSObjectID id,
			IndelibleVersion version, RetrieveVersionFlags flags,
			AsyncCompletion<IndelibleFileNodeIF, A> completionHandler,
			A attachment) throws IOException, ObjectNotFoundException
	{
		IndelibleFileNodeCompletion<A> adapterCompletionHandler = new IndelibleFileNodeCompletion<A>(completionHandler, attachment);
		getClient().getObjectByIDAsync(getConnection(), getHandle(), id, adapterCompletionHandler, null);
	}

	@Override
	public Future<IndelibleFileNodeIF> getObjectByIDAsync(
			IndelibleFSObjectID id, IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException,
			ObjectNotFoundException
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void deleteObjectByID(IndelibleFSObjectID id) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public DeleteFileInfo deleteObjectByPath(FilePath deletePath)
			throws IOException, ObjectNotFoundException,
			PermissionDeniedException, NotDirectoryException
	{
		ComboFutureBase<DeleteFileInfo>deleteObjectFuture = new ComboFutureBase<DeleteFileInfo>();

		getClient().deleteObjectByPathAsync(getConnection(), this, deletePath, deleteObjectFuture, null);

		try
		{
			return (DeleteFileInfo)deleteObjectFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof ObjectNotFoundException)
				throw (ObjectNotFoundException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}

	}

	@Override
	public MoveObjectInfo moveObject(FilePath sourcePath,
			FilePath destinationPath) throws IOException,
			ObjectNotFoundException, PermissionDeniedException,
			FileExistsException, NotDirectoryException
	{
		ComboFutureBase<MoveObjectInfo>moveObjectFuture = new ComboFutureBase<MoveObjectInfo>();

		getClient().moveObjectAsync(getConnection(), this, sourcePath, destinationPath, moveObjectFuture, null);

		try
		{
			return moveObjectFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof ObjectNotFoundException)
				throw (ObjectNotFoundException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			if (e.getCause() instanceof FileExistsException)
				throw (FileExistsException)e.getCause();
			if (e.getCause() instanceof NotDirectoryException)
				throw (NotDirectoryException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public IndelibleFSObjectID getVolumeID()
	{
		return getHandle().getObjectID();
	}

	@Override
	public void setVolumeName(String volumeName)
			throws PermissionDeniedException, IOException
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public String getVolumeName() throws PermissionDeniedException, IOException
	{
		ComboFutureBase<String>getVolumeNameFuture = new ComboFutureBase<String>();
		getClient().getVolumeNameAsync(getConnection(), this.getHandle(), getVolumeNameFuture, null);
		try
		{
			return getVolumeNameFuture.get();
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
	public void setUserProperties(Properties propertiesToSet)
			throws PermissionDeniedException, IOException
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public IndelibleFSVolumeIF setMetaDataResource(String mdResourceName,
			Map<String, Object> resources)
			throws PermissionDeniedException, IOException
	{
		return (IndelibleFSVolumeIF)super.setMetaDataResource(mdResourceName, resources);
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
		ComboFutureBase<Void>addSnapshotFuture = new ComboFutureBase<Void>();
		getClient().addSnapshotAsync(getConnection(), getHandle(), snapshotInfo, addSnapshotFuture, null);
		try
		{
			addSnapshotFuture.get();
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
	public boolean releaseSnapshot(IndelibleVersion removeSnapshotVersion)
			throws PermissionDeniedException, IOException
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public IndelibleSnapshotInfo getInfoForSnapshot(
			IndelibleVersion retrieveSnapshotVersion)
			throws PermissionDeniedException, IOException
	{
		ComboFutureBase<IndelibleSnapshotInfo>getSnapshotInfoFuture = new ComboFutureBase<IndelibleSnapshotInfo>();
		getClient().getInfoForSnapshotAsync(getConnection(), getHandle(), retrieveSnapshotVersion, getSnapshotInfoFuture, null);
		try
		{
			return getSnapshotInfoFuture.get();
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
	public IndelibleSnapshotIterator listSnapshots()
			throws PermissionDeniedException, IOException
	{
		ComboFutureBase<IndelibleSnapshotIterator>listSnapshotsFuture = new ComboFutureBase<IndelibleSnapshotIterator>();
		getClient().listSnapshotsAsync(getConnection(), getHandle(), listSnapshotsFuture, null);
		try
		{
			return listSnapshotsFuture.get();
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
	protected IndelibleFSObjectIF getProxyForRemote(
			IndelibleFSObjectHandle handle)
	{
		return super.getProxyForRemote(this, handle);
	}
}
