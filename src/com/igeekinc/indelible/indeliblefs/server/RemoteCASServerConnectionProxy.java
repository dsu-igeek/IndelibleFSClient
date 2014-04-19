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
 
package com.igeekinc.indelible.indeliblefs.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverReceiver;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSource;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.NoMoverPathException;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession.DataDescriptorAvailability;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventListener;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventSupport;
import com.igeekinc.indelible.indeliblefs.events.RemoteIndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.events.RemoteIndelibleIteratorProxy;
import com.igeekinc.indelible.indeliblefs.proxies.RemoteCASServerProxy;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.indeliblefs.uniblock.remote.RemoteCASServerConnection;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.DebugLogMessage;
import com.igeekinc.util.logging.ErrorLogMessage;

class CASServerEventListenerRunnable implements Runnable
{
	RemoteCASServerConnectionProxy proxy;
	long lastEventID;
	public CASServerEventListenerRunnable(RemoteCASServerConnectionProxy proxy, long startingEventID)
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
				IndelibleEventIterator events = proxy.getServerEventsAfterEventID(lastEventID, 30000);
				while (events.hasNext())
				{
					IndelibleEvent eventToFire = events.next();
					lastEventID = eventToFire.getEventID() + 1;
					proxy.fireIndelibleEvent(eventToFire);
				}
			} catch (Throwable e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				failed++;
			}
		}
	}
}

public class RemoteCASServerConnectionProxy implements CASServerConnectionIF
{
	RemoteCASServerConnection wrappedConnection;
	RemoteCASServerProxy remoteServer;
	DataMoverSession moverSession;
	IndelibleEventSupport eventSupport;
	Thread eventListenerThread;
	Logger logger = Logger.getLogger(getClass());
	
