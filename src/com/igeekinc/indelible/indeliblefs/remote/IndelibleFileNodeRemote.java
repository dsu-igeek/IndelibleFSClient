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
import java.rmi.RemoteException;

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public interface IndelibleFileNodeRemote extends IndelibleFSObjectRemote
{
    /* (non-Javadoc)
     * @see com.igeekinc.util.FileLike#lastModified()
     */
    public abstract long lastModified() throws RemoteException;

    /* (non-Javadoc)
     * @see com.igeekinc.util.FileLike#isDirectory()
     */
    public abstract boolean isDirectory() throws RemoteException;

    /* (non-Javadoc)
     * @see com.igeekinc.util.FileLike#isFile()
     */
    public abstract boolean isFile() throws RemoteException;

    /* (non-Javadoc)
     * @see com.igeekinc.util.FileLike#totalLength()
     */
    public abstract long totalLength() throws RemoteException;
    
    public abstract long lengthWithChildren() throws RemoteException;

    public abstract int getReferenceCount() throws RemoteException;

    public abstract String [] listForkNames() throws RemoteException;
    
    public abstract IndelibleFSForkRemote getFork(String name, boolean createIfNecessary) throws IOException, RemoteException, ForkNotFoundException;
	
	public abstract void deleteFork(String forkName) throws RemoteException, IOException, ForkNotFoundException, PermissionDeniedException;
	
    public abstract IndelibleFSObjectID getVolumeID() throws RemoteException;
    
    public IndelibleFSObjectRemote getObjectForVersion(IndelibleVersion version, RetrieveVersionFlags flags) throws RemoteException, IOException;


}