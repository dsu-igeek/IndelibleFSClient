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
package com.igeekinc.indelible.indeliblefs.uniblock.firehose;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.firehose.AuthenticatedFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleVersionIteratorHandle;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.NextVersionListItemsReply;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.CASStoreInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.DataVersionInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.SegmentInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASServerProxy;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.AddClientSessionAuthenticationMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.AddCollectionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.AddConnectedServerMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.BulkReleaseSegmentMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CASCollectionQueuedEventMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CASServerCommandMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CloseMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CommitCollectionTransactionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CommitMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CreateNewCollectionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CreateNewCollectionReply;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetCollectionEventsAfterIDIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetCollectionEventsAfterTimeIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetCollectionLastEventIDMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetCollectionLastReplicatedEventIDMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetEventsAfterIDIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetEventsAfterTimeIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetEventsForTransactionIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetLastEventIDMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetLastReplicatedEventIDMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetMetaDataForReplicationMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetMetaDataMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetMetaDataResourceMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetMoverAddressesMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetMoverIDMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetSecurityServerIDMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetServerEventsAfterEventIDIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetServerIDMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetSessionAuthenticationMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetTestDescriptorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetTransactionEventsAfterEventIDIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetVersionsForSegmentInRangeIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetVersionsForSegmentIteratorMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.ListCollectionsMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.ListMetaDataNamesMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.NextCASServerEventListItemsMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.NextCASServerEventListItemsReply;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.NextCollectionEventListItemsMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.NextCollectionEventListItemsReply;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.NextVersionListItemsMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.OpenCASServerConnectionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.OpenCollectionConnectionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.PollForCollectionEventsMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.ReleaseSegmentMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.ReleaseVersionedSegmentMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.RepairSegmentMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.ReplicateMetaDataResourceMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.RetrieveCASIdentifierMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.RetrieveSegmentByCASIdentifierMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.RetrieveSegmentByObjectIDAndVersionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.RetrieveSegmentByObjectIDMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.RetrieveSegmentInfoMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.RollbackCollectionTransactionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.RollbackMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.SetMetaDataMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.SetMetaDataResource;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.SetupReverseMoverConnectionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.StartCollectionReplicatedTransactionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.StartCollectionTransactionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.StartListeningForCollectionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.StartTransactionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.StoreReplicatedSegmentMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.StoreSegmentMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.StoreVersionedSegmentMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.TestReverseConnectionMessage;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.VerifySegmentMessage;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.CASSegmentID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;

public class CASServerFirehoseClient extends AuthenticatedFirehoseClient
{
	public CASServerFirehoseClient(SocketAddress address) throws IOException
	{
		super(address);
	}

