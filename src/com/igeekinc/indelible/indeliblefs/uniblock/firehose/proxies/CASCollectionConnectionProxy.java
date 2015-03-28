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
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventListener;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventSupport;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
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
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentNotFound;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASCollectionConnectionHandle;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.CASSegmentID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

/*
class CCCPEventListenerRunnable implements Runnable
{
	CASCollectionConnectionProxy proxy;
	long lastEventID;
	public CCCPEventListenerRunnable(CASCollectionConnectionProxy proxy, long startingEventID)
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
				Thread.currentThread().setName("CCCPEventListenerRunnable "+proxy.getCollectionID()+" lastEventID = "+lastEventID);
				IndelibleEventIterator events = proxy.getTransactionEventsAfterEventID(lastEventID, 1000);
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
*/
public class CASCollectionConnectionProxy implements CASCollectionConnection
{
	private CASServerProxy parent;
	private CASServerFirehoseClient wrappedServer;
	private CASServerConnectionProxy serverConnection;
	private CASCollectionConnectionHandle handle;
	private CASCollectionID collectionID;
	private IndelibleEventSupport eventSupport;
	private Thread eventListenerThread;
	
	public CASCollectionConnectionProxy(CASServerProxy parent, CASServerFirehoseClient wrappedServer, CASServerConnectionProxy serverConnection, CASCollectionID collectionID, CASCollectionConnectionHandle handle)
	{
		this.parent = parent;
		this.wrappedServer = wrappedServer;
		this.collectionID = collectionID;
		this.serverConnection = serverConnection;
		this.handle = handle;
	}

	public CASServerConnectionProxy getServerConnection()
	{
		return serverConnection;
	}
	
	public CASCollectionConnectionHandle getCASCollectionConnectionHandle()
	{
		return handle;
	}

	synchronized IndelibleEventSupport getEventSupport()
	{
		if (eventSupport == null)
		{
			eventSupport = new IndelibleEventSupport(this, serverConnection.getDispatcher());
			serverConnection.startListeningForCollection(this);
		}
		return eventSupport;
	}
	
	protected void fireIndelibleEvent(IndelibleEvent fireEvent)
	{
		getEventSupport().fireIndelibleEvent(fireEvent);
	}
	
