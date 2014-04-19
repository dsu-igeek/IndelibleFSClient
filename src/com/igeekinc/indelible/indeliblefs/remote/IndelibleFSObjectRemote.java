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
 
package com.igeekinc.indelible.indeliblefs.remote;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public interface IndelibleFSObjectRemote extends Remote
{
    public abstract IndelibleFSObjectID getObjectID() throws RemoteException;

    public abstract String[] listMetaDataResources()
            throws PermissionDeniedException, RemoteException, IOException;

    public abstract HashMap<String, Object> getMetaDataResource(String mdResourceName) 
        throws RemoteException, PermissionDeniedException, IOException;
    
    public abstract IndelibleFSObjectRemote setMetaDataResource(String mdResourceName, HashMap<String, Object> resource)
        throws RemoteException, PermissionDeniedException, IOException;
    
    public abstract IndelibleVersionIterator listVersions() throws RemoteException, IOException;
    
    public abstract IndelibleFSObjectRemote getObjectForVersion(IndelibleVersion version, RetrieveVersionFlags flags) throws RemoteException, IOException;
    
    public IndelibleVersion getVersion() throws RemoteException;
    
    /**
     * This is called automatically by the proxy finalize
     * @throws RemoteException
     */
    public void release() throws RemoteException;
}