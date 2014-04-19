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
 
package com.igeekinc.indelible.indeliblefs.datamover;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.NetworkDataDescriptorID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.datadescriptor.BasicDataDescriptor;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

class AuthorizedClient
{
	private boolean sessionAuthenticationSent;
	private EntityID clientID;
	
	public AuthorizedClient(EntityID clientID)
	{
		this.clientID = clientID;
	}
	
	public EntityID getClientID()
	{
		return clientID;
	}
	
	public boolean wasSessionAuthenticationSent()
	{
		return sessionAuthenticationSent;
	}
	
	public void setSessionAuthenticationSent(boolean sessionAuthenticationSent)
	{
		this.sessionAuthenticationSent = sessionAuthenticationSent;
	}
}

public class DataMoverSession
{
	// Maximum number of data descriptors that can be outstanding
	public static final int kMaxOutstandingDescriptors = 256;
	public static void initMapping()
	{
		ObjectIDFactory.addMapping(DataMoverSession.class, DataMoverSessionID.class);
	}
    protected DataMoverSessionID sessionID;
    protected EntityID securityServerID;    // The security server that we are registered with
    DataMoverSource parentMover;
    protected HashMap<NetworkDataDescriptorID, DataDescriptor> inUseDescriptors = new HashMap<NetworkDataDescriptorID,DataDescriptor>();
    protected ObjectIDFactory oidFactory;
    protected DataMoverSession(DataMoverSessionID sessionID, EntityID securityServerID, DataMoverSource parentMover, ObjectIDFactory oidFactory)
    {
        this.sessionID = sessionID;
        this.securityServerID = securityServerID;
        this.parentMover = parentMover;
        this.oidFactory = oidFactory;
    }
    
    public ObjectID getSessionID()
    {
        return sessionID;
    }

    public SessionAuthentication addAuthorizedClient(EntityAuthentication serverToAddID) throws SSLPeerUnverifiedException, CertificateParsingException, CertificateEncodingException, InvalidKeyException, UnrecoverableKeyException, IllegalStateException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException, KeyStoreException
    {
    	SessionAuthentication returnAuthentication = EntityAuthenticationClient.getEntityAuthenticationClient().authorizeEntityForSession(serverToAddID, sessionID);
    	return returnAuthentication;
    }
    
    public enum DataDescriptorAvailability
    {
    	kAllAccess,
    	kNetworkOnlyAccess,
    	kLocalOnlyAccess
    }
    public NetworkDataDescriptor registerDataDescriptor(DataDescriptor localDataDescriptor)
    {
    	return registerDataDescriptor(localDataDescriptor, DataDescriptorAvailability.kAllAccess);
    }
    
    public NetworkDataDescriptor registerDataDescriptor(DataDescriptor localDataDescriptor, DataDescriptorAvailability availability)
    {
    	if (localDataDescriptor instanceof NetworkDataDescriptor)
    		return (NetworkDataDescriptor)localDataDescriptor;	// Pass through
        NetworkDataDescriptorID noid = (NetworkDataDescriptorID)oidFactory.getNewOID(NetworkDataDescriptor.class);
        CASIdentifier casIdentifier = null;
        if (localDataDescriptor instanceof CASIDDataDescriptor)
        {
            casIdentifier = ((CASIDDataDescriptor)localDataDescriptor).getCASIdentifier();
        }
        DataDescriptor originalDescriptor = sendStrategy(localDataDescriptor);

        InetSocketAddress[] networkPorts;
        if (availability == DataDescriptorAvailability.kAllAccess || availability == DataDescriptorAvailability.kNetworkOnlyAccess)
        	networkPorts = parentMover.getHostPorts(securityServerID);
        else
        	networkPorts = new InetSocketAddress[0];
		File localSocketFile = null;
		if (availability == DataDescriptorAvailability.kAllAccess || availability == DataDescriptorAvailability.kLocalOnlyAccess)
			localSocketFile = parentMover.getLocalSocket(securityServerID);
		NetworkDataDescriptor returnDescriptor = new NetworkDataDescriptor(parentMover.getEntityID(), securityServerID, sessionID, noid, 
                casIdentifier, localDataDescriptor.getLength(), networkPorts,
                localSocketFile, originalDescriptor);
        // For small sizes we return the data in the descriptor.  No need to register it
        if (returnDescriptor.getShareableDescriptor() == null || (!(returnDescriptor.getShareableDescriptor() instanceof CASIDMemoryDataDescriptor)))
        {
        	synchronized(inUseDescriptors)
        	{
        		if (inUseDescriptors.size() > kMaxOutstandingDescriptors)
        			throw new IndexOutOfBoundsException("Too many descriptors outstanding - max is "+kMaxOutstandingDescriptors);
        		inUseDescriptors.put(noid, localDataDescriptor);
        	}
        }
        return returnDescriptor;
    }
    
    
    /**
     * Decides whether to include the data descriptor along with the network descriptor
     * @param checkDescriptor
     * @return
     */
    public DataDescriptor sendStrategy(DataDescriptor checkDescriptor)
    {
        DataDescriptor returnDescriptor = null;
        if (checkDescriptor.getLength() <= 1024)    // For little descriptors, just wrap them into the network descriptor
        {
            if (checkDescriptor.descriptorContainsData())
            {
                returnDescriptor = checkDescriptor;
            }
            else
            {
                try
                {
                    returnDescriptor = new BasicDataDescriptor(checkDescriptor);
                } catch (IOException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                }
            } 
        }
        else
        {
            if (!(checkDescriptor.descriptorContainsData()) && checkDescriptor.isShareableWithLocalProcess())  // We don't want to send big memory data descriptors around
            {
                returnDescriptor = checkDescriptor;
            }
        }
        return returnDescriptor;
    }
    
    public DataDescriptor getDataDescriptor(NetworkDataDescriptorID noid)
    {
        synchronized(inUseDescriptors)
        {
            DataDescriptor dataDescriptor = inUseDescriptors.get(noid);
            return dataDescriptor;
        }
    }
    public void removeDataDescriptor(DataDescriptor writeDescriptor)
    {
        if (writeDescriptor instanceof NetworkDataDescriptor)
            removeDataDescriptor(((NetworkDataDescriptor)writeDescriptor).getID());
    }
    
    public void removeDataDescriptor(NetworkDataDescriptorID noid)
    {
        synchronized(inUseDescriptors)
        {
            inUseDescriptors.remove(noid);
        }
    }
    public void close()
    {
        inUseDescriptors.clear();
        parentMover.closeSession(this);
    }

    public boolean isAuthorized(EntityAuthentication [] authenticatedClients, SessionAuthentication sessionAuthentication)
    {
    	if (sessionAuthentication.getSessionID().equals(sessionID))
    	{
    		try
			{
    			boolean matchesClient = false;
    			for (EntityAuthentication checkClient:authenticatedClients)
    			{
    				if (checkClient.getEntityID().equals(sessionAuthentication.getAuthenticatedEntityID()))
    				{
    					matchesClient = true;
    					break;
    				}
    			}
    			if (matchesClient)
    			{
    				EntityAuthenticationClient.getEntityAuthenticationClient().checkSessionAuthentication(sessionID, sessionAuthentication);
    				return true;
    			}
			} catch (Throwable e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
    	}
    	return false;
    }
    
    public boolean wasSessionAuthenticationSent()
    {
    	return false;
    }
    
    public int getNumberOfRegisteredDescriptors()
    {
    	synchronized(inUseDescriptors)
    	{
    		return inUseDescriptors.size();
    	}
    }
}
