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
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import com.igeekinc.indelible.indeliblefs.security.AuthenticatedConnection;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.oid.EntityID;

public class AuthenticatedAFUnixConnection extends AuthenticatedConnection
{

	public AuthenticatedAFUnixConnection(AFUnixAuthenticatedSocket connectedSocket) throws IOException
	{
		super(connectedSocket);
	}
	
	@Override
	public EntityAuthentication[] getAuthenticatedClients()
			throws SecurityException
	{
    	ArrayList<EntityAuthentication>authenticatedClients = new ArrayList<EntityAuthentication>();
        Certificate[] clientCertificates;
        clientCertificates = ((AFUnixAuthenticatedSocket)getSocket()).getPeerCertificates();
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
	protected AFUnixAuthenticatedSocket getSocket()
	{
		return (AFUnixAuthenticatedSocket)super.getSocket();
	}

	@Override
	public void validateSocket(EntityID expectedID) throws AuthenticationFailureException
	{
		EntityAuthenticationClient.getEntityAuthenticationClient().checkAFUnixAuthenticatedSocket(getSocket(), expectedID);
	}
}
