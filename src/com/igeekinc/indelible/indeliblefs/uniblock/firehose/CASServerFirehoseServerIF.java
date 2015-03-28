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
import java.util.Map;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleVersionIteratorHandle;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.indeliblefs.uniblock.DataVersionInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentExists;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentNotFound;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASServerConnectionProxy;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.CASSegmentID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectID;

public interface CASServerFirehoseServerIF
{

	public CommandResult addClientSessionAuthentication(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			SessionAuthentication sessionAuthentication);

	public 	CommandResult addCollection(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, CASCollectionID addCollectionID) throws IOException;

	public CommandResult addConnectedServer(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, EntityID serverID,
			EntityID securityServerID);

	public CommandResult commit(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle) throws IOException;

	public CommandResult createNewCollection(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle) throws IOException;

	public CommandResult listCollections(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle);

	public CommandResult openCollectionConnection(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, CASCollectionID collectionID) throws CollectionNotFoundException, IOException;

	public CommandResult eventsAfterIDIterator(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, long startingID);

	public CommandResult eventsAfterTimeIterator(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, long timestamp);

	public CommandResult retrieveMetaData(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, String name) throws IOException;

	public CommandResult rollback(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle);

	public CommandResult setupReverseMoverConnection(CASServerClientInfoIF clientInfo, CASServerConnectionProxy connection, EntityID objectID,
			InetAddress connectToAddress, int connectToPort) throws IOException, AuthenticationFailureException;

	public CommandResult startTransaction(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle) throws IOException;

	public CommandResult storeMetaData(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, String name,
			NetworkDataDescriptor networkDataDescriptor) throws IOException;

	public CommandResult testReverseConnection(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			NetworkDataDescriptor networkDataDescriptor) throws IOException;

	public CommandResult getMoverAddresses(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, EntityID objectID);

	public CommandResult getMoverID(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle);
	
	public CommandResult getTestDescriptor(CASServerClientInfoIF clientInfo,CASServerConnectionHandle connectionHandle);

	public CommandResult getLastEventID(CASServerClientInfoIF clientInfo, CASServerConnectionHandle casServerConnectionHandle);

	public CommandResult getLastReplicatedEventID(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, EntityID objectID,
			CASCollectionID collectionID);

	public CommandResult getServerEventsAfterIDIterator(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, long startingID, int timeToWait) throws IOException;

	public CommandResult getSessionAuthentication(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle);

	public CommandResult getCollectionLastEventID(CASServerClientInfoIF clientInfo, CASServerConnectionHandle serverConnectionHandle, CASCollectionConnectionHandle connectionHandle);
	
	public CommandResult getCollectionLastReplicatedEventID(CASServerClientInfoIF clientInfo, CASServerConnectionHandle serverConnectionHandle, CASCollectionConnectionHandle connectionHandle, EntityID objectID,
			CASCollectionID collectionID);

	public CommandResult openCASServerConnection(CASServerClientInfoIF clientInfo) throws PermissionDeniedException, IOException;

	public CommandResult getSecurityServerID(CASServerClientInfoIF clientInfo) throws IOException;

	public CommandResult nextEventListItems(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			IndelibleEventIteratorHandle handle) throws IOException;

	public CommandResult collectionEventsAfterIDIterator(CASServerClientInfoIF clientInfo, CASServerConnectionHandle serverConnectionHandle, CASCollectionConnectionHandle connectionHandle,
			long startingID) throws IOException;

	public CommandResult collectionEventsAfterTimeIterator(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, long timestamp) throws IOException;

	public CommandResult nextCollectionEventListItems(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, IndelibleEventIteratorHandle handle) throws IOException;

	public CommandResult retrieveSegmentByCASIdentifier(CASServerClientInfoIF clientInfo, CASServerConnectionHandle serverConnectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, CASIdentifier casIdentifier) throws IOException, SegmentNotFound;

	public CommandResult retrieveSegmentByObjectID(CASServerClientInfoIF clientInfo, CASServerConnectionHandle serverConnectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID objectID) throws IOException, SegmentNotFound;

	public CommandResult retrieveSegmentByObjectIDAndVersion(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID objectID, IndelibleVersion indelibleVersion, RetrieveVersionFlags retrieveFlags) throws IOException, SegmentNotFound;

	public CommandResult storeSegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, NetworkDataDescriptor networkDataDescriptor) throws IOException;

	public CommandResult releaseSegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID objectID) throws IOException;

	public CommandResult bulkReleaseSegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, CASSegmentID[] releaseIDs) throws IOException;

	public CommandResult startCollectionReplicatedTransaction(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, TransactionCommittedEvent transactionEvent) throws IOException;
	
	public CommandResult startCollectionTransaction(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle) throws IOException;

	public CommandResult rollbackCollectionTransaction(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle) throws IOException;
	
	public CommandResult commitCollectionTransaction(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle) throws IOException;

	public CommandResult verifySegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID objectID, IndelibleVersion indelibleVersion,
			RetrieveVersionFlags flagForNum) throws IOException;

	public CommandResult storeReplicatedSegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID objectID, IndelibleVersion indelibleVersion,
			NetworkDataDescriptor networkDataDescriptor, CASCollectionEvent replicatedEvent) throws IOException;

	public CommandResult repairSegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID objectID, IndelibleVersion indelibleVersion, DataVersionInfo masterData) throws IOException;

	public CommandResult replicateMetaDataResource(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle casCollectionConnectionHandle, NetworkDataDescriptor networkDataDescriptor, CASCollectionEvent casCollectionEvent) throws IOException;

	public CommandResult storeVersionedSegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID objectID, NetworkDataDescriptor networkDataDescriptor) throws IOException, SegmentExists;

	public CommandResult releaseVersionedSegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID objectID, IndelibleVersion indelibleVersion) throws IOException;

	public CommandResult retrieveCASIdentifier(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, CASSegmentID objectID) throws IOException;

	public CommandResult getMetaDataForReplication(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle) throws IOException;

	public CommandResult nextVersionListItems(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, IndelibleVersionIteratorHandle handle) throws IOException;


	public CommandResult listVersionsForSegmentInRange(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID id, IndelibleVersion first, IndelibleVersion last) throws IOException;

	public CommandResult listVersionsForSegment(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, ObjectID id) throws IOException;

	public CommandResult getEventsForTransaction(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, IndelibleFSTransaction transaction) throws IOException;

	public CommandResult getTransactionEventsAfterEventID(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, long eventID, int timeToWait) throws IOException;

	public CommandResult getMetaDataResource(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, String resourceName) throws IOException, PermissionDeniedException;

	public CommandResult setMetaDataResource(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle, String mdResourceName, Map<String, Serializable> resource) throws IOException, PermissionDeniedException;

	public CommandResult closeCASServerConnection(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle);

	public CommandResult listMetaDataNames(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, CASCollectionConnectionHandle collectionConnectionHandle) throws IOException, PermissionDeniedException;

	public CommandResult startListeningForCollection(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle,
			CASCollectionConnectionHandle collectionConnectionHandle) throws IOException;

	public CommandResult pollForCollectionEvents(CASServerClientInfoIF clientInfo, CASServerConnectionHandle connectionHandle, int maxEvents, long timeout) throws IOException, InterruptedException;

	public CommandResult getServerID(CASServerClientInfoIF clientInfo) throws IOException;
}
