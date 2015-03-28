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
package com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSource;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.NoMoverPathException;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventListener;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventSupport;
import com.igeekinc.indelible.indeliblefs.events.MultipleQueueDispatcher;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASCollectionConnectionHandle;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerConnectionHandle;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CASCollectionQueuedEventMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CreateNewCollectionReply;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

class EventListenerRunnable implements Runnable
{
	CASServerConnectionProxy proxy;
	long lastEventID;
	public EventListenerRunnable(CASServerConnectionProxy proxy, long startingEventID)
	{
		this.proxy = proxy;
		this.lastEventID = startingEventID;
	}
	public void run()
	{
		int failed = 0;
		while(true && failed < 10)
		{
			try
			{
				IndelibleEventIterator events = proxy.getServerEventsAfterEventID(lastEventID, 10000);
				while (events.hasNext())
				{
					IndelibleEvent eventToFire = events.next();
					lastEventID = eventToFire.getEventID() + 1;
					proxy.fireIndelibleEvent(eventToFire);
				}
				events.close();
			} catch (Throwable e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				failed++;
			}
		}
	}
}

public class CASServerConnectionProxy implements CASServerConnectionIF
{
	private CASServerProxy parent;
	private CASServerFirehoseClient wrappedServer;
	private CASServerConnectionHandle casServerConnectionHandle;
    private IndelibleEventSupport eventSupport;
	private Thread eventListenerThread;
	private DataMoverSession moverSession;
	private SessionAuthentication sessionAuthentication, localSessionAuthentication;
	private MultipleQueueDispatcher dispatcher = new MultipleQueueDispatcher();
	private Thread collectionEventThread;
	private HashMap<CASCollectionID, ArrayList<CASCollectionConnectionProxy>>collectionConnections = new HashMap<CASCollectionID, ArrayList<CASCollectionConnectionProxy>>();
	
