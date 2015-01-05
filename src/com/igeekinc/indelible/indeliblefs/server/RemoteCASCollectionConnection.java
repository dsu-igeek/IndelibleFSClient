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
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.events.RemoteIndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.indeliblefs.uniblock.CASStoreInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.DataVersionInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.SegmentInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentExists;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentNotFound;
import com.igeekinc.indelible.indeliblefs.uniblock.remote.RemoteCASServer;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.CASSegmentID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.util.async.RemoteAsyncCommandBlock;
import com.igeekinc.util.async.RemoteAsyncCompletionStatus;

public interface RemoteCASCollectionConnection extends Remote
{
    /**
     * Returns the unique ID of this collection
     * @return
     * @throws RemoteException 
     */
    public CASCollectionID getID() throws RemoteException;
    /**
     * Retrieves the segment identified by this CASIdentifier (checksum)
     * @param segmentID
     * @return
     * @throws IOException
     * @throws SegmentNotFound 
     */
    public NetworkDataDescriptor retrieveSegment(CASIdentifier segmentID) throws IOException, RemoteException, SegmentNotFound;
    
    /**
     * Retrieves the specified version of the segment identified by this ObjectID
     * @param segmentID
     * @param version - the version to retrieve.  Set to null for latest version
     * @param flags - Retrieves exactly the version if set to kExact.  If set to kNearest will return the first version <= specified version
     * @return
     * @throws IOException
     */
	public DataVersionInfo retrieveSegment(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws RemoteException, IOException;

	/**
     * Retrieves the CASID and length for the specified version of the segment identified by this ObjectID
     * @param segmentID
     * @param version - the version to retrieve.  Set to null for latest version
     * @param flags - Retrieves exactly the version if set to kExact.  If set to kNearest will return the first version <= specified version
     * @return
     * @throws IOException
     */
    public SegmentInfo retrieveSegmentInfo(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException, RemoteException;
    
    /**
     * Retrieves the segment identified by this ObjectID
     * @param segmentID
     * @return
     * @throws IOException
     */
    public DataVersionInfo retrieveSegment(ObjectID segmentID) throws IOException, RemoteException;
    
    /**
     * Verifies that the data matches the stored CASID
     * @param segmentID
     * @param version - the version to retrieve.  Set to null for latest version
     * @param flags - Retrieves exactly the version if set to kExact.  If set to kNearest will return the first version <= specified version
     * @return
     * @throws IOException
     */
    public boolean verifySegment(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException, RemoteException;
    
    /**
     * Stores a segment and returns the CASSegmentID assigned to it.  The caller can specify whether the segment is to
     * be marked as mutable or not.  If the mutable flag is true, the CASSegmentID returned will always be a new ID.
     * If the mutable flag is false, the CASSegmentID returned may be the CASSegmentID returned to previous call to 
     * store a segment with the same CASIdentifier.
     * @param segmentDescriptor
     * @param mutable
     * @return
     * @throws IOException
     * @throws NoSpaceException 
     */
    public CASStoreInfo storeSegment(NetworkDataDescriptor segmentDescriptor) throws IOException, RemoteException;
    /**
     * Stores a segment with a given ObjectID.  Using this call implies that the data is mutable.  If the ObjectID has already
     * been stored, an IOException will be thrown.
     * @param id
     * @param segmentDescriptor
     * @throws IOException
     * @throws SegmentExists 
     * @throws NoSpaceException 
     */
    public void storeVersionedSegment(ObjectID id, NetworkDataDescriptor segmentDescriptor) throws IOException, RemoteException, SegmentExists;
    
    /**
     * Replicates a segment from another server.  Should only be called by the ReplicationManager.  Needs to be
     * removed from the public API
     * @param replicateSegmentID
     * @param networkDescriptor
     * @param curCASEvent
     * @throws IOException
     */
	public void storeReplicatedSegment(ObjectID replicateSegmentID, IndelibleVersion version, NetworkDataDescriptor networkDescriptor,
			CASCollectionEvent curCASEvent) throws IOException, RemoteException;
	
    /**
     * Release the segment referred to by the ID.  If all segments referring to a particular CASIdentifier are released, the
     * data may be deleted.
     * @param releaseID
     * @return true if the releaseID was in the collection, false if the releaseID was not found
     * @throws IOException
     */
    public boolean releaseSegment(ObjectID releaseID) throws IOException, RemoteException;
    
    /**
     * Release the segments referred to by the ID.  If all segments referring to a particular CASIdentifier are released, the
     * data may be deleted.
     * @param releaseID
     * @return true if the releaseID was in the collection, false if the releaseID was not found
     * @throws IOException
     */
    public boolean[] bulkReleaseSegment(CASSegmentID [] releaseIDs) throws IOException, RemoteException;
    
    public Map<String, Serializable> getMetaDataResource(String mdResourceName) 
    		throws RemoteException, PermissionDeniedException, IOException;

    public void setMetaDataResource(String mdResourceName, Map<String, Serializable> resource)
    		throws RemoteException, PermissionDeniedException, IOException;
    
    public RemoteCASServer getCASServer() throws RemoteException;
    
	public CASIdentifier retrieveCASIdentifier(CASSegmentID casSegmentID) throws IOException, RemoteException;
	
	public long getLastEventID() throws RemoteException;
	public EntityID getMoverID() throws RemoteException;
	public RemoteIndelibleEventIterator getEventsForTransaction(IndelibleFSTransaction transaction) throws RemoteException, IOException;
	public RemoteIndelibleEventIterator getTransactionEventsAfterEventID(long eventID, int timeToWait) throws RemoteException, IOException;
	public RemoteIndelibleEventIterator eventsAfterID(long startingID) throws RemoteException;
	public RemoteIndelibleEventIterator eventsAfterTime(long timestamp) throws RemoteException;
	public long getLastReplicatedEventID(EntityID sourceServerID, CASCollectionID collectionID) throws RemoteException, IOException;
	public String[] listMetaDataNames() throws RemoteException, PermissionDeniedException, IOException;
	public void startTransaction() throws RemoteException, IOException;
	public IndelibleFSTransaction commit() throws RemoteException, IOException;
	public void rollback() throws RemoteException, IOException;
	public void startReplicatedTransaction(TransactionCommittedEvent transactionEvent) throws RemoteException, IOException;
	public EntityAuthentication getEntityAuthentication() throws RemoteException;
	public CASIDDataDescriptor getMetaDataForReplication() throws RemoteException, IOException;
	public void replicateMetaDataResource(CASIDDataDescriptor replicateMetaData, CASCollectionEvent curCASEvent) throws IOException, RemoteException;	
	public RemoteCASSegmentIDIterator listSegmentIDs() throws RemoteException, IOException;
	
	public RemoteAsyncCompletionStatus [] executeAsync(RemoteAsyncCommandBlock [] commandsToExecute, long timeToWaitForNewCompletionsInMS) throws RemoteException;
	public void repairSegment(ObjectID checkSegmentID, IndelibleVersion transactionVersion, DataVersionInfo masterData) throws IOException, RemoteException;
}
