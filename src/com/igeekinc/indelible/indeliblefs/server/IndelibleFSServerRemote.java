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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.remote.RemoteCASServer;
import com.igeekinc.indelible.oid.EntityID;

public interface IndelibleFSServerRemote extends Remote
{
    public static final String kIndelibleFSBonjourServiceName = "_indeliblefs._tcp";
    public static final String kIndelibleAuthBonjourServiceName = "_indelibleau._tcp";

    public IndelibleFSServerConnectionRemote open() throws RemoteException;
    public EntityID getServerID() throws RemoteException;
    public EntityID getSecurityServerID() throws RemoteException;
    public InetAddress getServerAddress() throws RemoteException;
    public int getServerPort() throws RemoteException;
    public RemoteCASServer getCASServer() throws RemoteException;
    
    // These two calls support testing the server to client mover connection (pulls for writes) and setting
    // up a reverse connection
    public InetSocketAddress [] getMoverAddresses(EntityID securityServerID) throws RemoteException;
	public void testReverseConnection(NetworkDataDescriptor testNetworkDescriptor) throws IOException, RemoteException;
}
