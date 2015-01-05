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
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSource;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventListener;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventSupport;
import com.igeekinc.indelible.indeliblefs.events.RemoteIndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.events.RemoteIndelibleIteratorProxy;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.indeliblefs.uniblock.CASSegmentIDIterator;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASStoreInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.DataVersionInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.SegmentInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentExists;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentNotFound;
import com.igeekinc.indelible.indeliblefs.uniblock.remote.AsyncRetrieveSegmentByCASIdentifierCommandBlock;
import com.igeekinc.indelible.indeliblefs.uniblock.remote.AsyncStoreReplicatedSegmentCommandBlock;
import com.igeekinc.indelible.indeliblefs.uniblock.remote.AsyncStoreSegmentCommandBlock;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.CASSegmentID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ClientRemoteAsyncManager;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.async.RemoteAsyncCommandBlock;
import com.igeekinc.util.async.RemoteAsyncCompletionStatus;
import com.igeekinc.util.logging.ErrorLogMessage;

class EventListenerRunnable implements Runnable
{
	RemoteCASCollectionConnectionProxy proxy;
	long lastEventID;
	public EventListenerRunnable(RemoteCASCollectionConnectionProxy proxy, long startingEventID)
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
				IndelibleEventIterator events = proxy.getTransactionEventsAfterEventID(lastEventID, 30000);
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

class RemoteCASCollectionConnectionProxyFuture extends ComboFutureBase<Void>
{
	public RemoteCASCollectionConnectionProxyFuture()
	{
		
	}
	
	public <A>RemoteCASCollectionConnectionProxyFuture(AsyncCompletion<Void, ? super A> completionHandler, A attachment)
	{
		super(completionHandler, attachment);
	}
}

class RemoteStoreSegmentFuture extends ComboFutureBase<CASStoreInfo>
{
	public RemoteStoreSegmentFuture()
	{
		
	}
	
	public <A>RemoteStoreSegmentFuture(AsyncCompletion<CASStoreInfo, ? super A> completionHandler, A attachment)
	{
		super(completionHandler, attachment);
	}
}

class RemoteRetrieveSegmentFuture extends ComboFutureBase<CASIDDataDescriptor>
{
	public RemoteRetrieveSegmentFuture()
	{
		
	}
	
