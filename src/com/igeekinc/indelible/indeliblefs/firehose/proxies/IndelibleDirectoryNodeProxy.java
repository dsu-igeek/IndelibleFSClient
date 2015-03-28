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
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.msgpack.MessagePack;

import com.igeekinc.indelible.indeliblefs.CreateDirectoryInfo;
import com.igeekinc.indelible.indeliblefs.CreateFileInfo;
import com.igeekinc.indelible.indeliblefs.CreateSymlinkInfo;
import com.igeekinc.indelible.indeliblefs.DeleteFileInfo;
import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleNodeInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.exceptions.CannotDeleteDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotFileException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSDirectoryHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFileHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.IndelibleNodeInfoMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

class GetChildNodeInfoFuture extends ComboFutureBase<IndelibleNodeInfo[]>
{
	public GetChildNodeInfoFuture()
	{
		
	}
	
	public <A>GetChildNodeInfoFuture(AsyncCompletion<IndelibleNodeInfo[], ? super A>completionHandler, A attachment)
	{
		super(completionHandler, attachment);
	}
}

class GetChildNodeInfoCompletionHandler implements AsyncCompletion<NetworkDataDescriptor, Void>
{
	private static MessagePack packer = new MessagePack();
	private GetChildNodeInfoFuture gcniFuture;
	
	public GetChildNodeInfoCompletionHandler(GetChildNodeInfoFuture gcniFuture)
	{
		if (gcniFuture == null)
			throw new IllegalArgumentException("Must supply a GetChildNodeInfoFuture");
		this.gcniFuture = gcniFuture;
	}

	@Override
	public void completed(NetworkDataDescriptor result, Void attachment)
	{
		if (result == null)
			throw new IllegalArgumentException("Result cannot be null");
		try
		{
			byte [] childNodeData = result.getData();
			IndelibleNodeInfoMsgPack [] childInfoMsgPack = packer.read(childNodeData, IndelibleNodeInfoMsgPack[].class);
			IndelibleNodeInfo [] returnInfo = new IndelibleNodeInfo[childInfoMsgPack.length];
			for (int curNodeNum = 0; curNodeNum < childInfoMsgPack.length; curNodeNum++)
				returnInfo[curNodeNum] = childInfoMsgPack[curNodeNum].getNodeInfo();
			gcniFuture.completed(returnInfo, null);
		} catch (Throwable e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			failed(e, null);
		}
	}

