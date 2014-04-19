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
import java.util.HashMap;
import java.util.Properties;

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSVolumeRemote;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public interface IndelibleFSServerConnectionRemote extends Remote
{
    public IndelibleFSVolumeRemote createVolume(Properties volumeProperties)
    throws IOException, RemoteException, PermissionDeniedException;
    
    public IndelibleFSVolumeRemote retrieveVolume(IndelibleFSObjectID retrieveVolumeID)
    throws VolumeNotFoundException, RemoteException, IOException;
    
    public IndelibleFSObjectID [] listVolumes() throws RemoteException, IOException;
    
    public void startTransaction() throws RemoteException, IOException;
    public boolean inTransaction() throws RemoteException, IOException;
    public IndelibleVersion commit() throws RemoteException, IOException;
    public IndelibleVersion commitAndSnapshot(HashMap<String, Serializable>snapshotMetadata) throws RemoteException, IOException, PermissionDeniedException;
    public void rollback() throws RemoteException, IOException;

    public void close() throws RemoteException, IOException;

    public EntityAuthentication getClientEntityAuthentication() throws RemoteException;
    public EntityAuthentication getServerEntityAuthentication() throws RemoteException;
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
}