	@SuppressWarnings("rawtypes")
	public static Class<? extends CASServerCommandMessage> getClassForCommandCode(int commandCode)
	{
		switch (CASServerCommand.getCommandForNum(commandCode))
		{
		case kIllegalCommand:
			throw new IllegalArgumentException("Illegal command 0");
		case kAddClientSessionAuthentication:
			return AddClientSessionAuthenticationMessage.class;
		case kAddCollection:
			return AddCollectionMessage.class;
		case kAddConnectedServer:
			return AddConnectedServerMessage.class;
		case kClose:
			return CloseMessage.class;
		case kCommit:
			return CommitMessage.class;
		case kCreateNewCollection:
			return CreateNewCollectionMessage.class;
		case kOpenCollectionConnection:
			return OpenCollectionConnectionMessage.class;
		case kGetEventsAfterIDIterator:
			return GetEventsAfterIDIteratorMessage.class;
		case kGetEventsAfterTimeIterator:
			return GetEventsAfterTimeIteratorMessage.class;
		case kGetLastEventID:
			return GetLastEventIDMessage.class;
		case kGetLastReplicatedEventID:
			return GetLastReplicatedEventIDMessage.class;
		case kGetServerEventsAfterEventIDIterator:
			return GetServerEventsAfterEventIDIteratorMessage.class;
		case kGetSessionAuthentication:
			return GetSessionAuthenticationMessage.class;
		case kListCollections:
			return ListCollectionsMessage.class;
		case kOpenConnection:
			return OpenCASServerConnectionMessage.class;
		case kGetMetaData:
			return GetMetaDataMessage.class;
		case kRollback:
			return RollbackMessage.class;
		case kSetupReverseMoverConnection:
			return SetupReverseMoverConnectionMessage.class;
		case kStartTransaction:
			return StartTransactionMessage.class;
		case kSetMetaData:
			return SetMetaDataMessage.class;
		case kTestReverseConnection:
			return TestReverseConnectionMessage.class;
		case kGetMoverAddresses:
			return GetMoverAddressesMessage.class;
		case kGetMoverID:
			return GetMoverIDMessage.class;
		case kGetTestDescriptor:
			return GetTestDescriptorMessage.class;
		case kGetCollectionLastEventID:
			return GetCollectionLastEventIDMessage.class;
		case kGetCollectionLastReplicatedEventID:
			return GetCollectionLastReplicatedEventIDMessage.class;
		case kGetSecurityServerID:
			return GetSecurityServerIDMessage.class;
		case kGetCollectionEventsAfterIDIterator:
			return GetCollectionEventsAfterIDIteratorMessage.class;
		case kGetCollectionEventsAfterTimeIterator:
			return GetCollectionEventsAfterTimeIteratorMessage.class;
		case kNextCASServerEventListItems:
			return NextCASServerEventListItemsMessage.class;
		case kRepairSegment:
			return RepairSegmentMessage.class;
		case kReplicateMetaDataResource:
			return ReplicateMetaDataResourceMessage.class;
		case kRetrieveSegmentByCASIdentifier:
			return RetrieveSegmentByCASIdentifierMessage.class;
		case kRetrieveSegmentByObjectID:
			return RetrieveSegmentByObjectIDMessage.class;
		case kStoreReplicatedSegment:
			return StoreReplicatedSegmentMessage.class;
		case kStoreSegment:
			return StoreSegmentMessage.class;
		case kStoreVersionedSegment:
			return StoreVersionedSegmentMessage.class;
		case kVerifySegment:
			return VerifySegmentMessage.class;
		case kRetrieveSegmentByObjectIDAndVersion:
			return RetrieveSegmentByObjectIDAndVersionMessage.class;
		case kRetrieveSegmentInfo:
			return RetrieveSegmentInfoMessage.class;
		case kReleaseVersionedSegment:
			return ReleaseVersionedSegmentMessage.class;
		case kBulkReleaseSegment:
			return BulkReleaseSegmentMessage.class;
		case kCommitCollectionTransaction:
			return CommitCollectionTransactionMessage.class;
		case kListMetaDataNames:
			return ListMetaDataNamesMessage.class;
		case kReleaseSegment:
			return ReleaseSegmentMessage.class;
		case kRetrieveCASIdentifier:
			return RetrieveCASIdentifierMessage.class;
		case kRollbackCollectionTransaction:
			return RollbackCollectionTransactionMessage.class;
		case kStartCollectionReplicatedTransaction:
			return StartCollectionReplicatedTransactionMessage.class;
		case kStartCollectionTransaction:
			return StartCollectionTransactionMessage.class;
		case kGetMetaDataForReplication:
			return GetMetaDataForReplicationMessage.class;
		case kGetVersionsForSegmentInRangeIterator:
			return GetVersionsForSegmentInRangeIteratorMessage.class;
		case kGetVersionsForSegmentIterator:
			return GetVersionsForSegmentIteratorMessage.class;
		case kNextVersionListItems:
			return NextVersionListItemsMessage.class;
		case kGetEventsForTransactionIterator:
			return GetEventsForTransactionIteratorMessage.class;
		case kGetTransactionEventsAfterEventIDIterator:
			return GetTransactionEventsAfterEventIDIteratorMessage.class;
		case kGetMetaDataResource:
			return GetMetaDataResourceMessage.class;
		case kSetMetaDataResource:
			return SetMetaDataResource.class;
		case kNextCASCollectionEventListItems:
			return NextCollectionEventListItemsMessage.class;
		case kStartListeningForCollection:
			return StartListeningForCollectionMessage.class;
		case kPollForCollectionEvents:
			return PollForCollectionEventsMessage.class;
		case kGetServerID:
			return GetServerIDMessage.class;
		default:
			break;
		}
		throw new InternalError(IndelibleFSServerCommand.getCommandForNum(commandCode)+" not configured");
	}
	
	private static HashMap<Integer, Class<? extends Object>>returnClassMap = new HashMap<Integer, Class<? extends Object>>();


