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
 
package com.igeekinc.indelible.indeliblefs.firehose.proxies;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverReceiver;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession.DataDescriptorAvailability;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSource;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.NoMoverPathException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerConnectionHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.DebugLogMessage;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSServerConnectionProxy implements IndelibleServerConnectionIF
{
	private IndelibleFSFirehoseClient wrappedServer;
	private IndelibleFSServerConnectionHandle indeibleFSServerConnectionHandle;
	private DataMoverSession moverSession;
	private LinkedList<IndelibleFSObjectHandle>releaseList = new LinkedList<IndelibleFSObjectHandle>();
	private Logger logger = Logger.getLogger(getClass());
	private boolean inTransaction;
	private HashMap<String, Object>perTransactionData;
	
	/**
	 * Handles a remote IndelibleFSServerConnection
	 * @param wrappedServer	- the Firehose client
	 * @param preferredServerAddress - the address to contact us on for reverse connections
	 * @throws IOException
	 * @throws PermissionDeniedException 
	 * @throws AuthenticationFailureException 
	 */
	public IndelibleFSServerConnectionProxy(IndelibleFSFirehoseClient wrappedServer, InetAddress preferredServerAddress) throws IOException, PermissionDeniedException, AuthenticationFailureException
	{
		this.wrappedServer = wrappedServer;
		ComboFutureBase<IndelibleFSServerConnectionHandle>openFuture = new ComboFutureBase<IndelibleFSServerConnectionHandle>();
		wrappedServer.openConnectionAsync(openFuture, null);
		try
		{
			this.indeibleFSServerConnectionHandle = openFuture.get();
		} catch (InterruptedException e1)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
			throw new IOException("open connection was interrupted");
		} catch (ExecutionException e1)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
			throw new IOException("Could not open connection to server cause = "+e1.getCause());
		}
        EntityAuthentication serverAuthentication = wrappedServer.getServerAuthentication();
        EntityID securityServerID = wrappedServer.getConnectionAuthenticationServerID();
		moverSession = DataMoverSource.getDataMoverSource().createDataMoverSession(securityServerID);
        try
		{
        	/*
        	 * Authorize the server to read data from us
        	 */
			SessionAuthentication sessionAuthentication = moverSession.addAuthorizedClient(serverAuthentication);
			ComboFutureBase<Void>addSessionAuthenticationFuture = new ComboFutureBase<Void>();
			wrappedServer.addClientSessionAuthenticationAsync(this, sessionAuthentication, addSessionAuthenticationFuture, null);
			addSessionAuthenticationFuture.get();
			wrappedServer.addClientSessionAuthenticationAsync(this, sessionAuthentication, addSessionAuthenticationFuture, null);
			addSessionAuthenticationFuture.get();
			
			/*
			 * Now, get the authentication that allows us to read data from the server
			 */
			ComboFutureBase<SessionAuthentication> getSessionAuthenticationFuture = new ComboFutureBase<SessionAuthentication>();
			wrappedServer.getSessionAuthenticationAsync(this, getSessionAuthenticationFuture, null);
			SessionAuthentication remoteAuthentication = getSessionAuthenticationFuture.get();
			DataMoverReceiver.getDataMoverReceiver().addSessionAuthentication(remoteAuthentication);
			
			/*
			 * Ideally, the mover would simply establish connections on demand.  However, things like firewalls
			 * and/or NAT can interfere with our ability to establish a TCP connection.  Furthermore, the way
			 * firewalls are typically setup today, the connection is not quickly rejected but instead must
			 * timeout.  So, we try the local connection first.  If that fails, then we try establishing a reverse connection
			 * because if we're in a NAT situation, we are probably on the NAT side (since we're the client) and should
			 * be able to establish a connection.  If the reverse connection fails, then just to be certain there is no
			 * connectivity, we try to estable a normal connection.
			 */
			byte [] testBuffer = new byte[32*1024];
			CASIDDataDescriptor testDescriptor = new CASIDMemoryDataDescriptor(testBuffer);
			NetworkDataDescriptor testNetworkDescriptor = null;
			ExecutionException e = null;
			boolean testSuccessful = false;
			try
			{
				testNetworkDescriptor = moverSession.registerDataDescriptor(testDescriptor, DataDescriptorAvailability.kLocalOnlyAccess);
			}
			catch (IllegalArgumentException ie)
			{
				logger.debug("No local session available");
			}
			if (testNetworkDescriptor != null)
			{
				try
				{
					logger.error(new ErrorLogMessage("Attempting to establish local connection"));
					ComboFutureBase<Void>testReverseConnectionCompletion = new ComboFutureBase<Void>();
					wrappedServer.testReverseConnectionAsync(testNetworkDescriptor, testReverseConnectionCompletion, null);
					testReverseConnectionCompletion.get();
					logger.error(new ErrorLogMessage("Local connection succeeded"));
					testSuccessful = true;
				}
				catch (ExecutionException ee)
				{
					if (ee.getCause() instanceof NoMoverPathException)
					{
						e = ee;
					}
					else
					{
						throw ee.getCause();
					}

				}
			}
			if (!testSuccessful)
			{
				logger.debug(new DebugLogMessage("No local connection, trying reverse network connection first"));
				ComboFutureBase<InetSocketAddress []>getMoverAddressesCompletion = new ComboFutureBase<InetSocketAddress[]>();
				wrappedServer.getMoverAddressesAsync(securityServerID, getMoverAddressesCompletion, null);
				InetSocketAddress [] moverAddresses = getMoverAddressesCompletion.get(30, TimeUnit.SECONDS);
				if (moverAddresses == null || moverAddresses.length == 0)
					throw new NoMoverPathException();
				testNetworkDescriptor = moverSession.registerDataDescriptor(testDescriptor, DataDescriptorAvailability.kAllAccess);
				try
				{
					logger.error(new ErrorLogMessage("Attempting to establish reverse connection"));
					DataMoverSource.getDataMoverSource().openReverseConnection(securityServerID, preferredServerAddress, moverAddresses[0].getPort());
					ComboFutureBase<Void>testReverseConnectionCompletion = new ComboFutureBase<Void>();
					wrappedServer.testReverseConnectionAsync(testNetworkDescriptor, testReverseConnectionCompletion, null);
					testReverseConnectionCompletion.get(10, TimeUnit.SECONDS);
					logger.error(new ErrorLogMessage("Reverse connection succeeded"));
				} catch (Throwable t)
				{
					logger.error(new ErrorLogMessage("Reverse connection failed, trying forward connection"), t);
					ComboFutureBase<Void>testReverseConnectionCompletion = new ComboFutureBase<Void>();
					wrappedServer.testReverseConnectionAsync(testNetworkDescriptor, testReverseConnectionCompletion, null);
					testReverseConnectionCompletion.get(10, TimeUnit.SECONDS);
				}
			}
		} catch (Throwable t)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), t);
			throw new InternalError("Could not authorize client to mover");
		}
	}
	
	@Override
	public IndelibleFSVolumeIF createVolume(Properties volumeProperties)
			throws IOException, PermissionDeniedException
	{
		ComboFutureBase<IndelibleFSVolumeIF>createVolumeFuture = new ComboFutureBase<IndelibleFSVolumeIF>();
		wrappedServer.createVolumeAsync(this, volumeProperties, createVolumeFuture, null);
		try
		{
			return createVolumeFuture.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public void deleteVolume(IndelibleFSObjectID deleteVolumeID) throws VolumeNotFoundException, PermissionDeniedException, IOException
	{
		ComboFutureBase<Void>createVolumeFuture = new ComboFutureBase<Void>();
		wrappedServer.deleteVolumeAsync(this, deleteVolumeID, createVolumeFuture, null);
		try
		{
			createVolumeFuture.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public IndelibleFSVolumeIF retrieveVolume(
			IndelibleFSObjectID retrieveVolumeID)
			throws VolumeNotFoundException, IOException
	{
		ComboFutureBase<IndelibleFSVolumeHandle>listVolumesFuture = new ComboFutureBase<IndelibleFSVolumeHandle>();
		wrappedServer.retrieveVolumeAsync(this, retrieveVolumeID, listVolumesFuture, null);
		try
		{
			return new IndelibleFSVolumeProxy(wrappedServer, this, listVolumesFuture.get());
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof VolumeNotFoundException)
				throw (VolumeNotFoundException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public IndelibleFSObjectID[] listVolumes() throws IOException
	{
		ComboFutureBase<IndelibleFSObjectID[]>listVolumesFuture = new ComboFutureBase<IndelibleFSObjectID[]>();
		wrappedServer.listVolumesAsync(this, listVolumesFuture, null);
		try
		{
			return listVolumesFuture.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public synchronized void startTransaction() throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		wrappedServer.startTransactionAsync(this, future, null);
		try
		{
			future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public boolean inTransaction() throws IOException
	{
		return inTransaction;
	}

	@Override
	public synchronized IndelibleVersion commit() throws IOException
	{
		ComboFutureBase<IndelibleVersion>future = new ComboFutureBase<IndelibleVersion>();
		wrappedServer.commitAsync(this, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
		finally
		{
			inTransaction = false;
			perTransactionData = null;
		}
	}

	@Override
	public IndelibleVersion commitAndSnapshot(HashMap<String, Serializable>snapshotMetadata) throws IOException, PermissionDeniedException
	{
		ComboFutureBase<IndelibleVersion>future = new ComboFutureBase<IndelibleVersion>();
		wrappedServer.commitAndSnapshot(this, snapshotMetadata, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public synchronized void rollback() throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		wrappedServer.rollbackAsync(this, future, null);
		try
		{
			future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
		finally
		{
			inTransaction = false;
			perTransactionData = null;
		}
	}

	@Override
	public void close() throws IOException
	{
		ComboFutureBase<Void>closeFuture = new ComboFutureBase<Void>();
		wrappedServer.closeConnectionAsync(this, closeFuture, null);
		try
		{
			closeFuture.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public EntityAuthentication getClientEntityAuthentication() throws IOException
	{
		ComboFutureBase<EntityAuthentication>getClientEntityAuthenticationFuture = new ComboFutureBase<EntityAuthentication>();
		wrappedServer.getClientEntityAuthenticationAsync(this, getClientEntityAuthenticationFuture, null);
		try
		{
			return getClientEntityAuthenticationFuture.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public EntityAuthentication getServerEntityAuthentication() throws IOException, AuthenticationFailureException
	{
		return wrappedServer.getServerAuthentication();
	}
	
	@Override
	public void addClientSessionAuthentication(SessionAuthentication sessionAuthentication) throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		wrappedServer.addClientSessionAuthenticationAsync(this, sessionAuthentication, future, null);
		try
		{
			future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public SessionAuthentication getSessionAuthentication() throws IOException
	{
		ComboFutureBase<SessionAuthentication>future = new ComboFutureBase<SessionAuthentication>();
		wrappedServer.getSessionAuthenticationAsync(this, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Operation interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public NetworkDataDescriptor registerDataDescriptor(DataDescriptor localDataDescriptor)
	{
		return moverSession.registerDataDescriptor(localDataDescriptor);
	}
	
	@Override
	public NetworkDataDescriptor registerNetworkDescriptor(byte [] bytes)
	{
		return registerNetworkDescriptor(bytes, 0, bytes.length);
	}
	
	@Override
	public NetworkDataDescriptor registerNetworkDescriptor(byte [] bytes, int offset, int length)
	{
		return registerNetworkDescriptor(ByteBuffer.wrap(bytes, offset, length));
	}
	
	@Override
	public NetworkDataDescriptor registerNetworkDescriptor(ByteBuffer byteBuffer)
	{
		CASIDMemoryDataDescriptor baseDescriptor = new CASIDMemoryDataDescriptor(byteBuffer);
		return moverSession.registerDataDescriptor(baseDescriptor);
	}
	
	@Override
	public void removeDataDescriptor(DataDescriptor writeDescriptor)
	{
		moverSession.removeDataDescriptor(writeDescriptor);
	}
	
	@Override
	public DataMoverSession getMoverSession()
	{
		return moverSession;
	}
	
	/**
	 * The connection on the server sides maintains hard references to all objects so that long running or
	 * stalled transactions don't result in garbage collected objects.  We release the objects here when they
	 * get finalized on our side
	 * @param releaseProxy
	 */
	void proxyFinalized(IndelibleFSObjectProxy releaseProxy)
	{
		synchronized(releaseList)
		{
			releaseList.add(releaseProxy.getHandle());
		}
		wrappedServer.proxyFinalized(this);
	}
	
	public void proxyFinalized(IndelibleVersionIteratorProxy indelibleVersionIteratorProxy)
	{
		
	}
	
	public void proxyFinalized(IndelibleSnapshotIteratorProxy indelibleSnapshotIteratorProxy)
	{
		
	}
	public IndelibleFSObjectHandle [] getObjectsToRelease()
	{
		synchronized(releaseList)
		{
			IndelibleFSObjectHandle[] returnList = releaseList.toArray(new IndelibleFSObjectHandle[releaseList.size()]);
			releaseList.clear();
			return returnList;
		}
	}
	
	public IndelibleFSObjectHandle [] popObjectsToRelease(int maxObjects)
	{
		synchronized(releaseList)
		{
			IndelibleFSObjectHandle[] returnList;
			if (maxObjects > releaseList.size())
			{
				returnList = releaseList.toArray(new IndelibleFSObjectHandle[releaseList.size()]);
				releaseList.clear();
			}
			else
			{
				returnList = new IndelibleFSObjectHandle[maxObjects];
				for (int curObjectNum = 0; curObjectNum < maxObjects; curObjectNum++)
				{
					returnList[curObjectNum] = releaseList.remove(0);
				}
			}
			return returnList;
		}
	}
	
	public int getNumObjectsToRelease()
	{
		synchronized(releaseList)
		{
			return releaseList.size();
		}
	}
	public IndelibleFSServerConnectionHandle getHandle()
	{
		return indeibleFSServerConnectionHandle;
	}

	@Override
	public synchronized void setPerTransactionData(String key, Object value)
	{
		if (inTransaction)
		{
			if (perTransactionData == null)
				perTransactionData = new HashMap<String, Object>();
			perTransactionData.put(key, value);
		}
	}

	@Override
	public synchronized Object getPerTransactionData(String key)
	{
		if (inTransaction)
		{
			if (perTransactionData != null)
				return perTransactionData.get(key);
		}
		return null;
	}
}