	public <A>RemoteRetrieveSegmentFuture(AsyncCompletion<CASIDDataDescriptor, ? super A> completionHandler, A attachment)
	{
		super(completionHandler, attachment);
	}
}
public class RemoteCASCollectionConnectionProxy extends ClientRemoteAsyncManager implements
		CASCollectionConnection
{
	RemoteCASCollectionConnection remoteConnection;
	private IndelibleEventSupport eventSupport;
	DataMoverSession moverSession;
	private Thread eventListenerThread;
	SessionAuthentication sessionAuthentication;
	
	public RemoteCASCollectionConnectionProxy(RemoteCASCollectionConnection remoteConnection)
	{
		this.remoteConnection = remoteConnection;
		eventSupport = null;
        try
		{
			moverSession = DataMoverSource.getDataMoverSource().createDataMoverSession(EntityAuthenticationClient.getEntityAuthenticationClient().listTrustedServers()[0].getEntityID());
			sessionAuthentication = moverSession.addAuthorizedClient(remoteConnection.getEntityAuthentication());
		} catch (Throwable e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}
	
	@Override
	public long getLastEventID()
	{
		try
		{
			return remoteConnection.getLastEventID();
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
			RemoteIndelibleEventIterator remoteIterator = remoteConnection.eventsAfterID(startingID);
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
			RemoteIndelibleEventIterator remoteIterator = remoteConnection.eventsAfterTime(timestamp);
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
	public CASCollectionID getCollectionID()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASIDDataDescriptor retrieveSegment(CASIdentifier segmentID)
			throws IOException, SegmentNotFound
	{
		return remoteConnection.retrieveSegment(segmentID);
	}

	@Override
	public DataVersionInfo retrieveSegment(ObjectID segmentID)
			throws IOException
	{
		return remoteConnection.retrieveSegment(segmentID);
	}

	@Override
	public DataVersionInfo retrieveSegment(ObjectID segmentID,
			IndelibleVersion version, RetrieveVersionFlags flags)
			throws IOException
	{
		return remoteConnection.retrieveSegment(segmentID, version, flags);
	}

	@Override
	public SegmentInfo retrieveSegmentInfo(ObjectID segmentID,
			IndelibleVersion version, RetrieveVersionFlags flags)
			throws IOException
	{
		return remoteConnection.retrieveSegmentInfo(segmentID, version, flags);
	}

	@Override
	public boolean verifySegment(ObjectID segmentID, IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException
	{
		return remoteConnection.verifySegment(segmentID, version, flags);
	}

	@Override
	public void repairSegment(ObjectID checkSegmentID,
			IndelibleVersion transactionVersion, DataVersionInfo masterData)
			throws IOException
	{
		remoteConnection.repairSegment(checkSegmentID, transactionVersion, masterData);
	}

	@Override
	public CASStoreInfo storeSegment(CASIDDataDescriptor segmentDescriptor) throws IOException
	{
		if (segmentDescriptor == null)
			throw new IllegalArgumentException("segmentDescriptor cannot be null");
		NetworkDataDescriptor networkDescriptor = moverSession.registerDataDescriptor(segmentDescriptor);
		return remoteConnection.storeSegment(networkDescriptor);
	}

	@Override
	public void storeVersionedSegment(ObjectID id, CASIDDataDescriptor segmentDescriptor)
			throws IOException, SegmentExists
	{
		if (segmentDescriptor == null)
			throw new IllegalArgumentException("segmentDescriptor cannot be null");
		NetworkDataDescriptor networkDescriptor = moverSession.registerDataDescriptor(segmentDescriptor);
		remoteConnection.storeVersionedSegment(id, networkDescriptor);
	}

	
	@Override
	public void storeReplicatedSegment(ObjectID replicateSegmentID, IndelibleVersion version,
			CASIDDataDescriptor sourceDescriptor, CASCollectionEvent curCASEvent)
			throws IOException
	{
		if (replicateSegmentID == null)
			throw new IllegalArgumentException("replicateSegmentID cannot be null");
		if (sourceDescriptor == null)
			throw new IllegalArgumentException("sourceDescriptor cannot be null");
		if (curCASEvent == null)
			throw new IllegalArgumentException("curCASEvent cannot be null");
		NetworkDataDescriptor networkDescriptor = moverSession.registerDataDescriptor(sourceDescriptor);
		remoteConnection.storeReplicatedSegment(replicateSegmentID, version, networkDescriptor, curCASEvent);
	}

	@Override
	public boolean releaseSegment(ObjectID releaseID) throws IOException
	{
		return remoteConnection.releaseSegment(releaseID);
	}

	@Override
	public boolean releaseVersionedSegment(ObjectID id, IndelibleVersion version)
			throws IOException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean[] bulkReleaseSegment(CASSegmentID[] releaseIDs)
			throws IOException
	{
		return remoteConnection.bulkReleaseSegment(releaseIDs);
	}

	@Override
	public CASServer getCASServer()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASIdentifier retrieveCASIdentifier(CASSegmentID casSegmentID)
			throws IOException
	{
		return remoteConnection.retrieveCASIdentifier(casSegmentID);
	}

	@Override
	public Map<String, Serializable> getMetaDataResource(
			String mdResourceName) throws RemoteException,
			PermissionDeniedException, IOException
	{
		return remoteConnection.getMetaDataResource(mdResourceName);
	}

	@Override
	public void setMetaDataResource(String mdResourceName,
			Map<String, Serializable> resource) throws RemoteException,
			PermissionDeniedException, IOException
	{
		remoteConnection.setMetaDataResource(mdResourceName, resource);
	}

	
	@Override
	public void removeMetaDataResouce(String mdResourceName) throws PermissionDeniedException, IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public IndelibleEventIterator getEventsForTransaction(IndelibleFSTransaction transaction) throws IOException
	{
		return new RemoteIndelibleIteratorProxy(remoteConnection.getEventsForTransaction(transaction));
	}

	@Override
	public IndelibleEventIterator getTransactionEventsAfterEventID(long eventID, int timeToWait) throws IOException
	{
		return new RemoteIndelibleIteratorProxy(remoteConnection.getTransactionEventsAfterEventID(eventID, timeToWait));
	}

	synchronized IndelibleEventSupport getEventSupport()
	{
		if (eventSupport == null)
		{
			eventSupport = new IndelibleEventSupport(this, null);
			try
			{
				eventListenerThread = new Thread(new EventListenerRunnable(this, remoteConnection.getLastEventID() + 1), "RemoteCASCollectionConnectionProxy event listener");
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
			return remoteConnection.getLastReplicatedEventID(sourceServerID, collectionID);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			return -1L;
		}
	}

	@Override
	public String[] listMetaDataNames() throws PermissionDeniedException,
			IOException
	{
		return remoteConnection.listMetaDataNames();
	}

	@Override
	public void startTransaction() throws IOException
	{
		remoteConnection.startTransaction();
	}

	@Override
	public IndelibleFSTransaction commit() throws IOException
	{
		// TODO - the current implementation assumes that async commands and commits are being serialized by the caller
		// Eventually, this should queue a commit to the server
		waitForQueueDrain();
		return remoteConnection.commit();
	}

	@Override
	public void rollback() throws IOException
	{
		remoteConnection.rollback();
	}

	@Override
	public void startReplicatedTransaction(TransactionCommittedEvent transactionEvent)
			throws IOException
	{
		remoteConnection.startReplicatedTransaction(transactionEvent);
	}

	public SessionAuthentication getSessionAuthentication()
	{
		return sessionAuthentication;
	}

	@Override
	public CASIDDataDescriptor getMetaDataForReplication() throws IOException
	{
		return remoteConnection.getMetaDataForReplication();
	}

	@Override
	public void replicateMetaDataResource(CASIDDataDescriptor replicateMetaData,
			CASCollectionEvent curCASEvent) throws IOException
	{
		remoteConnection.replicateMetaDataResource(replicateMetaData, curCASEvent);
	}

	@Override
	public CASSegmentIDIterator listSegments() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndelibleVersionIterator listVersionsForSegment(ObjectID id)
			throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndelibleVersionIterator listVersionsForSegmentInRange(ObjectID id,
			IndelibleVersion first, IndelibleVersion last) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<Void> storeReplicatedSegmentAsync(
			ObjectID replicateSegmentID, IndelibleVersion replicateVersion,
			CASIDDataDescriptor sourceDescriptor, CASCollectionEvent curCASEvent)
			throws IOException
	{
		RemoteCASCollectionConnectionProxyFuture future = new RemoteCASCollectionConnectionProxyFuture();
		storeReplicatedSegmentAsyncCommon(replicateSegmentID, replicateVersion, sourceDescriptor, curCASEvent, future);
		return future;
	}

	@Override
	public <A> void storeReplicatedSegmentAsync(ObjectID replicateSegmentID,
			IndelibleVersion replicateVersion,
			CASIDDataDescriptor sourceDescriptor,
			CASCollectionEvent curCASEvent,
			AsyncCompletion<Void, A> completionHandler, A attachment)
			throws IOException
	{
		RemoteCASCollectionConnectionProxyFuture future = new RemoteCASCollectionConnectionProxyFuture(completionHandler, attachment);
		storeReplicatedSegmentAsyncCommon(replicateSegmentID, replicateVersion, sourceDescriptor, curCASEvent, future);
	}

	private void storeReplicatedSegmentAsyncCommon(ObjectID replicateSegmentID,
			IndelibleVersion replicateVersion,
			CASIDDataDescriptor sourceDescriptor,
			CASCollectionEvent curCASEvent, RemoteCASCollectionConnectionProxyFuture future)
	{
		AsyncStoreReplicatedSegmentCommandBlock commandBlock = new AsyncStoreReplicatedSegmentCommandBlock(replicateSegmentID, replicateVersion, sourceDescriptor, curCASEvent);
		queueCommand(commandBlock, future, null);
	}
	@Override
	protected RemoteAsyncCompletionStatus[] executeAsync(
			RemoteAsyncCommandBlock[] commandsToExecute,
			long timeToWaitForNewCompletionsInMS) throws RemoteException
	{
		return remoteConnection.executeAsync(commandsToExecute, timeToWaitForNewCompletionsInMS);
	}

	@Override
	public Future<CASStoreInfo> storeSegmentAsync(CASIDDataDescriptor sourceDescriptor) throws IOException
	{
		RemoteStoreSegmentFuture future = new RemoteStoreSegmentFuture();
		storeSegmentAsyncCommon(sourceDescriptor, future);
		return future;
	}

	@Override
	public <A> void storeSegmentAsync(CASIDDataDescriptor sourceDescriptor, AsyncCompletion<CASStoreInfo, A> completionHandler,
			A attachment) throws IOException
	{
		RemoteStoreSegmentFuture future = new RemoteStoreSegmentFuture(completionHandler, attachment);
		storeSegmentAsyncCommon(sourceDescriptor, future);
	}

	private void storeSegmentAsyncCommon(CASIDDataDescriptor sourceDescriptor, RemoteStoreSegmentFuture future)
	{
		AsyncStoreSegmentCommandBlock commandBlock = new AsyncStoreSegmentCommandBlock(sourceDescriptor);
		queueCommand(commandBlock, future, null);
	}
	
	@Override
	public Future<CASIDDataDescriptor> retrieveSegmentAsync(CASIdentifier segmentID) throws IOException, SegmentNotFound
	{
		RemoteRetrieveSegmentFuture future = new RemoteRetrieveSegmentFuture();
		retrieveSegmentAsyncCommon(segmentID, future);
		return future;
	}

	@Override
	public <A> void retrieveSegmentAsync(CASIdentifier segmentID,
			AsyncCompletion<CASIDDataDescriptor, A> completionHandler,
			A attachment) throws IOException, SegmentNotFound
	{
		RemoteRetrieveSegmentFuture future = new RemoteRetrieveSegmentFuture(completionHandler, attachment);
		retrieveSegmentAsyncCommon(segmentID, future);
	}

	public void retrieveSegmentAsyncCommon(CASIdentifier segmentID, RemoteRetrieveSegmentFuture future)
	{
		AsyncRetrieveSegmentByCASIdentifierCommandBlock commandBlock = new AsyncRetrieveSegmentByCASIdentifierCommandBlock(segmentID);
		queueCommand(commandBlock, future, null);
	}
	@Override
	public Future<DataVersionInfo> retrieveSegmentAsync(ObjectID segmentID)
			throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A> void retrieveSegmentAsync(ObjectID segmentID,
			AsyncCompletion<DataVersionInfo, A> completionHandler, A attachment)
			throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Future<DataVersionInfo> retrieveSegmentAsync(ObjectID segmentID,
			IndelibleVersion version, RetrieveVersionFlags flags)
			throws IOException, SegmentNotFound
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A> void retrieveSegmentAsync(ObjectID segmentID,
			IndelibleVersion version, RetrieveVersionFlags flags,
			AsyncCompletion<DataVersionInfo, A> completionHandler, A attachment)
			throws IOException, SegmentNotFound
	{
		// TODO Auto-generated method stub
		
	}
}