	public RemoteCASServerConnectionProxy(RemoteCASServerConnection wrappedConnection, RemoteCASServerProxy remoteServer, InetAddress preferredServerAddress)
	{
		this.wrappedConnection = wrappedConnection;
		this.remoteServer = remoteServer;
        try
		{
        	/*
        	 * Authorize the server to read data from us
        	 */
			moverSession = DataMoverSource.getDataMoverSource().createDataMoverSession(wrappedConnection.getSecurityServerID());
			SessionAuthentication ourSessionAuthentication = moverSession.addAuthorizedClient(wrappedConnection.getServerEntityAuthentication());
			wrappedConnection.addClientSessionAuthentication(ourSessionAuthentication);
			
			/*
			 * Now, get the authentication that allows us to read data from the server
			 */
			SessionAuthentication remoteAuthentication = wrappedConnection.getSessionAuthentication();
	        EntityID securityServerID = wrappedConnection.getSecurityServerID();
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
			// See if we can get a local reverse connection
			NetworkDataDescriptor testNetworkDescriptor = moverSession.registerDataDescriptor(testDescriptor, DataDescriptorAvailability.kLocalOnlyAccess);
			try
			{
				logger.error(new ErrorLogMessage("Attempting to establish local connection"));
				wrappedConnection.testReverseConnection(testNetworkDescriptor);
				logger.error(new ErrorLogMessage("Local connection succeeded"));
			}
			catch (NoMoverPathException e)
			{
				// OK, just go ahead and talk directly to the remote mover
				InetSocketAddress [] moverAddresses = wrappedConnection.getMoverAddresses(securityServerID);
				if (moverAddresses.length == 0)
					throw e;
				try
				{
					logger.error(new ErrorLogMessage("Attempting to establish reverse connection"));
					DataMoverSource.getDataMoverSource().openReverseConnection(securityServerID, preferredServerAddress, moverAddresses[0].getPort());
					wrappedConnection.testReverseConnection(testNetworkDescriptor);
					logger.error(new ErrorLogMessage("Reverse connection succeeded"));
				} catch (Throwable t)
				{
					logger.error(new ErrorLogMessage("Reverse connection failed, trying forward connection"));
					testNetworkDescriptor = moverSession.registerDataDescriptor(testDescriptor, DataDescriptorAvailability.kAllAccess);
					wrappedConnection.testReverseConnection(testNetworkDescriptor);
					logger.error(new ErrorLogMessage("Mover connection succeeded"));
				}
			}
		} catch (Throwable e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}
	@Override
	public void close()
	{
		try
		{
			wrappedConnection.close();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}

	@Override
	public boolean isClosed()
	{
		try
		{
			return wrappedConnection.isClosed();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}
	
	@Override
	public CASCollectionConnection getCollectionConnection(CASCollectionID id)
			throws CollectionNotFoundException
	{
		try
		{
			return new RemoteCASCollectionConnectionProxy(wrappedConnection.getCollection(id));
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new CollectionNotFoundException("Caught remote exception");
		}
	}

	@Override
	public CASCollectionConnection createNewCollection() throws IOException
	{
		try
		{
			return new RemoteCASCollectionConnectionProxy(wrappedConnection.createNewCollection());
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public EntityID getServerID()
	{
		try
		{
			return wrappedConnection.getServerID();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}
	
	@Override
	public SessionAuthentication getSessionAuthentication()
	{
		try
		{
			return wrappedConnection.getSessionAuthentication();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}
	
	@Override
	public void addClientSessionAuthentication(SessionAuthentication sessionAuthentication)
	{
		try
		{
			SessionAuthentication forwardedAuthentication = EntityAuthenticationClient.getEntityAuthenticationClient().
					forwardAuthentication(wrappedConnection.getServerEntityAuthentication(), sessionAuthentication);
			wrappedConnection.addClientSessionAuthentication(forwardedAuthentication);
		} catch (Throwable e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}
	
	@Override
	public CASCollectionID[] listCollections()
	{
		try
		{
			return wrappedConnection.listCollections();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public DataDescriptor retrieveMetaData(String name) throws IOException
	{
		try
		{
			return wrappedConnection.retrieveMetaData(name);
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public void storeMetaData(String name, DataDescriptor metaData)
			throws IOException
	{
		NetworkDataDescriptor networkDescriptor = moverSession.registerDataDescriptor(metaData);
		wrappedConnection.storeMetaData(name, networkDescriptor);
	}

	@Override
	public IndelibleVersion startTransaction() throws IOException
	{
		try
		{
			return wrappedConnection.startTransaction();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public IndelibleFSTransaction commit() throws IOException
	{
		try
		{
			return wrappedConnection.commit();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public void rollback()
	{
		try
		{
			wrappedConnection.rollback();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public CASServer getServer()
	{
		return remoteServer;
	}
	@Override
	public CASCollectionConnection addCollection(CASCollectionID curCollectionID) throws IOException
	{
		try
		{
			return new RemoteCASCollectionConnectionProxy(wrappedConnection.addCollection(curCollectionID));
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}
	@Override
	public void addConnectedServer(EntityID serverID, EntityID securityServerID)
	{
		try
		{
			wrappedConnection.addConnectedServer(serverID, securityServerID);
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}
	@Override
	public EntityID getSecurityServerID()
	{
		try
		{
			return wrappedConnection.getSecurityServerID();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public IndelibleEventIterator getServerEventsAfterEventID(long eventID, int timeToWait) throws IOException
	{
		return new RemoteIndelibleIteratorProxy(wrappedConnection.getServerEventsAfterEventID(eventID, timeToWait));
	}

	synchronized IndelibleEventSupport getEventSupport()
	{
		if (eventSupport == null)
		{
			eventSupport = new IndelibleEventSupport(this);
			try
			{
				eventListenerThread = new Thread(new CASServerEventListenerRunnable(this, wrappedConnection.getLastEventID() + 1), "RemoteCASServerConnectionProxy event listener");
			} catch (RemoteException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new InternalError("Could not initialize event support - server had an exception");
			}
			eventListenerThread.start();
		}
		return eventSupport;
	}
	
	
	protected void fireIndelibleEvent(IndelibleEvent fireEvent)
	{
		getEventSupport().fireIndelibleEvent(fireEvent);
	}

	@Override
	public long getLastReplicatedEventID(EntityID sourceServerID, CASCollectionID collectionID)
	{
		try
		{
			return wrappedConnection.getLastReplicatedEventID(sourceServerID, collectionID);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			return -1L;
		}
	}
	
	@Override
	public long getLastEventID()
	{
		try
		{
			return wrappedConnection.getLastEventID();
		} catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		return -1;
	}
	
	@Override
	public IndelibleEventIterator eventsAfterID(long startingID)
	{
		try
		{
			RemoteIndelibleEventIterator remoteIterator = wrappedConnection.eventsAfterID(startingID);
			return new RemoteIndelibleIteratorProxy(remoteIterator);
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public IndelibleEventIterator eventsAfterTime(long timestamp)
	{
		try
		{
			RemoteIndelibleEventIterator remoteIterator = wrappedConnection.eventsAfterTime(timestamp);
			return new RemoteIndelibleIteratorProxy(remoteIterator);
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public void addListener(IndelibleEventListener listener)
	{
		getEventSupport().addListener(listener);
	}
	
	@Override
	public void addListenerAfterID(IndelibleEventListener listener,
			long startingID)
	{
		getEventSupport().addListenerAfterID(listener, startingID);
	}

	@Override
	public void addListenerAfterTime(IndelibleEventListener listener,
			long timestamp)
	{
		getEventSupport().addListenerAfterTime(listener, timestamp);
	}
	@Override
	public void finalizeVersion(IndelibleVersion snapshotVersion)
	{
		throw new UnsupportedOperationException();	// TODO - remove this from the public interface entirely
	}
	
	@Override
	public void prepareForDirectIO(CASServerConnectionIF receivingServer) throws IOException, RemoteException, AuthenticationFailureException
	{
		receivingServer.addConnectedServer(getServerID(), getSecurityServerID());
		receivingServer.addClientSessionAuthentication(getSessionAuthentication());
		addClientSessionAuthentication(receivingServer.getSessionAuthentication());
		
		if (receivingServer instanceof RemoteCASServerConnectionProxy)
		{
			
			NetworkDataDescriptor testNetworkDescriptor = wrappedConnection.getTestDescriptor();
			try
			{
				((RemoteCASServerConnectionProxy)receivingServer).wrappedConnection.testReverseConnection(testNetworkDescriptor);
			}
			catch (NoMoverPathException e)
			{
				EntityID securityServerID = getSecurityServerID();
				InetSocketAddress [] moverAddresses = ((RemoteCASServerConnectionProxy)receivingServer).wrappedConnection.getMoverAddresses(securityServerID);
				if (moverAddresses.length == 0)
					throw e;
				boolean connected = false;
				for (InetSocketAddress tryAddress:moverAddresses)
				{
					try
					{
						wrappedConnection.setupReverseMoverConnection(securityServerID, tryAddress.getAddress(), tryAddress.getPort());
						((RemoteCASServerConnectionProxy)receivingServer).wrappedConnection.testReverseConnection(testNetworkDescriptor);
						connected = true;
						break;
					} catch (Throwable e1)
					{
						Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
					}
				}
				if (!connected)
					throw e;	// We failed
			}
		}
		
	}
}
