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
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class AuthenticatedSSLConnection extends AuthenticatedConnection
{
	protected AuthenticatedSSLConnection(SSLSocket connectedSocket) throws IOException
	{
		super(connectedSocket);
	}

	@Override
	public EntityAuthentication[] getAuthenticatedClients()
			throws SecurityException
	{
    	ArrayList<EntityAuthentication>authenticatedClients = new ArrayList<EntityAuthentication>();
        Certificate[] clientCertificates;
		try
		{
			clientCertificates = ((SSLSocket)getSocket()).getSession().getPeerCertificates();
		} catch (SSLPeerUnverifiedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new SecurityException("No peer certificates");
		}
        for (Certificate curCertificate:clientCertificates)
        {
            if (curCertificate instanceof X509Certificate)
            {
                EntityAuthentication curAuthentication = new EntityAuthentication((X509Certificate)curCertificate);
                if (EntityAuthenticationClient.getEntityAuthenticationClient().checkAuthentication(curAuthentication))
                    authenticatedClients.add(curAuthentication);
            }
        }
        EntityAuthentication [] returnAuthentications = new EntityAuthentication[authenticatedClients.size()];
        returnAuthentications = authenticatedClients.toArray(returnAuthentications);
        return returnAuthentications;
	}

	@Override
	protected SSLSocket getSocket()
	{
		return (SSLSocket)super.getSocket();
	}

	@Override
	public void validateSocket(EntityID expectedID) throws AuthenticationFailureException
	{
		EntityAuthenticationClient.getEntityAuthenticationClient().checkSSLSocket(getSocket(), expectedID);
	}

}