	public CASServerConnectionProxy(CASServerProxy parent, CASServerFirehoseClient wrappedServer) throws IOException
	{
		this.parent = parent;
		this.wrappedServer = wrappedServer;
		ComboFutureBase<CASServerConnectionHandle>openFuture = new ComboFutureBase<CASServerConnectionHandle>();
		wrappedServer.openAsync(openFuture, null);
		try
		{
			casServerConnectionHandle = openFuture.get();
			moverSession = DataMoverSource.getDataMoverSource().createDataMoverSession(EntityAuthenticationClient.getEntityAuthenticationClient().listTrustedServers()[0].getEntityID());
			localSessionAuthentication = moverSession.addAuthorizedClient(wrappedServer.getServerAuthentication());
		} catch (InterruptedException e1)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
			throw new IOException("open connection was interrupted");
		} catch (ExecutionException e1)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
			throw new IOException("Could not open connection to server cause = "+e1.getCause());
		} catch (CertificateParsingException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (CertificateEncodingException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InvalidKeyException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (UnrecoverableKeyException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IllegalStateException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchProviderException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (SignatureException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (KeyStoreException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (AuthenticationFailureException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	
	public CASServerConnectionHandle getCASServerConnectionHandle()
	{
		return casServerConnectionHandle;
	}

	@Override
	public long getLastEventID()
	{
		ComboFutureBase<Long> getLastEventIDFuture = new ComboFutureBase<Long>();
		try
		{
			wrappedServer.getLastEventIDAsync(this, getLastEventIDFuture, null);
			return getLastEventIDFuture.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public long getLastReplicatedEventID(EntityID sourceServerID, CASCollectionID collectionID)
	{
		ComboFutureBase<Long> getLastReplicatedEventIDFuture = new ComboFutureBase<Long>();
		try
		{
			wrappedServer.getLastReplicatedEventID(this, sourceServerID, collectionID, getLastReplicatedEventIDFuture, null);
			return getLastReplicatedEventIDFuture.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public IndelibleEventIterator eventsAfterID(long startingID)
	{
		ComboFutureBase<IndelibleEventIterator> future = new ComboFutureBase<IndelibleEventIterator>();
		try
		{
			wrappedServer.eventsAfterIDAsync(this, startingID, future, null);
			return future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public IndelibleEventIterator eventsAfterTime(long timestamp)
	{
		ComboFutureBase<IndelibleEventIterator> future = new ComboFutureBase<IndelibleEventIterator>();
		try
		{
			wrappedServer.eventsAfterTimeAsync(this, timestamp, future, null);
			return future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void addListener(IndelibleEventListener listener)
	{
		getEventSupport().addListener(listener);
	}

	@Override
	public void addListenerAfterID(IndelibleEventListener listener, long startingID)
	{
		getEventSupport().addListenerAfterID(listener, startingID);
	}

	@Override
	public void addListenerAfterTime(IndelibleEventListener listener, long timestamp)
	{
		getEventSupport().addListenerAfterTime(listener, timestamp);
	}

	@Override
	public void close()
	{
		try
		{
			/*
			 // TODO -implement close on collection connections !
			for (ArrayList<CASCollectionConnectionProxy>curConnectionList:collectionConnections.values())
			{
				for (CASCollectionConnectionProxy curConnection:curConnectionList)
				{
					curConnection.close();
				}
			}*/
			if (wrappedServer != null)
				wrappedServer.close();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		finally
		{
			wrappedServer = null;
		}
	}

	@Override
	public boolean isClosed()
	{
		return wrappedServer == null;
	}

	@Override
	public CASCollectionConnection openCollectionConnection(CASCollectionID id) throws CollectionNotFoundException, IOException
	{
		ComboFutureBase<CASCollectionConnectionHandle>openCollectionConnectionFuture = new ComboFutureBase<CASCollectionConnectionHandle>();
		
		wrappedServer.openCollectionConnectionAsync(this, id, openCollectionConnectionFuture, null);
		
		try
		{
			CASCollectionConnectionHandle connectionhandle = openCollectionConnectionFuture.get();
			CASCollectionConnectionProxy returnConnectionProxy = new CASCollectionConnectionProxy(parent, wrappedServer, this, id, connectionhandle);
			synchronized(collectionConnections)
			{
				ArrayList<CASCollectionConnectionProxy>connectionList = collectionConnections.get(id);
				if (connectionList == null)
				{
					connectionList = new ArrayList<CASCollectionConnectionProxy>();
					collectionConnections.put(id, connectionList);
				}
				connectionList.add(returnConnectionProxy);
			}
			return returnConnectionProxy;
		}
		catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof CollectionNotFoundException)
				throw (CollectionNotFoundException)e.getCause();
			throw new IOException("Could not open connection");
		}
		catch (Exception e)
		{
			throw new IOException("Could not open connection");
		}
	}

	@Override
	public CASCollectionConnection createNewCollection() throws IOException
	{
		ComboFutureBase<CreateNewCollectionReply>openCollectionConnectionFuture = new ComboFutureBase<CreateNewCollectionReply>();
		
		wrappedServer.createNewCollectionAsync(this, openCollectionConnectionFuture, null);
		
		try
		{
			CreateNewCollectionReply connectionReply = openCollectionConnectionFuture.get();
			CASCollectionConnectionProxy returnConnectionProxy = new CASCollectionConnectionProxy(parent, wrappedServer, this, connectionReply.getCollectionID(), connectionReply.getHandle());
			return returnConnectionProxy;
		}
		catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			throw new IOException("Could not open connection");
		}
		catch (Exception e)
		{
			throw new IOException("Could not open connection");
		}
	}

	@Override
	public EntityID getServerID() throws IOException
	{
		return parent.getServerID();
	}

	@Override
	public EntityID getSecurityServerID()
	{
		return parent.getSecurityServerID();
	}

	@Override
	public CASCollectionID[] listCollections()
	{
		ComboFutureBase<CASCollectionID []>future = new ComboFutureBase<CASCollectionID []>();
		try
		{
			wrappedServer.listCollectionsAsync(this, future, null);
			return future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public DataDescriptor retrieveMetaData(String name) throws IOException
	{
		ComboFutureBase<CASIDDataDescriptor>future = new ComboFutureBase<CASIDDataDescriptor>();
		try
		{
			wrappedServer.retrieveMetaData(this, name, future, null);
			return future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void storeMetaData(String name, DataDescriptor metaData) throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		try
		{
			NetworkDataDescriptor metaDataNDD = moverSession.registerDataDescriptor(metaData);
			wrappedServer.storeMetaData(this, name, metaDataNDD, future, null);
			future.get();
			return;
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public IndelibleVersion startTransaction() throws IOException
	{
		ComboFutureBase<IndelibleVersion>future = new ComboFutureBase<IndelibleVersion>();
		try
		{
			wrappedServer.startTransaction(this, future, null);
			return future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public IndelibleFSTransaction commit() throws IOException
	{
		ComboFutureBase<IndelibleFSTransaction>future = new ComboFutureBase<IndelibleFSTransaction>();
		try
		{
			wrappedServer.commit(this, future, null);
			return future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void rollback()
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		try
		{
			wrappedServer.rollback(this, future, null);
			future.get();
			return;
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public CASServer getServer()
	{
		return parent;
	}

	@Override
	public CASCollectionConnection addCollection(CASCollectionID curCollectionID) throws IOException
	{
		ComboFutureBase<CASCollectionConnectionHandle>future = new ComboFutureBase<CASCollectionConnectionHandle>();
		try
		{
			wrappedServer.addCollection(this, curCollectionID, future, null);
			CASCollectionConnectionHandle connectionhandle = future.get();
			CASCollectionConnectionProxy returnConnectionProxy = new CASCollectionConnectionProxy(parent, wrappedServer, this, curCollectionID, connectionhandle);
			return returnConnectionProxy;
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void addConnectedServer(EntityID serverID, EntityID securityServerID)
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		try
		{
			wrappedServer.addConnectedServer(this, serverID, securityServerID, future, null);
			future.get();
			return;
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void addClientSessionAuthentication(SessionAuthentication sessionAuthentication)
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		try
		{
            SessionAuthentication forwardedAuthentication = EntityAuthenticationClient.getEntityAuthenticationClient().
                    forwardAuthentication(wrappedServer.getServerAuthentication(), sessionAuthentication);
			wrappedServer.addClientSessionAuthentication(this, forwardedAuthentication, future, null);
			future.get();
			return;
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (CertificateParsingException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (CertificateEncodingException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InvalidKeyException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (UnrecoverableKeyException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchProviderException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchAlgorithmException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (SignatureException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (KeyStoreException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (AuthenticationFailureException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void prepareForDirectIO(CASServerConnectionIF receivingServer) throws IOException, RemoteException, AuthenticationFailureException
	{
		receivingServer.addConnectedServer(getServerID(), getSecurityServerID());
		receivingServer.addClientSessionAuthentication(getSessionAuthentication());
		addClientSessionAuthentication(receivingServer.getSessionAuthentication());

		if (receivingServer instanceof CASServerConnectionProxy)
		{
			NetworkDataDescriptor testNetworkDescriptor = getTestDescriptor();
			try
			{
				testReverseConnection((CASServerConnectionProxy) receivingServer, testNetworkDescriptor);
			}
			catch (IOException e)
			{
				boolean connected = false;
				if (e.getCause() instanceof NoMoverPathException)
				{
					EntityID securityServerID = getSecurityServerID();
					InetSocketAddress [] moverAddresses = getMoverAddresses(receivingServer, securityServerID);
					if (moverAddresses.length == 0)
					{
						throw new IOException("Could not open reverse connection");
					}
					for (InetSocketAddress tryAddress:moverAddresses)
					{
						try
						{
							setupReverseMoverConnection(receivingServer, securityServerID, tryAddress);
							testReverseMoverConnection(receivingServer, testNetworkDescriptor);
							connected = true;
							break;
						} catch (Throwable e1)
						{
							Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
						}
					}
				}
				if (!connected)
					throw e;        // We failed
			}
		}
	}

	private NetworkDataDescriptor getTestDescriptor() throws IOException
	{
		ComboFutureBase<NetworkDataDescriptor>testDescriptorFuture = new ComboFutureBase<NetworkDataDescriptor>();
		wrappedServer.getTestDescriptorAsync(this, testDescriptorFuture, null);
		try
		{
			return testDescriptorFuture.get();
		}
		catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e.getCause());
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new IOException("Could not get test descriptor");
	}
	
	private void testReverseConnection(CASServerConnectionProxy receivingServer, NetworkDataDescriptor testNetworkDescriptor) throws IOException
	{
		ComboFutureBase<Void>testReverseFuture = new ComboFutureBase<Void>();
		receivingServer.wrappedServer.testReverseConnectionAsync(this, testNetworkDescriptor, testReverseFuture, null);
		try
		{
			testReverseFuture.get();
			return;
		}
		catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e.getCause());
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new IOException("Could not get test descriptor");
	}
	
	private InetSocketAddress[] getMoverAddresses(CASServerConnectionIF receivingServer, EntityID securityServerID) throws IOException
	{
		ComboFutureBase<InetSocketAddress []>future = new ComboFutureBase<InetSocketAddress[]>();
		((CASServerConnectionProxy)receivingServer).wrappedServer.getMoverAddressesAsync(this, securityServerID, future, null);
		try
		{
			return future.get();
		}
		catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e.getCause());
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new IOException("Could not setup reverse connection");
	}
	
	private void setupReverseMoverConnection(CASServerConnectionIF receivingServer, EntityID securityServerID, InetSocketAddress tryAddress) throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		((CASServerConnectionProxy)receivingServer).wrappedServer.setupReverseMoverConnectionAsync(this, securityServerID, tryAddress.getAddress(), tryAddress.getPort(), future, null);
		try
		{
			future.get();
			return;
		}
		catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e.getCause());
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new IOException("Could not setup reverse connection");
	}
	
	private void testReverseMoverConnection(CASServerConnectionIF receivingServer, NetworkDataDescriptor testNetworkDescriptor) throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		((CASServerConnectionProxy)receivingServer).wrappedServer.testReverseConnectionAsync(this, testNetworkDescriptor, future, null);
		try
		{
			future.get();
			return;
		}
		catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e.getCause());
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new IOException("Could not setup reverse connection");
	}
	@Override
	public SessionAuthentication getSessionAuthentication()
	{
		if (sessionAuthentication == null)
		{
			ComboFutureBase<SessionAuthentication>future = new ComboFutureBase<SessionAuthentication>();
			try
			{
				wrappedServer.getSessionAuthenticationAsync(this, future, null);
				return future.get();
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			} catch (InterruptedException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			} catch (ExecutionException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
			throw new InternalError("Unexpected exception");
		}
		return sessionAuthentication;
	}

	@Override
	public IndelibleEventIterator getServerEventsAfterEventID(long eventID, int timeToWait) throws IOException
	{
		ComboFutureBase<IndelibleEventIterator>future = new ComboFutureBase<IndelibleEventIterator>();
		try
		{
			wrappedServer.getServerEventsAfterEventIDAsync(this, eventID, timeToWait, future, null);
			return future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void finalizeVersion(IndelibleVersion snapshotVersion)
	{
		throw new UnsupportedOperationException();
	}

	public void proxyFinalized(CASServerEventIteratorProxy iteratorProxy)
	{
		// TODO Auto-generated method stub
		
	}

	public void proxyFinalized(CASCollectionEventIteratorProxy iteratorProxy)
	{
		// TODO Auto-generated method stub
		
	}
	
	synchronized IndelibleEventSupport getEventSupport()
	{
		if (eventSupport == null)
		{
			eventSupport = new IndelibleEventSupport(this, dispatcher);
			eventListenerThread = new Thread(new EventListenerRunnable(this, getLastEventID() + 1), "RemoteCASCollectionConnectionProxy event listener");
			eventListenerThread.start();
		}
		return eventSupport;
	}
	
	protected void fireIndelibleEvent(IndelibleEvent fireEvent)
	{
		getEventSupport().fireIndelibleEvent(fireEvent);
	}
	
	DataMoverSession getMoverSession()
	{
		return moverSession;
	}
	
	protected MultipleQueueDispatcher getDispatcher()
	{
		return dispatcher;
	}
	
	public synchronized void startListeningForCollection(CASCollectionConnectionProxy connection)
	{
		if (collectionEventThread == null)
		{
			try
			{
				collectionEventThread = new Thread(new Runnable()
				{
					
					@Override
					public void run()
					{
						pollForCollectionEvents();
					}
				}, "CollectionEventThread "+getServerID());
				collectionEventThread.start();
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		try
		{
			wrappedServer.startListeningForCollection(connection, future, null);
			future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	
	public void pollForCollectionEvents()
	{
		while(true)
		{
			try
			{
				ComboFutureBase<CASCollectionQueuedEventMsgPack []>future = new ComboFutureBase<CASCollectionQueuedEventMsgPack[]>();
				wrappedServer.pollForCollectionEventsAsync(this, 64, 1000, future, null);
				CASCollectionQueuedEventMsgPack [] events = future.get();
				for (CASCollectionQueuedEventMsgPack curEventMsgPack:events)
				{
					CASCollectionID curCollectionID = curEventMsgPack.getCollectionID();
					CASCollectionEvent curEvent = curEventMsgPack.getEvent();
					synchronized(collectionConnections)
					{
						ArrayList<CASCollectionConnectionProxy>dispatchList = collectionConnections.get(curCollectionID);
						if (dispatchList != null)
						{
							for (CASCollectionConnectionProxy curDispatchConnection:dispatchList)
							{
								curDispatchConnection.fireIndelibleEvent(curEvent);
							}
						}
					}
				}
			}
			catch (Throwable t)
			{
				
			}
		}
	}

	@Override
	public void deleteCollection(CASCollectionID id) throws CollectionNotFoundException, IOException
	{
		// TODO Auto-generated method stub
		
	}
}
