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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSelectorProvider;
import org.newsclub.net.unix.AFUNIXSocketChannelImpl;

import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.logging.ErrorLogMessage;

public class AuthenticatedInitiatorSSLSetup implements AuthenticatedSSLSetup
{
	private Logger logger = Logger.getLogger(getClass());
	private EntityID connectionAuthenticationServerID;
	
	public SSLContext getSSLContextForSocket(SocketChannel socketChannel) throws IOException
	{
		SSLContext sslContext;
		try
		{
			logger.debug("Finding SSL Context for socket "+socketChannel.toString());
			EntityAuthenticationClient entityAuthenticationClient = EntityAuthenticationClient.getEntityAuthenticationClient();
			EntityAuthenticationServer [] trustedServers = entityAuthenticationClient.listTrustedServers();
			if (trustedServers == null || trustedServers.length == 0)
				throw new IOException("No trusted authentication servers");	// We don't trust anybody!

			byte [] numServersBuffer = new byte[Integer.SIZE/8];
			readFully(socketChannel, numServersBuffer, 10000L);
			int numServers = BitTwiddle.javaByteArrayToInt(numServersBuffer, 0);
			if (numServers > 256)
			{
				throw new IOException("Too many EA servers");
			}
			logger.debug("Remote offering "+numServers+" servers");
			byte [] serverIDBytes = new byte[EntityID.kTotalBytes];
			EntityID [] serverTrustedServerIDs = new EntityID[numServers];
			for (int curServerNum = 0; curServerNum < numServers; curServerNum++)
			{
				readFully(socketChannel, serverIDBytes, 10000L);
				serverTrustedServerIDs[curServerNum] = (EntityID)ObjectIDFactory.reconstituteFromBytes(serverIDBytes);
				logger.debug("Got remote server "+serverTrustedServerIDs[curServerNum]);
			}
			logger.debug("Finding common server");
			connectionAuthenticationServerID = null;
			finished: for (EntityID checkID:serverTrustedServerIDs)
			{
				logger.debug("Checking "+checkID);
				for (EntityAuthenticationServer checkTrustedServer:trustedServers)
				{
					if (checkID.equals(checkTrustedServer.getEntityID()))
					{
						logger.debug("Matches "+checkTrustedServer);
						connectionAuthenticationServerID = checkID;
						break finished;
					}
					logger.debug("No match to "+checkTrustedServer);
				}
			}
			if (connectionAuthenticationServerID == null)
			{
				logger.error("Remote offering "+numServers+" servers");
				for (int curServerNum = 0; curServerNum < numServers; curServerNum++)
				{
					logger.error("Remote server "+serverTrustedServerIDs[curServerNum]);
				}
				logger.error("Our trusted servers: "+trustedServers.length);
				for (EntityAuthenticationServer curTrustedServer:trustedServers)
				{
					logger.error(curTrustedServer.getServerAddress()+" = "+curTrustedServer.getEntityID());
				}
				throw new IOException("No common authentication servers");
			}
			connectionAuthenticationServerID.getBytes(serverIDBytes, 0);
			writeFully(socketChannel, serverIDBytes);
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(entityAuthenticationClient.getKeyManagers(connectionAuthenticationServerID), 
					entityAuthenticationClient.getTrustManagers(connectionAuthenticationServerID), new SecureRandom());
			logger.debug("Successfully created SSLContext for EntityAuthenticationServer "+connectionAuthenticationServerID);
		} catch (Exception e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Could not initialize SSL Context");
		}
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
	
	/*
	 * Read the complete buffer or timeout
	 */
	public static void readFully(SocketChannel channel, byte [] buffer, long timeoutMS)
	throws IOException
	{
		if (channel.isBlocking())
			throw new IllegalArgumentException("Channel must be in non-blocking mode");
		Selector selector;
		if (channel instanceof AFUNIXSocketChannelImpl)
			selector = AFUNIXSelectorProvider.provider().openSelector();
		else
			selector = Selector.open();
		try
		{
			channel.register(selector, SelectionKey.OP_READ);
			long timeStarted = System.currentTimeMillis();
			long waitTime = timeoutMS;
			ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

			while (byteBuffer.hasRemaining())
			{
				int channelsSelected = selector.select(waitTime);
				long elapsedTime = System.currentTimeMillis() - timeStarted;
				if (channelsSelected == 0)
				{
					// timed out - we'll exception out below
				}
				else
				{
					channel.read(byteBuffer);
				}
				selector.selectedKeys().clear();
				waitTime = timeoutMS - elapsedTime;
				if (waitTime <= 0 && byteBuffer.hasRemaining())
					throw new IOException("Read timed out");
			}
		}
		finally
		{
			selector.close();
		}
	}
	
	public static void writeFully(SocketChannel channel, byte [] buffer)
	throws IOException
	{
		ByteBuffer writeBuffer = ByteBuffer.wrap(buffer);
		while (writeBuffer.hasRemaining())
		{
			int bytesWritten = channel.write(writeBuffer);
			if (bytesWritten == 0)
			{
				try
				{
					Thread.sleep(10);	// Give the channel some time to clear
				}
				catch (InterruptedException e)
				{
					
				}
			}
		}
	}
}