	@SuppressWarnings({ "unchecked", "rawtypes" })	// Warnings?  We don't need no stinkin' warnings.
	public static Class<? extends Object> getReturnClassForCommandCodeStatic(int commandCode)
	{
		synchronized(returnClassMap)
		{
			Class<? extends Object> returnClass = returnClassMap.get(commandCode);
			if (returnClass == null)
			{
				Class<? extends CASServerCommandMessage>commandClass = getClassForCommandCode(commandCode);
				try
				{
					CASServerCommandMessage commandClassInstance = commandClass.getConstructor().newInstance();
					if (commandClassInstance.getCommandCode() != commandCode)
						throw new InternalError("Command class inconsistent");
					returnClass = commandClassInstance.getResultClass();
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
	
	@Override
	protected Class<? extends Object> getReturnClassForCommandCode(int commandCode)
	{
		return CASServerFirehoseClient.getReturnClassForCommandCodeStatic(commandCode);
	}

	@Override
	public Throwable getExtendedThrowableForErrorCode(int errorCode)
	{
		return IndelibleFSFirehoseClient.getExtendedThrowableForErrorCodeStatic(errorCode);
	}

	public CASServer<CASServerConnectionIF> getCASServer()
	{
		return new CASServerProxy(this);
	}
	
	public <A> void openAsync(AsyncCompletion<CASServerConnectionHandle, A>completionHandler, A attachment) throws IOException
	{
		OpenCASServerConnectionMessage<A> openMessage = new OpenCASServerConnectionMessage<A>(completionHandler, attachment);
		sendMessage(openMessage);
	}
	
	public <A>void getServerIDAsync(AsyncCompletion<EntityID, A>completionHandler, A attachment) throws IOException
	{
		com.igeekinc.indelible.indeliblefs.uniblock.msgpack.GetServerIDMessage<A> getServerIDMessage = new GetServerIDMessage<A>(completionHandler, attachment);
		sendMessage(getServerIDMessage);
	}
	
	public <A> void getSecurityServerIDAsync(AsyncCompletion<EntityID, A>completionHandler, A attachment) throws IOException
	{
		GetSecurityServerIDMessage<A> openMessage = new GetSecurityServerIDMessage<A>(completionHandler, attachment);
		sendMessage(openMessage);
	}
	public <A> void getLastEventIDAsync(CASServerConnectionProxy casServerConnection, AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		GetLastEventIDMessage<A>getLastEventIDMessage = new GetLastEventIDMessage<A>(this, casServerConnection, completionHandler, attachment);
		sendMessage(getLastEventIDMessage);
	}
	
	public <A> void getLastReplicatedEventID(CASServerConnectionProxy casServerConnection, EntityID sourceServerID, CASCollectionID collectionID,
			AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		GetLastReplicatedEventIDMessage<A>message = new GetLastReplicatedEventIDMessage<A>(this, casServerConnection, sourceServerID, collectionID, completionHandler, attachment);
		sendMessage(message);
		
	}
	
	private void sendMessage(CASServerCommandMessage<?, ?, ?> message) throws IOException
	{
		// CASServerCommandMessage is the completion handler, a dessert topping and a floor wax!
		sendMessage(message, message, null);
	}

	public <A>void openCollectionConnectionAsync(CASServerConnectionProxy casServerConnection, CASCollectionID collectionID, AsyncCompletion<CASCollectionConnectionHandle, A>completionHandler, A attachment) throws IOException
	{
		OpenCollectionConnectionMessage<A>openCollectionConnectionMessage = new OpenCollectionConnectionMessage<A>(this, casServerConnection, collectionID, completionHandler, attachment);
		sendMessage(openCollectionConnectionMessage);
	}

	public <A>void createNewCollectionAsync(CASServerConnectionProxy connection,
			AsyncCompletion<CreateNewCollectionReply, A> completionHandler, A attachment) throws IOException
	{
		CreateNewCollectionMessage<A>message = new CreateNewCollectionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void getCollectionLastEventIDAsync(CASCollectionConnectionProxy connection, AsyncCompletion<Long, A> completionHandler, A attachment) throws IOException
	{
		GetCollectionLastEventIDMessage<A>message = new GetCollectionLastEventIDMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A> void getCollectionLastReplicatedEventID(CASCollectionConnectionProxy connection, EntityID sourceServerID, CASCollectionID collectionID,
			AsyncCompletion<Long, A>completionHandler, A attachment) throws IOException
	{
		GetCollectionLastReplicatedEventIDMessage<A>message = new GetCollectionLastReplicatedEventIDMessage<A>(this, connection, sourceServerID, collectionID, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void eventsAfterIDAsync(CASServerConnectionProxy casServerConnectionProxy, long startingID, AsyncCompletion<IndelibleEventIterator, A> future, A attachment) throws IOException
	{
		GetEventsAfterIDIteratorMessage<A>message = new GetEventsAfterIDIteratorMessage<A>(this, casServerConnectionProxy, startingID, future, attachment);
		sendMessage(message);
	}

	public <A>void collectionEventsAfterIDAsync(CASCollectionConnectionProxy collectionConnection, long startingID, AsyncCompletion<IndelibleEventIterator, A> future, A attachment) throws IOException
	{
		GetCollectionEventsAfterIDIteratorMessage<A>message = new GetCollectionEventsAfterIDIteratorMessage<A>(this, collectionConnection, startingID, future, attachment);
		sendMessage(message);
	}
	
	public <A>void collectionEventsAfterTimeAsync(CASCollectionConnectionProxy collectionConnection, long timestamp, AsyncCompletion<IndelibleEventIterator, A> future, A attachment) throws IOException
	{
		GetCollectionEventsAfterTimeIteratorMessage<A>message = new GetCollectionEventsAfterTimeIteratorMessage<A>(this, collectionConnection, timestamp, future, attachment);
		sendMessage(message);
	}
	
	public <A>void eventsAfterTimeAsync(CASServerConnectionProxy casServerConnectionProxy, long timestamp, AsyncCompletion<IndelibleEventIterator, A> future, A attachment) throws IOException
	{
		GetEventsAfterTimeIteratorMessage<A>message = new GetEventsAfterTimeIteratorMessage<A>(this, casServerConnectionProxy, timestamp, future, attachment);
		sendMessage(message);
	}
	
	public <A>void nextCASServerEventListItemAsync(CASServerConnectionProxy connection, IndelibleEventIteratorHandle handle,
			AsyncCompletion<NextCASServerEventListItemsReply, A>completionHandler, A attachment) throws IOException
	{
		NextCASServerEventListItemsMessage<A> message = new NextCASServerEventListItemsMessage<A>(this, connection, handle, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void nextCASCollectionEventListItemAsync(CASCollectionConnectionProxy connection, IndelibleEventIteratorHandle handle,
			AsyncCompletion<NextCollectionEventListItemsReply, A>completionHandler, A attachment) throws IOException
	{
		NextCollectionEventListItemsMessage<A> message = new NextCollectionEventListItemsMessage<A>(this, connection, handle, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void retrieveSegmentByCASIdentifierASync(CASCollectionConnectionProxy collectionConnection, CASIdentifier segmentID,
			AsyncCompletion<CASIDDataDescriptor, A> completionHandler, A attachment) throws IOException
	{
		RetrieveSegmentByCASIdentifierMessage<A>message = new RetrieveSegmentByCASIdentifierMessage<A>(this, collectionConnection, segmentID, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void retrieveSegmentByObjectID(CASCollectionConnectionProxy collectionConnection, ObjectID segmentID,
			AsyncCompletion<DataVersionInfo, A> completionHandler, A attachment) throws IOException
	{
		RetrieveSegmentByObjectIDMessage<A>message = new RetrieveSegmentByObjectIDMessage<A>(this, collectionConnection, segmentID, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void retrieveSegmentByObjectIDAndVersion(CASCollectionConnectionProxy collectionConnection, ObjectID segmentID,
			IndelibleVersion version, RetrieveVersionFlags flags, AsyncCompletion<DataVersionInfo, A> completionHandler, A attachment) throws IOException
	{
		RetrieveSegmentByObjectIDAndVersionMessage<A>message = new RetrieveSegmentByObjectIDAndVersionMessage<A>(this, collectionConnection, segmentID, version, flags, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void retrieveSegmentInfo(CASCollectionConnectionProxy collectionConnection, ObjectID segmentID,
			IndelibleVersion version, RetrieveVersionFlags flags, AsyncCompletion<SegmentInfo, A> completionHandler, A attachment) throws IOException
	{
		RetrieveSegmentInfoMessage<A>message = new RetrieveSegmentInfoMessage<A>(this, collectionConnection, segmentID, version, flags, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void verifySegmentInfo(CASCollectionConnectionProxy collectionConnection, ObjectID segmentID,
			IndelibleVersion version, RetrieveVersionFlags flags, AsyncCompletion<Boolean, A> completionHandler, A attachment) throws IOException
	{
		VerifySegmentMessage<A>message = new VerifySegmentMessage<A>(this, collectionConnection, segmentID, version, flags, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void storeSegmentAsync(CASCollectionConnectionProxy collectionConnection, NetworkDataDescriptor sourceDescriptor, AsyncCompletion<CASStoreInfo, A> completionHandler, A attachment) throws IOException
	{
		StoreSegmentMessage<A>message = new StoreSegmentMessage<A>(this, collectionConnection, sourceDescriptor, completionHandler, attachment);
		sendMessage(message);
	}
	
	
	public <A>void storeReplicatedSegmentAsync(CASCollectionConnectionProxy collectionConnection, ObjectID replicateSegmentID, IndelibleVersion replicateVersion, 
			NetworkDataDescriptor sourceDescriptor, CASCollectionEvent replicateEvent, AsyncCompletion<Void, A> completionHandler, A attachment) throws IOException
	{
		StoreReplicatedSegmentMessage<A>message = new StoreReplicatedSegmentMessage<A>(this, collectionConnection, replicateSegmentID, replicateVersion, sourceDescriptor, replicateEvent, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void storeVersionedSegmentAsync(CASCollectionConnectionProxy collectionConnection, ObjectID id, NetworkDataDescriptor segmentDescriptor, AsyncCompletion<Void, A> completionHandler, A attachment) throws IOException
	{
		StoreVersionedSegmentMessage<A>message = new StoreVersionedSegmentMessage<A>(this, collectionConnection, id, segmentDescriptor, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void listCollectionsAsync(CASServerConnectionProxy connection, AsyncCompletion<CASCollectionID [], A> completionHandler, A attachment) throws IOException
	{
		ListCollectionsMessage<A>message = new ListCollectionsMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void retrieveMetaData(CASServerConnectionProxy connection, String name, AsyncCompletion<CASIDDataDescriptor, A> completionHandler, A attachment) throws IOException
	{
		GetMetaDataMessage<A> message = new GetMetaDataMessage<A>(this, connection, name, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void storeMetaData(CASServerConnectionProxy connection, String name, NetworkDataDescriptor metadata, AsyncCompletion<Void, A>completionHandler,
			A attachment) throws IOException
	{
		SetMetaDataMessage<A> message = new SetMetaDataMessage<A>(this, connection, name, metadata, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void startTransaction(CASServerConnectionProxy connection, AsyncCompletion<IndelibleVersion, A> completionHandler, A attachment) throws IOException
	{
		StartTransactionMessage<A>message = new StartTransactionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void commit(CASServerConnectionProxy connection, AsyncCompletion<IndelibleFSTransaction, A> completionHandler, A attachment) throws IOException
	{
		CommitMessage<A>message = new CommitMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void rollback(CASServerConnectionProxy connection, AsyncCompletion<Void, A> completionHandler, A attachment) throws IOException
	{
		RollbackMessage<A>message = new RollbackMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void addCollection(CASServerConnectionProxy connection, CASCollectionID addCollectionID,
			AsyncCompletion<CASCollectionConnectionHandle, A> completionHandler, A attachment) throws IOException
	{
		AddCollectionMessage<A>message = new AddCollectionMessage<A>(this, connection, addCollectionID, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void addConnectedServer(CASServerConnectionProxy connection, EntityID serverID, EntityID securityServerID,
			AsyncCompletion<Void, A> completionHandler, A attachment) throws IOException
	{
		AddConnectedServerMessage<A>message = new AddConnectedServerMessage<A>(this, connection, serverID, securityServerID, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void addClientSessionAuthentication(CASServerConnectionProxy connection, SessionAuthentication sessionAuthentication,
			AsyncCompletion<Void, A> completionHandler, A attachment) throws IOException
	{
		AddClientSessionAuthenticationMessage<A> message = new AddClientSessionAuthenticationMessage<A>(this, connection, sessionAuthentication, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void getServerEventsAfterEventIDAsync(CASServerConnectionProxy connection, long startingID, int timeToWait,
			AsyncCompletion<IndelibleEventIterator, A> completionHandler, A attachment) throws IOException
	{
		GetServerEventsAfterEventIDIteratorMessage<A>message = new GetServerEventsAfterEventIDIteratorMessage<A>(this, connection, startingID, timeToWait, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void releaseSegmentAsync(CASCollectionConnectionProxy collectionConnection, ObjectID releaseID, AsyncCompletion<Boolean, A> completionHandler, A attachment) throws IOException
	{
		ReleaseSegmentMessage<A>message = new ReleaseSegmentMessage<A>(this, collectionConnection, releaseID, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void bulkReleaseSegmentAsync(CASCollectionConnectionProxy collectionConnection, CASSegmentID[] releaseIDs,
			AsyncCompletion<boolean[], A> future, A attachment) throws IOException
	{
		BulkReleaseSegmentMessage<A>message = new BulkReleaseSegmentMessage<A>(this, collectionConnection, releaseIDs, future, attachment);
		sendMessage(message);
	}

	public <A>void startCollectionReplicatedTransactionAsync(CASCollectionConnectionProxy connection,
			TransactionCommittedEvent transactionEvent, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		StartCollectionReplicatedTransactionMessage<A>message = new StartCollectionReplicatedTransactionMessage<A>(this, connection, transactionEvent, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void rollbackCollectionTransactionAsync(CASCollectionConnectionProxy connection, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		RollbackCollectionTransactionMessage<A>message = new RollbackCollectionTransactionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void startCollectionTransactionAsync(CASCollectionConnectionProxy connection, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		StartCollectionTransactionMessage<A>message = new StartCollectionTransactionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void commitCollectionTransactionAsync(CASCollectionConnectionProxy connection, AsyncCompletion<IndelibleFSTransaction, A>completionHandler, A attachment) throws IOException
	{
		CommitCollectionTransactionMessage<A>message = new CommitCollectionTransactionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void repairSegmentAsync(CASCollectionConnectionProxy collectionConnection, ObjectID checkSegmentID, IndelibleVersion transactionVersion, NetworkDataDescriptor masterDescriptor, IndelibleVersion masterVersion, AsyncCompletion<Void, A> completionHandler, A attachment) throws IOException
	{
		RepairSegmentMessage<A>message = new RepairSegmentMessage<A>(this, collectionConnection, checkSegmentID, transactionVersion, masterDescriptor, masterVersion, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void replicateMetaDataResourceAsync(CASCollectionConnectionProxy collectionConnection, NetworkDataDescriptor sourceMetaDataNDD,
			CASCollectionEvent curCASEvent, AsyncCompletion<Void, A> completionHandler, A attachment) throws IOException
	{
		ReplicateMetaDataResourceMessage<A>message = new ReplicateMetaDataResourceMessage<A>(this, collectionConnection, sourceMetaDataNDD, curCASEvent, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void listMetaDataNamesAsync(CASCollectionConnectionProxy collectionConnection, AsyncCompletion<String[], A> completionHandler, A attachment) throws IOException
	{
		ListMetaDataNamesMessage<A>message = new ListMetaDataNamesMessage<A>(this, collectionConnection, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void releaseVersionedSegmentAsync(CASCollectionConnectionProxy collectionConnection, ObjectID id, IndelibleVersion version,
			AsyncCompletion<Boolean, A> completionHandler, A attachment) throws IOException
	{
		ReleaseVersionedSegmentMessage<A>message = new ReleaseVersionedSegmentMessage<A>(this, collectionConnection, id, version, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void retrieveCASIdentifierAsync(CASCollectionConnectionProxy collectionConnection, CASSegmentID casSegmentID,
			AsyncCompletion<CASIdentifier, A> completionHandler, A attachment) throws IOException
	{
		RetrieveCASIdentifierMessage<A>message = new RetrieveCASIdentifierMessage<A>(this, collectionConnection, casSegmentID, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void getMetaDataForReplicationAsync(CASCollectionConnectionProxy collectionConnection,
			AsyncCompletion<CASIDDataDescriptor, A> completionHandler, A attachment) throws IOException
	{
		GetMetaDataForReplicationMessage<A>message = new GetMetaDataForReplicationMessage<A>(this, collectionConnection, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void nextVersionsListItem(CASCollectionConnectionProxy connection, IndelibleVersionIteratorHandle handle,
			AsyncCompletion<NextVersionListItemsReply, A>completionHandler, A attachment) throws IOException
	{
		NextVersionListItemsMessage<A>message = new NextVersionListItemsMessage<A>(this, connection, handle, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void listVersionForSegmentAsync(CASCollectionConnectionProxy connection, ObjectID id,
			AsyncCompletion<IndelibleVersionIterator, A>completionHandler, A attachment) throws IOException
	{
		GetVersionsForSegmentIteratorMessage<A>message = new GetVersionsForSegmentIteratorMessage<A>(this, connection, id, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void listVersionForSegmentInRangeAsync(CASCollectionConnectionProxy connection, ObjectID id,
			IndelibleVersion first, IndelibleVersion last,
			AsyncCompletion<IndelibleVersionIterator, A>completionHandler, A attachment) throws IOException
	{
		GetVersionsForSegmentInRangeIteratorMessage<A>message = new GetVersionsForSegmentInRangeIteratorMessage<A>(this, connection, id, first, last, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void getEventsForTransactionAsync(CASCollectionConnectionProxy connection, IndelibleFSTransaction transaction,
			AsyncCompletion<IndelibleEventIterator, A>completionHandler, A attachment) throws IOException
	{
		GetEventsForTransactionIteratorMessage<A>message = new GetEventsForTransactionIteratorMessage<A>(this, connection, transaction, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void getTransactionEventsAfterEventID(CASCollectionConnectionProxy connection, long eventID, int timeToWait,
			AsyncCompletion<IndelibleEventIterator, A>completionHandler, A attachment) throws IOException
	{
		GetTransactionEventsAfterEventIDIteratorMessage<A>message = new GetTransactionEventsAfterEventIDIteratorMessage<A>(this, connection, eventID, timeToWait, completionHandler, attachment);
		sendMessage(message);
	}

	public <A>void getMetaDataResource(CASCollectionConnectionProxy connection, String mdResourceName,
			AsyncCompletion<Map<String, Serializable>, A>completionHandler, A attachment) throws IOException
	{
		GetMetaDataResourceMessage<A>message = new GetMetaDataResourceMessage<A>(this, connection, mdResourceName, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void getTestDescriptorAsync(CASServerConnectionProxy connection, AsyncCompletion<NetworkDataDescriptor, A>completionHandler,
		A attachment) throws IOException
	{
		GetTestDescriptorMessage<A>message = new GetTestDescriptorMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void testReverseConnectionAsync(CASServerConnectionProxy connection, NetworkDataDescriptor testDescriptor, AsyncCompletion<Void, A>completionHandler,	A attachment) throws IOException
	{
		TestReverseConnectionMessage<A>message = new TestReverseConnectionMessage<A>(this, connection, testDescriptor, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void getMoverAddressesAsync(CASServerConnectionProxy connection, EntityID securityServerID, AsyncCompletion<InetSocketAddress [], A>completionHandler,	A attachment) throws IOException
	{
		GetMoverAddressesMessage<A>message = new GetMoverAddressesMessage<A>(this, connection, securityServerID, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void setupReverseMoverConnectionAsync(CASServerConnectionProxy connection, EntityID securityServerID, InetAddress address, int port, AsyncCompletion<Void, A>completionHandler,	A attachment) throws IOException
	{
		SetupReverseMoverConnectionMessage<A>message = new SetupReverseMoverConnectionMessage<A>(this, connection, securityServerID, address, port, completionHandler, null);
		sendMessage(message);
	}
	
	public <A>void getSessionAuthenticationAsync(CASServerConnectionProxy connection, AsyncCompletion<SessionAuthentication, A>completionHandler,	A attachment) throws IOException
	{
		GetSessionAuthenticationMessage<A>message = new GetSessionAuthenticationMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void startListeningForCollection(CASCollectionConnectionProxy connection, 
			AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		StartListeningForCollectionMessage<A>message = new StartListeningForCollectionMessage<A>(this, connection, completionHandler, attachment);
		sendMessage(message);
	}
	
	public <A>void pollForCollectionEventsAsync(CASServerConnectionProxy connection, int maxEvents, long timeout, AsyncCompletion<CASCollectionQueuedEventMsgPack [], A>completionHandler,	A attachment) throws IOException
	{
		PollForCollectionEventsMessage<A>message = new PollForCollectionEventsMessage<A>(this, connection, maxEvents, timeout, completionHandler, attachment);
		sendMessage(message);
	}
}