	@Override
	public void failed(Throwable exc, Void attachment)
	{
		gcniFuture.failed(exc, null);
	}
	
	
}
public class IndelibleDirectoryNodeProxy extends IndelibleFileNodeProxy implements
		IndelibleDirectoryNodeIF
{

	private static MessagePack packer = new MessagePack();

	public IndelibleDirectoryNodeProxy(IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeProxy volume,
			IndelibleFSDirectoryHandle handle)
	{
		super(client, connection, volume, handle);
	}

	@Override
	public IndelibleFSDirectoryHandle getHandle()
	{
		return (IndelibleFSDirectoryHandle)super.getHandle();
	}

	@Override
	public boolean isDirectory()
	{
		return true;
	}

	@Override
	public boolean isFile()
	{
		return false;
	}

	@Override
	public CreateFileInfo createChildFile(String name, boolean exclusive)
			throws IOException, PermissionDeniedException, FileExistsException
	{
		ComboFutureBase<CreateFileInfo>createChildFileFuture = new ComboFutureBase<CreateFileInfo>();
		getClient().createChildFileAsync(getConnection(), this, name, exclusive, createChildFileFuture, null);
		try
		{
			return createChildFileFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			if (e.getCause() instanceof FileExistsException)
				throw (FileExistsException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public CreateFileInfo createChildFile(String name,
			Map<String, CASIDDataDescriptor> initialForkData,
			boolean exclusive) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException
	{
		Future<CreateFileInfo>createChildFileFuture = createChildFileAsync(name, initialForkData, exclusive);
		try
		{
			return createChildFileFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			if (e.getCause() instanceof FileExistsException)
				throw (FileExistsException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public Future<CreateFileInfo> createChildFileAsync(String name,
			Map<String, CASIDDataDescriptor> initialForkData,
			boolean exclusive) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException
	{
		ComboFutureBase<CreateFileInfo>createChildFileFuture = new ComboFutureBase<CreateFileInfo>();
		createChildFileAsync(name, initialForkData, exclusive, createChildFileFuture, null);
		return createChildFileFuture;
	}

	class ReleaseNetworkInitialForkDataCompletionHandler<A> implements AsyncCompletion<CreateFileInfo, A>
	{
		private IndelibleFSServerConnectionProxy connection;
		private AsyncCompletion<CreateFileInfo, ? super A>completionHandler;
		private Map<String, NetworkDataDescriptor>networkInitialForkData;

		public ReleaseNetworkInitialForkDataCompletionHandler(IndelibleFSServerConnectionProxy connection, 
				AsyncCompletion<CreateFileInfo, ? super A>completionHandler, Map<String, NetworkDataDescriptor>networkInitialForkData)
		{
			this.connection = connection;
			this.completionHandler = completionHandler;
			this.networkInitialForkData = networkInitialForkData;
		}

		@Override
		public void completed(CreateFileInfo result, A attachment)
		{
			releaseAll();
			completionHandler.completed(result, attachment);
		}

		@Override
		public void failed(Throwable exc, A attachment)
		{
			releaseAll();
			completionHandler.failed(exc, attachment);
		}
		
		private void releaseAll()
		{
			for (NetworkDataDescriptor releaseDescriptor:networkInitialForkData.values())
			{
				connection.removeDataDescriptor(releaseDescriptor);
			}
		}
	}
	
	@Override
	public <A> void createChildFileAsync(String name,
			Map<String, CASIDDataDescriptor> initialForkData,
			boolean exclusive,
			AsyncCompletion<CreateFileInfo, ? super A> completionHandler,
			A attachment) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException
	{
		HashMap<String, NetworkDataDescriptor>networkInitialForkData = new HashMap<String, NetworkDataDescriptor>();
		for (Map.Entry<String, CASIDDataDescriptor>curEntry:initialForkData.entrySet())
		{
			NetworkDataDescriptor curNetworkDescriptor = getConnection().registerDataDescriptor(curEntry.getValue());
			networkInitialForkData.put(curEntry.getKey(), curNetworkDescriptor);
		}
		ReleaseNetworkInitialForkDataCompletionHandler<A> releaseCompletionHandler = 
				new ReleaseNetworkInitialForkDataCompletionHandler<A>(getConnection(), completionHandler, networkInitialForkData);
		getClient().createChildFileAsync(getConnection(), this, name, networkInitialForkData, exclusive, releaseCompletionHandler, attachment);
	}

	@Override
	public CreateFileInfo createChildFile(String name,
			IndelibleFileNodeIF sourceFile, boolean exclusive)
			throws PermissionDeniedException, FileExistsException, IOException,
			NotFileException, ObjectNotFoundException
	{
		ComboFutureBase<CreateFileInfo>createChildFileFuture = new ComboFutureBase<CreateFileInfo>();
		getClient().createChildFileAsync(getConnection(), this, name, ((IndelibleFileNodeProxy)sourceFile).getHandle(), exclusive, createChildFileFuture, null);
		try
		{
			return createChildFileFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			if (e.getCause() instanceof FileExistsException)
				throw (FileExistsException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public CreateSymlinkInfo createChildSymlink(String name, String targetPath,
			boolean exclusive) throws PermissionDeniedException,
			FileExistsException, IOException, ObjectNotFoundException
	{
		ComboFutureBase<CreateSymlinkInfo>createChildSymlinkFuture = new ComboFutureBase<CreateSymlinkInfo>();
		getClient().createChildSymlinkAsync(getConnection(), this, name, targetPath, exclusive, createChildSymlinkFuture, null);
		try
		{
			return createChildSymlinkFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			if (e.getCause() instanceof FileExistsException)
				throw (FileExistsException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public CreateFileInfo createChildLink(String name,
			IndelibleFileNodeIF sourceFile) throws PermissionDeniedException,
			FileExistsException, IOException, NotFileException,
			ObjectNotFoundException
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public CreateDirectoryInfo createChildDirectory(String name)
			throws IOException, PermissionDeniedException, FileExistsException
	{
		ComboFutureBase<CreateDirectoryInfo>createDirectoryFuture = new ComboFutureBase<CreateDirectoryInfo>();
		getClient().createChildDirectoryAsync(getConnection(), this, name, createDirectoryFuture, null);
		try
		{
			return createDirectoryFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			if (e.getCause() instanceof FileExistsException)
				throw (FileExistsException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public DeleteFileInfo deleteChild(String name) throws IOException,
			PermissionDeniedException, CannotDeleteDirectoryException
	{
		ComboFutureBase<DeleteFileInfo>deleteFuture = new ComboFutureBase<DeleteFileInfo>();

		getClient().deleteChildAsync(getConnection(), this, name, deleteFuture, null);
		try
		{
			return deleteFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			if (e.getCause() instanceof CannotDeleteDirectoryException)
				throw (CannotDeleteDirectoryException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public DeleteFileInfo deleteChildDirectory(String name) throws IOException,
			PermissionDeniedException, NotDirectoryException
	{
		ComboFutureBase<DeleteFileInfo>deleteFuture = new ComboFutureBase<DeleteFileInfo>();

		getClient().deleteChildDirectoryAsync(getConnection(), this, name, deleteFuture, null);
		try
		{
			return deleteFuture.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			if (e.getCause() instanceof NotDirectoryException)
				throw (NotDirectoryException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public String[] list() throws IOException, PermissionDeniedException
	{
		ComboFutureBase<NetworkDataDescriptor>listFuture = new ComboFutureBase<NetworkDataDescriptor>();
		getClient().listAsync(getConnection(), this, listFuture, null);
		try
		{
			NetworkDataDescriptor listDataDescriptor = listFuture.get();
			byte [] listData = listDataDescriptor.getData();
			String [] returnList = packer.read(listData, String [].class);
			return returnList;
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
	public int getNumChildren() throws IOException, PermissionDeniedException
	{
		ComboFutureBase<Integer>getNumChildrenFuture = new ComboFutureBase<Integer>();
		getClient().getNumChildrenAsync(getConnection(), this, getNumChildrenFuture, null);
		
		try
		{
			return getNumChildrenFuture.get();
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
	public IndelibleFileNodeIF getChildNode(String name) throws IOException,
			PermissionDeniedException, ObjectNotFoundException
	{
		ComboFutureBase<IndelibleFSFileHandle>getObjectFuture = new ComboFutureBase<IndelibleFSFileHandle>();
		getClient().getChildNodeAsync(getConnection(), this, name, getObjectFuture, null);

		try
		{
			return (IndelibleFileNodeIF) getProxyForRemote((IndelibleFSVolumeProxy) getVolume(), getObjectFuture.get());
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
	public IndelibleNodeInfo[] getChildNodeInfo(String[] mdToRetrieve)
			throws IOException, PermissionDeniedException, RemoteException
	{
		Future<IndelibleNodeInfo []> future = getChildNodeInfoAsync(mdToRetrieve);
		try
		{
			return future.get();
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
		/*ComboFutureBase<NetworkDataDescriptor>getChildNodeInfoFuture = new ComboFutureBase<NetworkDataDescriptor>();
		getClient().getChildNodeInfoAsync(getConnection(), this, mdToRetrieve, getChildNodeInfoFuture, null);
		try
		{
			NetworkDataDescriptor childNodeDataDescriptor = getChildNodeInfoFuture.get();
			byte [] childNodeData = childNodeDataDescriptor.getData();
			IndelibleNodeInfoMsgPack [] childInfoMsgPack = packer.read(childNodeData, IndelibleNodeInfoMsgPack[].class);
			IndelibleNodeInfo [] returnInfo = new IndelibleNodeInfo[childInfoMsgPack.length];
			for (int curNodeNum = 0; curNodeNum < childInfoMsgPack.length; curNodeNum++)
				returnInfo[curNodeNum] = childInfoMsgPack[curNodeNum].getNodeInfo();
			return returnInfo;
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
		}*/
	}

	@Override
	public Future<IndelibleNodeInfo []> getChildNodeInfoAsync(String[] mdToRetrieve) throws IOException, PermissionDeniedException, RemoteException
	{
		GetChildNodeInfoFuture returnFuture = new GetChildNodeInfoFuture();
		getChildNodeInfoAsyncCommon(mdToRetrieve, returnFuture);
		return returnFuture;
	}
	
	@Override
	public <A> void getChildNodeInfoAsync( String[] mdToRetrieve,
			AsyncCompletion<IndelibleNodeInfo[], ? super A> completionHandler,
			A attachment) throws IOException, PermissionDeniedException, RemoteException
	{
		GetChildNodeInfoFuture future = new GetChildNodeInfoFuture(completionHandler, attachment);
		getChildNodeInfoAsyncCommon(mdToRetrieve, future);
	}
	
	protected void getChildNodeInfoAsyncCommon(String[] mdToRetrieve,
			GetChildNodeInfoFuture future) throws IOException, PermissionDeniedException, RemoteException
	{
		getClient().getChildNodeInfoAsync(getConnection(), this, mdToRetrieve, new GetChildNodeInfoCompletionHandler(future), null);

	}
	@Override
	public IndelibleDirectoryNodeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		return (IndelibleDirectoryNodeIF)super.getVersion(version, flags);
	}
}
