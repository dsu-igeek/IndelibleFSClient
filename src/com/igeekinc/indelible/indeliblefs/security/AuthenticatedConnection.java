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
 
package com.igeekinc.indelible.indeliblefs.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.ssl.SSLSocket;

import com.igeekinc.indelible.indeliblefs.security.afunix.AFUnixAuthenticatedSocket;
import com.igeekinc.indelible.indeliblefs.security.afunix.AuthenticatedAFUnixConnection;
import com.igeekinc.indelible.oid.EntityID;

public abstract class AuthenticatedConnection
{
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	
	public static AuthenticatedConnection getAuthenticatedConnection(Socket connectedSocket) throws IOException
	{
		if (connectedSocket instanceof SSLSocket)
			return new AuthenticatedSSLConnection((SSLSocket)connectedSocket);
		if (connectedSocket instanceof AFUnixAuthenticatedSocket)
			return new AuthenticatedAFUnixConnection((AFUnixAuthenticatedSocket)connectedSocket);
		throw new IllegalArgumentException(connectedSocket.getClass().getName()+" not a supported socket type");
	}
	
	protected AuthenticatedConnection(Socket socket) throws IOException
	{
		this.socket = socket;
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
	}
	
	protected Socket getSocket()
	{
		return socket;
	}
	
	
	public InputStream getInputStream()
	{
		return inputStream;
	}

	public OutputStream getOutputStream()
	{
		return outputStream;
	}

	public abstract EntityAuthentication [] getAuthenticatedClients() throws SecurityException;
	
	public abstract void validateSocket(EntityID expectedID) throws AuthenticationFailureException;

	public int getLocalPort()
	{
		return socket.getLocalPort();
	}
	
	public SocketAddress getRemotePort()
	{
		return socket.getRemoteSocketAddress();
	}
}
