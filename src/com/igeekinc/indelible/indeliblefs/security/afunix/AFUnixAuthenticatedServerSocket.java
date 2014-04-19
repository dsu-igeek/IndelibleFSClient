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
 
package com.igeekinc.indelible.indeliblefs.security.afunix;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.cert.X509Certificate;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.igeekinc.indelible.oid.EntityID;

public class AFUnixAuthenticatedServerSocket extends AFUNIXServerSocket
{
	private EntityID securityServerID;
	
	public static AFUnixAuthenticatedServerSocket newInstance(EntityID securityServerID) throws IOException
	{
		AFUnixAuthenticatedServerSocket instance = new AFUnixAuthenticatedServerSocket(securityServerID);
		return instance;
	}

	public static AFUnixAuthenticatedServerSocket bindOn(AFUNIXSocketAddress addr, EntityID securityServerID) throws IOException
	{
		AFUnixAuthenticatedServerSocket socket = newInstance(securityServerID);
		socket.bind(addr);
		return socket;
	}
	protected AFUnixAuthenticatedServerSocket(EntityID securityServerID) throws IOException
	{
		super();
		this.securityServerID = securityServerID;
	}

	@Override
	public Socket accept() throws IOException
	{
        if (isClosed())
            throw new SocketException("Socket is closed");
        AFUnixAuthenticatedSocket acceptSocket = (AFUnixAuthenticatedSocket)super.accept();
        acceptSocket.authenticate();
        return acceptSocket;
	}

	@Override
	protected AFUNIXSocket newSocketInstance() throws IOException
	{
		return AFUnixAuthenticatedSocket.newInstance(securityServerID);
	}


}
