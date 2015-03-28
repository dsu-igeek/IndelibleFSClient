/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igeekinc.indelible.indeliblefs.security;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.logging.ErrorLogMessage;

public class AuthenticatedTargetSSLSetup implements AuthenticatedSSLSetup
{
	private EntityID	connectionAuthenticationServerID;
	protected Logger logger = Logger.getLogger(getClass());
	
	public SSLContext getSSLContextForSocket(SocketChannel socket) throws IOException
	{
		// First send the servers that we trust
		EntityAuthenticationClient entityAuthenticationClient = EntityAuthenticationClient.getEntityAuthenticationClient();

		EntityAuthenticationServer [] trustedServers = entityAuthenticationClient.listTrustedServers();
		if (trustedServers == null || trustedServers.length == 0)
			return null;	// We don't trust anybody!
		byte [] numServers = new byte[Integer.SIZE/8];
		// Send how many servers are in our list
		BitTwiddle.intToJavaByteArray(trustedServers.length, numServers, 0);
		AuthenticatedInitiatorSSLSetup.writeFully(socket, numServers);
		logger.debug("Wrote num servers "+trustedServers.length);
		byte [] serverIDBytes = new byte[EntityID.kTotalBytes];
		// Send the ID's of the server
		for (EntityAuthenticationServer curTrustedServer:trustedServers)
		{
			EntityID curTrustedID = curTrustedServer.getEntityID();
			curTrustedID.getBytes(serverIDBytes, 0);
			AuthenticatedInitiatorSSLSetup.writeFully(socket, serverIDBytes);
		}
		
		// Now, read which server the client wants to authenticate against
		AuthenticatedInitiatorSSLSetup.readFully(socket, serverIDBytes, 10000L);
		connectionAuthenticationServerID = (EntityID)ObjectIDFactory.reconstituteFromBytes(serverIDBytes);
		SSLContext sslContext;
		try
		{
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(entityAuthenticationClient.getKeyManagers(connectionAuthenticationServerID), 
					entityAuthenticationClient.getTrustManagers(connectionAuthenticationServerID), new SecureRandom());
		} catch (KeyManagementException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("KeyManagementException");
		} catch (NoSuchAlgorithmException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("NoSuchAlgorithmException");
		}
		logger.debug("created SSLContext");
		return sslContext;
	}

	public EntityID getConnectionAuthenticationServerID()
	{
		return connectionAuthenticationServerID;
	}

	@Override
	public boolean useSSL()
	{
		return true;
	}
}
