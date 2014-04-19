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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketImpl;

import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class AFUnixAuthenticatedSocket extends AFUNIXSocket
{
	private EntityID securityServerID, peerEntityID;
	private X509Certificate [] peerCertChain;
    public static AFUnixAuthenticatedSocket newInstance(EntityID securityServerID) throws IOException 
    {
        final AFUNIXSocketImpl impl = new AFUNIXSocketImpl();
        AFUnixAuthenticatedSocket instance =  new AFUnixAuthenticatedSocket(securityServerID, impl);
        instance.impl = impl;	// There's a reason why we shouldn't override fields...
        return instance;
    }
    public static AFUnixAuthenticatedSocket connectTo(AFUNIXSocketAddress addr, EntityID securityServerID) throws IOException
    {
    	AFUnixAuthenticatedSocket socket = newInstance(securityServerID);
        socket.connect(addr);
        return socket;
    }
    
    protected AFUnixAuthenticatedSocket(EntityID securityServerID, AFUNIXSocketImpl impl) throws IOException
    {
    	super(impl);
    	this.securityServerID = securityServerID;
    }
	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException
	{
		super.connect(endpoint, timeout);
		boolean shouldClose = true;
		try
		{
			EntityAuthenticationClient eac = EntityAuthenticationClient.getEntityAuthenticationClient();
			
			// The input and output streams from the socket are already open held in AFUnixSocketImpl and are
			// not new instances.  They must not be closed here or the socket will close.  Since they're held
			// in AFUnixSocketImpl the GC will not dispose/close them
			InputStream inStream = getInputStream();
			OutputStream outStream = getOutputStream();
			
			peerCertChain = AFUnixAuthenticatedSocketUtils.readCertChain(inStream);
			try
			{
				peerEntityID = eac.checkCertificateChain(peerCertChain, securityServerID);
			} catch (GeneralSecurityException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new IOException("Server certificate chain did not start from a trusted server");
			}
			AFUnixAuthenticatedSocketUtils.challengePeer(outStream, inStream, peerCertChain[0]);
			AFUnixAuthenticatedSocketUtils.writeCertChain(outStream, securityServerID);
			AFUnixAuthenticatedSocketUtils.handleChallenge(inStream, outStream);
			shouldClose = false;
		} catch (AuthenticationFailureException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Authentication failed"), e);
			throw new IOException("Authentication failed");
		}
		finally
		{
			if (shouldClose)
				close();
		}
	}
	
	protected void authenticate() throws IOException
	{
		boolean shouldClose = true;
		try
		{
			// The input and output streams from the socket are already open held in AFUnixSocketImpl and are
			// not new instances.  They must not be closed here or the socket will close.  Since they're held
			// in AFUnixSocketImpl the GC will not dispose/close them
			OutputStream outStream = getOutputStream();
			InputStream inStream = getInputStream();

			AFUnixAuthenticatedSocketUtils.writeCertChain(outStream, securityServerID);
			AFUnixAuthenticatedSocketUtils.handleChallenge(inStream, outStream);
			peerCertChain = AFUnixAuthenticatedSocketUtils.readCertChain(inStream);
			try
			{
				peerEntityID = EntityAuthenticationClient.getEntityAuthenticationClient().checkCertificateChain(peerCertChain, securityServerID);
			} catch (GeneralSecurityException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new IOException("Server certificate chain did not start from a trusted server");
			}
			AFUnixAuthenticatedSocketUtils.challengePeer(outStream, inStream, peerCertChain[0]);

			shouldClose = false;
			return;
		} catch (AuthenticationFailureException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Authentication failed");
		}
		finally
		{
			if (shouldClose)
				close();
		}
	}
    
    public EntityID getPeerEntityID()
    {
    	return peerEntityID;
    }
    
    public X509Certificate [] getPeerCertificates()
    {
    	// Just one for now
    	X509Certificate [] returnCertificates = new X509Certificate[1];
    	returnCertificates[0] = peerCertChain[0];
    	return returnCertificates;
    }
    
    protected AFUNIXSocketImpl getSocketImpl()
    {
    	return impl;
    }
}
