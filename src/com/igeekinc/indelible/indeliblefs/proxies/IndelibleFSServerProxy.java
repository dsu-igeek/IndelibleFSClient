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
 
package com.igeekinc.indelible.indeliblefs.proxies;

import java.io.IOException;
import java.net.InetAddress;

import sun.rmi.server.UnicastRef2;
import sun.rmi.transport.tcp.TCPEndpoint;

import com.igeekinc.indelible.indeliblefs.IndelibleServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.server.IndelibleFSServerRemote;
import com.igeekinc.indelible.indeliblefs.server.RemoteCASServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.remote.RemoteCASServer;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.remote.MultiHomeRMIClientSocketFactory;

public class IndelibleFSServerProxy
{
	private IndelibleFSServerRemote wrappedServer;
	
	public IndelibleFSServerProxy(IndelibleFSServerRemote wrappedServer)
	{
		this.wrappedServer = wrappedServer;
	}

	public IndelibleServerConnectionIF open() throws IOException
	{
		return new IndelibleFSServerConnectionProxy(wrappedServer, getServerAddress());
	}

	public EntityID getServerID() throws IOException
	{
		return wrappedServer.getServerID();
	}

	public EntityID getSecurityServerID() throws IOException
	{
		return wrappedServer.getSecurityServerID();
	}

	public InetAddress getServerAddress() throws IOException
	{
		TCPEndpoint tcpEndpoint = (TCPEndpoint)((UnicastRef2)((java.rmi.server.RemoteStub)wrappedServer).getRef()).getLiveRef().getChannel().getEndpoint();
		String hostName;
		if (tcpEndpoint.getClientSocketFactory() instanceof MultiHomeRMIClientSocketFactory)
		{
			hostName = ((MultiHomeRMIClientSocketFactory)tcpEndpoint.getClientSocketFactory()).getHostname(tcpEndpoint.getHost(), tcpEndpoint.getPort());
		}
		else
		{
			hostName = tcpEndpoint.getHost();
		}
		return InetAddress.getByName(hostName);
		//return wrappedServer.getServerAddress();
	}

	public int getServerPort() throws IOException
	{
		return wrappedServer.getServerPort();
	}
	
	public CASServerConnectionIF openCASServer() throws IOException
	{
		RemoteCASServer casServer = wrappedServer.getCASServer();
		return new RemoteCASServerConnectionProxy(casServer.open(), new RemoteCASServerProxy(casServer), getServerAddress());
	}
}
