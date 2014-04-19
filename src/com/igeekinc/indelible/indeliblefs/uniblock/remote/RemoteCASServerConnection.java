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
 
package com.igeekinc.indelible.indeliblefs.uniblock.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.events.RemoteIndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.server.RemoteCASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.EntityID;

public interface RemoteCASServerConnection extends Remote
{
	public void close() throws RemoteException;
	public boolean isClosed() throws RemoteException;
	
    public RemoteCASCollectionConnection getCollection(CASCollectionID id) throws CollectionNotFoundException, RemoteException;
    
    public RemoteCASCollectionConnection createNewCollection() throws RemoteException, IOException;
    
    public EntityID getServerID() throws RemoteException;
    
    /**
     * Returns the EntityAuthentication for the connected client
     * @return
     * @throws RemoteException
     */
	public EntityAuthentication getClientEntityAuthentication() throws RemoteException;
	
    /**
     * Returns the EntityAuthentication for the server
     * @return
     * @throws RemoteException
     */
	public EntityAuthentication getServerEntityAuthentication() throws RemoteException;
	
    public CASCollectionID [] listCollections() throws RemoteException;
    
    public NetworkDataDescriptor retrieveMetaData(String name) throws IOException, RemoteException;
    
    public void storeMetaData(String name, NetworkDataDescriptor metaData) throws IOException, RemoteException;
    
    public IndelibleVersion startTransaction() throws RemoteException, IOException;
    
    public IndelibleFSTransaction commit() throws RemoteException, IOException;
    
    public void rollback() throws RemoteException;

    public RemoteCASServer getServer() throws RemoteException;
    
    public EntityID getMoverID() throws RemoteException;

    /**
     * Get the security server ID in use for the mover
     * @return security server id
     * @throws RemoteException 
     */
    public EntityID getSecurityServerID() throws RemoteException;

	public RemoteCASCollectionConnection addCollection(CASCollectionID addCollectionID) throws RemoteException, IOException;

	public void addConnectedServer(EntityID serverID, EntityID securityServerID) throws RemoteException;
	
    /**
     * Adds a client session authentication to the ones the remote server is allowed to access
     * @param sessionAuthentication
     * @throws RemoteException
     */
	public void addClientSessionAuthentication(SessionAuthentication sessionAuthentication) throws RemoteException;
	
    /**
     * Returns the SessionAuthentication that authorizes this connection to the remote
     * Data Mover (in general, the IndelibleFSClient handles the setup
	 * of the data movers, however if you want to forward the data descriptors,
	 * you will need to forward the session authentication)
     * @return
     * @throws RemoteException
     */
	public SessionAuthentication getSessionAuthentication() throws RemoteException;
	public RemoteIndelibleEventIterator getServerEventsAfterEventID(long eventID, int timeToWait) throws IOException, RemoteException;
	public long getLastEventID() throws RemoteException;
	public long getLastReplicatedEventID(EntityID sourceServerID, CASCollectionID collectionID) throws IOException, RemoteException;
	public RemoteIndelibleEventIterator eventsAfterID(long startingID) throws RemoteException;
	public RemoteIndelibleEventIterator eventsAfterTime(long timestamp) throws RemoteException;
	
	public void createStoresForProperties(Properties initDBFSStoresProperties) throws IOException, RemoteException;
	
    // These two calls support testing the server to client mover connection (pulls for writes) and setting
    // up a reverse connection
    public InetSocketAddress [] getMoverAddresses(EntityID securityServerID) throws RemoteException;
	public void testReverseConnection(NetworkDataDescriptor testNetworkDescriptor) throws IOException, RemoteException;
	
	/**
	 * Returns a data descriptor (dummy data) for setting up third party I/O
	 * @return
	 * @throws RemoteException
	 */
	public NetworkDataDescriptor getTestDescriptor() throws RemoteException;
	public void setupReverseMoverConnection(EntityID securityServerID, InetAddress connectToAddress, int connectToPort) 
			throws IOException, RemoteException, AuthenticationFailureException;
}