	@Override
	public long getLastEventID()
	{
		ComboFutureBase<Long>future = new ComboFutureBase<Long>();
		try
		{
			wrappedServer.getCollectionLastEventIDAsync(this, future, null);
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
		return 0;
	}

	@Override
	public long getLastReplicatedEventID(EntityID sourceServerID, CASCollectionID collectionID)
	{
		ComboFutureBase<Long>future = new ComboFutureBase<Long>();
		try
		{
			wrappedServer.getCollectionLastReplicatedEventID(this, sourceServerID, collectionID, future, null);
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
		return 0;
	}

	@Override
	public IndelibleEventIterator eventsAfterID(long startingID)
	{
		ComboFutureBase<IndelibleEventIterator>future = new ComboFutureBase<IndelibleEventIterator>();
		try
		{
			wrappedServer.collectionEventsAfterIDAsync(this, startingID, future, null);
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
		ComboFutureBase<IndelibleEventIterator>future = new ComboFutureBase<IndelibleEventIterator>();
		try
		{
			wrappedServer.collectionEventsAfterTimeAsync(this, timestamp, future, null);
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
	public CASCollectionID getCollectionID()
	{
		return collectionID;
	}

	@Override
	public CASIDDataDescriptor retrieveSegment(CASIdentifier segmentID) throws IOException, SegmentNotFound
	{
		Future<CASIDDataDescriptor>future = retrieveSegmentAsync(segmentID);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof SegmentNotFound)
				throw (SegmentNotFound)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public Future<CASIDDataDescriptor> retrieveSegmentAsync(CASIdentifier segmentID) throws IOException, SegmentNotFound
	{
		ComboFutureBase<CASIDDataDescriptor>future = new ComboFutureBase<CASIDDataDescriptor>();
		retrieveSegmentAsync(segmentID, future, null);
		return future;
	}

	@Override
	public <A> void retrieveSegmentAsync(CASIdentifier segmentID, AsyncCompletion<CASIDDataDescriptor, A> completionHandler, A attachment)
			throws IOException, SegmentNotFound
	{
		wrappedServer.retrieveSegmentByCASIdentifierASync(this, segmentID, completionHandler, attachment);
	}

	@Override
	public DataVersionInfo retrieveSegment(ObjectID segmentID) throws IOException, SegmentNotFound
	{
		Future<DataVersionInfo>future = retrieveSegmentAsync(segmentID);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof SegmentNotFound)
				throw (SegmentNotFound)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public Future<DataVersionInfo> retrieveSegmentAsync(ObjectID segmentID) throws IOException
	{
		ComboFutureBase<DataVersionInfo>future = new ComboFutureBase<DataVersionInfo>();
		retrieveSegmentAsync(segmentID, future, null);
		return future;
	}

	@Override
	public <A> void retrieveSegmentAsync(ObjectID segmentID, AsyncCompletion<DataVersionInfo, A> completionHandler, A attachment) throws IOException
	{
		wrappedServer.retrieveSegmentByObjectID(this, segmentID, completionHandler, attachment);
	}

	@Override
	public DataVersionInfo retrieveSegment(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException, SegmentNotFound
	{
		Future<DataVersionInfo>future = retrieveSegmentAsync(segmentID, version, flags);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof SegmentNotFound)
				throw (SegmentNotFound)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public Future<DataVersionInfo> retrieveSegmentAsync(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException
	{
		ComboFutureBase<DataVersionInfo>future = new ComboFutureBase<DataVersionInfo>();
		retrieveSegmentAsync(segmentID, version, flags, future, null);
		return future;
	}

	@Override
	public <A> void retrieveSegmentAsync(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags,
			AsyncCompletion<DataVersionInfo, A> completionHandler, A attachment) throws IOException
	{
		wrappedServer.retrieveSegmentByObjectIDAndVersion(this, segmentID, version, flags, completionHandler, attachment);
	}

	@Override
	public SegmentInfo retrieveSegmentInfo(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException
	{
		ComboFutureBase<SegmentInfo>future = new ComboFutureBase<SegmentInfo>();
		try
		{
			retrieveSegmentInfoAsync(segmentID, version, flags, future, null);
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	public <A>void retrieveSegmentInfoAsync(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags,
			AsyncCompletion<SegmentInfo, A>completionHandler, A attachment) throws IOException
	{
		wrappedServer.retrieveSegmentInfo(this, segmentID, version, flags, completionHandler, attachment);
	}
	
	@Override
	public boolean verifySegment(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException
	{
		ComboFutureBase<Boolean>future = new ComboFutureBase<Boolean>();
		try
		{
			verifySegmentAsync(segmentID, version, flags, future, null);
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	public <A>void verifySegmentAsync(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags,
			AsyncCompletion<Boolean, A>completionHandler, A attachment) throws IOException
	{
		wrappedServer.verifySegmentInfo(this, segmentID, version, flags, completionHandler, attachment);
	}

	
	@Override
	public CASStoreInfo storeSegment(CASIDDataDescriptor segmentDescriptor) throws IOException
	{
		Future<CASStoreInfo>future = storeSegmentAsync(segmentDescriptor);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public Future<CASStoreInfo> storeSegmentAsync(CASIDDataDescriptor sourceDescriptor) throws IOException
	{
		ComboFutureBase<CASStoreInfo>future = new ComboFutureBase<CASStoreInfo>();
		storeSegmentAsync(sourceDescriptor, future, null);
		return future;
	}

	@Override
	public <A> void storeSegmentAsync(CASIDDataDescriptor sourceDescriptor, AsyncCompletion<CASStoreInfo, A> completionHandler, A attachment)
			throws IOException
	{			
		NetworkDataDescriptor sourceDescriptorNDD = serverConnection.getMoverSession().registerDataDescriptor(sourceDescriptor);
		wrappedServer.storeSegmentAsync(this, sourceDescriptorNDD, completionHandler, attachment);
	}

	@Override
	public void storeVersionedSegment(ObjectID id, CASIDDataDescriptor segmentDescriptor) throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		storeVersionedSegmentAsync(id, segmentDescriptor, future, null);
		try
		{
			future.get();
			return;
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}
	
	public <A>void storeVersionedSegmentAsync(ObjectID id, CASIDDataDescriptor segmentDescriptor, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		NetworkDataDescriptor segmentDescriptorNDD = serverConnection.getMoverSession().registerDataDescriptor(segmentDescriptor);
		wrappedServer.storeVersionedSegmentAsync(this, id, segmentDescriptorNDD, completionHandler, attachment);
	}

	@Override
	public boolean releaseSegment(ObjectID releaseID) throws IOException
	{
		ComboFutureBase<Boolean>future = new ComboFutureBase<Boolean>();
		wrappedServer.releaseSegmentAsync(this, releaseID, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public boolean[] bulkReleaseSegment(CASSegmentID[] releaseIDs) throws IOException
	{
		ComboFutureBase<boolean []>future = new ComboFutureBase<boolean[]>();
		wrappedServer.bulkReleaseSegmentAsync(this, releaseIDs, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public boolean releaseVersionedSegment(ObjectID id, IndelibleVersion version) throws IOException
	{
		ComboFutureBase<Boolean>future = new ComboFutureBase<Boolean>();
		wrappedServer.releaseVersionedSegmentAsync(this, id, version, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public IndelibleVersionIterator listVersionsForSegment(ObjectID id) throws IOException
	{
		ComboFutureBase<IndelibleVersionIterator>future = new ComboFutureBase<IndelibleVersionIterator>();
		try
		{
			wrappedServer.listVersionForSegmentAsync(this, id, future, null);
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
	public IndelibleVersionIterator listVersionsForSegmentInRange(ObjectID id, IndelibleVersion first, IndelibleVersion last) throws IOException
	{
		ComboFutureBase<IndelibleVersionIterator>future = new ComboFutureBase<IndelibleVersionIterator>();
		try
		{
			wrappedServer.listVersionForSegmentInRangeAsync(this, id, first, last, future, null);
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
	public String[] listMetaDataNames() throws PermissionDeniedException, IOException
	{
		ComboFutureBase<String[]>future = new ComboFutureBase<String[]>();
		wrappedServer.listMetaDataNamesAsync(this, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public Map<String, Serializable> getMetaDataResource(String mdResourceName) throws PermissionDeniedException, IOException
	{
		ComboFutureBase<Map<String, Serializable>>future = new ComboFutureBase<Map<String, Serializable>>();
		wrappedServer.getMetaDataResource(this, mdResourceName, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			if (e.getCause() instanceof PermissionDeniedException)
				throw (PermissionDeniedException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void setMetaDataResource(String mdResourceName, Map<String, Serializable> resource) throws PermissionDeniedException, IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeMetaDataResouce(String mdResourceName) throws PermissionDeniedException, IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public CASServer getCASServer()
	{
		return parent;
	}

	@Override
	public CASIdentifier retrieveCASIdentifier(CASSegmentID casSegmentID) throws IOException
	{
		ComboFutureBase<CASIdentifier>future = new ComboFutureBase<CASIdentifier>();
		wrappedServer.retrieveCASIdentifierAsync(this, casSegmentID, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public IndelibleEventIterator getEventsForTransaction(IndelibleFSTransaction transaction) throws IOException
	{
		ComboFutureBase<IndelibleEventIterator>future = new ComboFutureBase<IndelibleEventIterator>();
		try
		{
			wrappedServer.getEventsForTransactionAsync(this, transaction, future, null);
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
			{
				throw (IOException)e.getCause();
			}
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public IndelibleEventIterator getTransactionEventsAfterEventID(long eventID, int timeToWait) throws IOException
	{
		ComboFutureBase<IndelibleEventIterator>future = new ComboFutureBase<IndelibleEventIterator>();
		try
		{
			wrappedServer.getTransactionEventsAfterEventID(this, eventID, timeToWait, future, null);
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
			{
				throw (IOException)e.getCause();
			}
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public void startTransaction() throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		wrappedServer.startCollectionTransactionAsync(this, future, null);
		try
		{
			future.get();
			return;
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
			{
				throw (IOException)e.getCause();
			}
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Unexpected exception");
	}

	@Override
	public IndelibleFSTransaction commit() throws IOException
	{
		ComboFutureBase<IndelibleFSTransaction>future = new ComboFutureBase<IndelibleFSTransaction>();
		wrappedServer.commitCollectionTransactionAsync(this, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");	
	}

	@Override
	public void rollback() throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		wrappedServer.rollbackCollectionTransactionAsync(this, future, null);
		try
		{
			future.get();
			return;
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");	
	}

	@Override
	public void startReplicatedTransaction(TransactionCommittedEvent transactionEvent) throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		wrappedServer.startCollectionReplicatedTransactionAsync(this, transactionEvent, future, null);
		try
		{
			future.get();
			return;
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");	
	}

	@Override
	public void storeReplicatedSegment(ObjectID replicateSegmentID, IndelibleVersion replicateVersion, CASIDDataDescriptor sourceDescriptor,
			CASCollectionEvent curCASEvent) throws IOException
	{
		Future<Void>future = storeReplicatedSegmentAsync(replicateSegmentID, replicateVersion, sourceDescriptor, curCASEvent);
		try
		{
			future.get();
			return;
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");		
	}

	@Override
	public Future<Void> storeReplicatedSegmentAsync(ObjectID replicateSegmentID, IndelibleVersion replicateVersion,
			CASIDDataDescriptor sourceDescriptor, CASCollectionEvent curCASEvent) throws IOException
	{
		ComboFutureBase<Void>returnFuture = new ComboFutureBase<Void>();
		storeReplicatedSegmentAsync(replicateSegmentID, replicateVersion, sourceDescriptor, curCASEvent, returnFuture, null);
		return returnFuture;
	}

	@Override
	public <A> void storeReplicatedSegmentAsync(ObjectID replicateSegmentID, IndelibleVersion replicateVersion, CASIDDataDescriptor sourceDescriptor,
			CASCollectionEvent replicateEvent, AsyncCompletion<Void, A> completionHandler, A attachment) throws IOException
	{
		NetworkDataDescriptor sourceDescriptorNDD = serverConnection.getMoverSession().registerDataDescriptor(sourceDescriptor);
		wrappedServer.storeReplicatedSegmentAsync(this, replicateSegmentID, replicateVersion, sourceDescriptorNDD, replicateEvent, completionHandler, attachment);
	}

	@Override
	public CASIDDataDescriptor getMetaDataForReplication() throws IOException
	{
		ComboFutureBase<CASIDDataDescriptor>future = new ComboFutureBase<CASIDDataDescriptor>();
		wrappedServer.getMetaDataForReplicationAsync(this, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");	
	}

	@Override
	public void replicateMetaDataResource(CASIDDataDescriptor sourceMetaData, CASCollectionEvent curCASEvent) throws IOException
	{
		NetworkDataDescriptor sourceMetaDataNDD = serverConnection.getMoverSession().registerDataDescriptor(sourceMetaData);
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		wrappedServer.replicateMetaDataResourceAsync(this, sourceMetaDataNDD, curCASEvent, future, null);
		try
		{
			future.get();
			return;
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");	
	}

	@Override
	public CASSegmentIDIterator listSegments() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void repairSegment(ObjectID checkSegmentID, IndelibleVersion transactionVersion, DataVersionInfo masterData) throws IOException
	{
		NetworkDataDescriptor masterDescriptorNDD = serverConnection.getMoverSession().registerDataDescriptor(masterData.getDataDescriptor());
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		wrappedServer.repairSegmentAsync(this, checkSegmentID, transactionVersion, masterDescriptorNDD, masterData.getVersion(), future, null);
		try
		{
			future.get();
			return;
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		throw new InternalError("Unexpected exception");	
	}

	public void proxyFinalized(IndelibleVersionIteratorProxy iteratorProxy)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void proxyFinalized(CASCollectionEventIteratorProxy iteratorProxy)
	{
		// TODO Auto-generated method stub
		
	}
}
