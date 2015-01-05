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
import java.rmi.RemoteException;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventSource;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.datadescriptor.DataDescriptor;

public interface CASServerConnectionIF extends IndelibleEventSource
{

	public abstract void close();
	public abstract boolean isClosed();
	public abstract CASCollectionConnection openCollectionConnection(CASCollectionID id) throws CollectionNotFoundException, IOException;

	public abstract CASCollectionConnection createNewCollection() throws IOException;
	public abstract void deleteCollection(CASCollectionID id) throws CollectionNotFoundException, IOException;
	public abstract EntityID getServerID() throws IOException;
	public abstract EntityID getSecurityServerID();
	
	public abstract CASCollectionID[] listCollections();

	public abstract DataDescriptor retrieveMetaData(String name)
			throws IOException;

	public abstract void storeMetaData(String name, DataDescriptor metaData)
			throws IOException;

	public abstract IndelibleVersion startTransaction() throws IOException;

	public abstract IndelibleFSTransaction commit() throws IOException;

	public abstract void rollback();

	public abstract CASServer getServer();

	public abstract CASCollectionConnection addCollection(CASCollectionID curCollectionID) throws IOException;
	
	public abstract void addConnectedServer(EntityID serverID, EntityID securityServerID);
	
    /**
     * Adds a client session authentication to the ones the remote server is allowed to access
     * @param sessionAuthentication
     * @throws RemoteException
     */
	public void addClientSessionAuthentication(SessionAuthentication sessionAuthentication);
	
	/**
	 * Does all the work necessary to set up direct (third-party) I/O between this server and the designated server
	 * @param receivingServer
	 * @throws RemoteException 
	 * @throws IOException 
	 * @throws AuthenticationFailureException 
	 */
	public void prepareForDirectIO(CASServerConnectionIF receivingServer) throws IOException, RemoteException, AuthenticationFailureException;
    /**
     * Returns the SessionAuthentication that authorizes this connection to the remote
     * Data Mover (in general, the IndelibleFSClient handles the setup
	 * of the data movers, however if you want to forward the data descriptors,
	 * you will need to forward the session authentication)
     * @return
     * @throws RemoteException
     */
	public SessionAuthentication getSessionAuthentication();
	
	public IndelibleEventIterator getServerEventsAfterEventID(long eventID, int timeToWait) throws IOException;
	public abstract void finalizeVersion(IndelibleVersion snapshotVersion);
}