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
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.remote.MultiHomeRMIClientSocketFactory;

public class IndelibleEntityAuthenticationClientRMIClientSocketFactory extends MultiHomeRMIClientSocketFactory
{
    private static final long serialVersionUID = -3260035190601012078L;
    private EntityID securityServerID;
	private transient Logger logger;
    
    public IndelibleEntityAuthenticationClientRMIClientSocketFactory(EntityID securityServerID)
    {
        this.securityServerID = securityServerID;
    }
    
    public Socket createSocket(String hostListString, int port) throws IOException
    {
    	if (logger == null)
   		 logger = Logger.getLogger(getClass());
    	String host;
    	if (resolvedHost != null)
    	{
    		host = resolvedHost;
    	}
    	else
    	{
    		host = MultiHomeRMIClientSocketFactory.getHostnameFromList(hostListString, port);
    		resolvedHost = host;
    	}
        SSLContext sslContext;
        try
        {

            logger.debug("Creating client RMI SSL socket to "+host+":"+port+" SecurityServerID = "+securityServerID);
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(EntityAuthenticationClient.getEntityAuthenticationClient().getKeyManagers(securityServerID), EntityAuthenticationClient.getEntityAuthenticationClient().getTrustManagers(securityServerID), null);
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            return socketFactory.createSocket(host, port);
        } catch (GeneralSecurityException e)
        {
            logger.error(new ErrorLogMessage("Caught exception"), e);
            throw new IOException("Could not create client socket due to security exception", e);
        }
    }

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((securityServerID == null) ? 0 : securityServerID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndelibleEntityAuthenticationClientRMIClientSocketFactory other = (IndelibleEntityAuthenticationClientRMIClientSocketFactory) obj;
		if (securityServerID == null)
		{
			if (other.securityServerID != null)
				return false;
		} else if (!securityServerID.equals(other.securityServerID))
			return false;
		return true;
	}
    

} 
