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
 
package com.igeekinc.indelible.indeliblefs.uniblock;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.Future;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventSource;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentNotFound;
import com.igeekinc.indelible.oid.CASSegmentID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.util.async.AsyncCompletion;

public interface CASCollectionConnection extends IndelibleEventSource
{
	/**
	 * Returns the collection we're connected to
	 * @return
	 */
	public CASCollection getCollection();
    /**
     * Retrieves the segment identified by this CASIdentifier (checksum)
     * @param segmentID
     * @return
     * @throws IOException
     * @throws SegmentNotFound 
     */
    public CASIDDataDescriptor retrieveSegment(CASIdentifier segmentID) throws IOException, SegmentNotFound;
    
    public Future<CASIDDataDescriptor>retrieveSegmentAsync(CASIdentifier segmentID) throws IOException, SegmentNotFound;
    public <A> void retrieveSegmentAsync(CASIdentifier segmentID, AsyncCompletion<CASIDDataDescriptor, A> completionHandler, A attachment) throws IOException, SegmentNotFound;
    /**
     * Retrieves the latest version of the segment identified by this ObjectID
     * @param segmentID
     * @return
     * @throws IOException
     */
    public DataVersionInfo retrieveSegment(ObjectID segmentID) throws IOException;
    public Future<DataVersionInfo>retrieveSegmentAsync(ObjectID segmentID) throws IOException, SegmentNotFound;
    public <A> void retrieveSegmentAsync(ObjectID segmentID, AsyncCompletion<DataVersionInfo, A> completionHandler, A attachment) throws IOException, SegmentNotFound;
   
    /**
     * Retrieves the specified version of the segment identified by this ObjectID
     * @param segmentID
     * @param version - the version to retrieve.  Set to null for latest version
     * @param flags - Retrieves exactly the version if set to kExact.  If set to kNearest will return the first version <= specified version
     * @return
     * @throws IOException
     */
    public DataVersionInfo retrieveSegment(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException;
    public Future<DataVersionInfo>retrieveSegmentAsync(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException, SegmentNotFound;
    public <A> void retrieveSegmentAsync(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags, AsyncCompletion<DataVersionInfo, A> completionHandler, A attachment) throws IOException, SegmentNotFound;

    /**
     * Retrieves the CASID and length for the specified version of the segment identified by this ObjectID
     * @param segmentID
     * @param version - the version to retrieve.  Set to null for latest version
     * @param flags - Retrieves exactly the version if set to kExact.  If set to kNearest will return the first version <= specified version
     * @return
     * @throws IOException
     */
    public SegmentInfo retrieveSegmentInfo(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException;
    
    /**
     * Verifies that the data matches the stored CASID
     * @param segmentID
     * @param version - the version to retrieve.  Set to null for latest version
     * @param flags - Retrieves exactly the version if set to kExact.  If set to kNearest will return the first version <= specified version
     * @return
     * @throws IOException
     */
    public boolean verifySegment(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags) throws IOException;
    
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
    public CASStoreInfo storeSegment(CASIDDataDescriptor segmentDescriptor) throws IOException;
	public Future<CASStoreInfo> storeSegmentAsync(CASIDDataDescriptor sourceDescriptor) throws IOException;
	public <A>void storeSegmentAsync(CASIDDataDescriptor sourceDescriptor, AsyncCompletion<CASStoreInfo, ? super A>completionHandler, A attachment) throws IOException;

    /**
     * Stores a segment with a given ObjectID.  The version will be that allocated to the current transaction
     * @param id
     * @param segmentDescriptor
     * @throws IOException
     * @throws NoSpaceException 
     */
    public void storeVersionedSegment(ObjectID id, CASIDDataDescriptor segmentDescriptor) throws IOException;
    
    /**
     * Release the segment referred to by the ID.  If all segments referring to a particular CASIdentifier are released, the
     * data may be deleted.
     * @param releaseID
     * @return true if the releaseID was in the collection, false if the releaseID was not found
     * @throws IOException
     */
    public boolean releaseSegment(CASSegmentID releaseID) throws IOException;
    
    /**
     * Release the segments referred to by the ID.  If all segments referring to a particular CASIdentifier are released, the
     * data may be deleted.
     * @param releaseID
     * @return true if the releaseID was in the collection, false if the releaseID was not found
     * @throws IOException
     */
    public boolean[] bulkReleaseSegment(CASSegmentID [] releaseIDs) throws IOException;
    
    public boolean releaseVersionedSegment(ObjectID id, IndelibleVersion version) throws IOException;
    
    /**
     * Lists all of the versions for the specified segment.  Returns null if the segment ID cannot be found
     * @param id
     * @return
     * @throws IOException
     */
    public IndelibleVersionIterator listVersionsForSegment(ObjectID id) throws IOException;
    
    /**
     * Lists the versions in the range (inclusive) for the specified segments.  Returns null if the segmente ID cannot be found.
     * @param id - segment to list versions for
     * @param first - First version to return (pass null for first version)
     * @param last - last version to return (pass null for latest)
     * @return
     * @throws IOException
     */
    public IndelibleVersionIterator listVersionsForSegmentInRange(ObjectID id, IndelibleVersion first, IndelibleVersion last) throws IOException;
	
    public String [] listMetaDataNames() throws PermissionDeniedException, IOException;
	
    public HashMap<String, Serializable> getMetaDataResource(String mdResourceName) 
    		throws PermissionDeniedException, IOException;

    public void setMetaDataResource(String mdResourceName, HashMap<String, Serializable> resource)
    		throws PermissionDeniedException, IOException;
    
    public CASServer getCASServer();
    
	public CASIdentifier retrieveCASIdentifier(CASSegmentID casSegmentID) throws IOException;
	
	public IndelibleEventIterator getEventsForTransaction(IndelibleFSTransaction transaction) throws IOException;
	
	public IndelibleEventIterator getTransactionEventsAfterEventID(long eventID, int timeToWait) throws IOException;
	
    public void startTransaction() throws IOException;
    public IndelibleFSTransaction commit() throws IOException;
    public void rollback() throws IOException;
    
    public void startReplicatedTransaction(TransactionCommittedEvent transactionEvent) throws IOException;
    
    /**
     * Replicates a segment from another server.  Should only be called by the ReplicationManager.  Needs to be
     * removed from the public API
     * @param replicateSegmentID
     * @param replicateVersion
     * @param sourceDescriptor
     * @param curCASEvent
     * @throws IOException
     */
	public void storeReplicatedSegment(ObjectID replicateSegmentID, IndelibleVersion replicateVersion, CASIDDataDescriptor sourceDescriptor, CASCollectionEvent curCASEvent) throws IOException;
	
	public Future<Void> storeReplicatedSegmentAsync(ObjectID replicateSegmentID, IndelibleVersion replicateVersion, CASIDDataDescriptor sourceDescriptor, CASCollectionEvent curCASEvent) throws IOException;
	public <A>void storeReplicatedSegmentAsync(ObjectID replicateSegmentID, IndelibleVersion replicateVersion, CASIDDataDescriptor sourceDescriptor, CASCollectionEvent curCASEvent, AsyncCompletion<Void, ? super A>completionHandler, A attachment) throws IOException;
	public CASIDDataDescriptor getMetaDataForReplication() throws IOException;
	public void replicateMetaDataResource(CASIDDataDescriptor sourceMetaData,
			CASCollectionEvent curCASEvent) throws IOException;
	
	public CASSegmentIDIterator listSegments() throws IOException;
	public void repairSegment(ObjectID checkSegmentID, IndelibleVersion transactionVersion, DataVersionInfo masterData) throws IOException;
}
