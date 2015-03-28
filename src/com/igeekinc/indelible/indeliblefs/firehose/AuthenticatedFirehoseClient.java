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
package com.igeekinc.indelible.indeliblefs.firehose;

import java.io.IOException;
import java.net.SocketAddress;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.log4j.Logger;

import com.igeekinc.firehose.FirehoseClient;
import com.igeekinc.firehose.FirehoseInitiator;
import com.igeekinc.firehose.SSLFirehoseChannel;
import com.igeekinc.indelible.indeliblefs.security.AuthenticatedInitiatorSSLSetup;
import com.igeekinc.indelible.indeliblefs.security.AuthenticatedSSLSetup;
import com.igeekinc.indelible.indeliblefs.security.AuthenticatedTargetSSLSetup;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.logging.ErrorLogMessage;

public abstract class AuthenticatedFirehoseClient extends FirehoseClient
{
	private EntityAuthentication serverAuthentication;
	private EntityID	serverID;
	private AuthenticatedSSLSetup sslSetup;
	
	public AuthenticatedFirehoseClient(SocketAddress address) throws IOException
	{
		sslSetup = new AuthenticatedInitiatorSSLSetup();
		FirehoseInitiator.initiateClient(address, this, sslSetup);
	}

	public AuthenticatedFirehoseClient(SocketAddress [] addresses) throws IOException
	{
		sslSetup = new AuthenticatedInitiatorSSLSetup();
		FirehoseInitiator.initiateClient(addresses, this, sslSetup);
	}

	/*
	 * This constructor is for clients that support reverse connections
	 */
	public AuthenticatedFirehoseClient(AuthenticatedTargetSSLSetup sslSetup)
	{
		this.sslSetup = sslSetup;
	}
	
	public EntityID getConnectionAuthenticationServerID()
	{
		return sslSetup.getConnectionAuthenticationServerID();
	}
	
	public EntityID getServerEntityID() throws IOException
	{
		if (serverID == null)
		{
			try
			{
				serverID = getServerAuthentication().getEntityID();
			} catch (AuthenticationFailureException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new IOException("Connection to server is not authorized");
			}
		}
		return serverID;
	}
	
	public EntityAuthentication getServerAuthentication() throws AuthenticationFailureException
	{
		if (serverAuthentication == null)
			try
			{
				serverAuthentication = EntityAuthenticationClient.getEntityAuthenticationClient().getServerEntityAuthenticationForFirehoseChannel((SSLFirehoseChannel)remoteChannel);
			} catch (SSLPeerUnverifiedException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new AuthenticationFailureException("Peer unverified");
			}
		return serverAuthentication;
	}
}
