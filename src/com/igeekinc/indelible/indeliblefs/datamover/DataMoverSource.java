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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.igeekinc.firehose.AddressFilter;
import com.igeekinc.firehose.FirehoseInitiator;
import com.igeekinc.firehose.FirehoseTarget;
import com.igeekinc.indelible.indeliblefs.security.AuthenticatedInitiatorSSLSetup;
import com.igeekinc.indelible.indeliblefs.security.AuthenticatedTargetSSLSetup;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.IndelibleFSClientOIDs;
import com.igeekinc.indelible.oid.NetworkDataDescriptorID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.DebugLogMessage;
import com.igeekinc.util.logging.ErrorLogMessage;

public class DataMoverSource extends DataMover
{
	public static final int kDefaultMaxClients = 100;   
    public static final int kCopyBufSize = 16*1024;
    
    protected HashMap<DataMoverSessionID, DataMoverSession>sessions;

    protected DataMoverServer tcpServer;	// This is the server that will process incoming commands on TCP/IP
    protected DataMoverServer afUnixServer;	// This is the server that will process incoming commands from AFUNIX sockets
    protected FirehoseTarget localTarget;
   
    int maxClients = kDefaultMaxClients;
    InetSocketAddress [] advertiseAddresses = null;
    int numSecurityServers = 0;		// We use this to differentiate AFUnix sockets for different security servers
    private static DataMoverSource singleton;
    public static boolean noServer = false;
	static
	{
		IndelibleFSClientOIDs.initMappings();
	}
	
    public static DataMoverSource getDataMoverSource()
    {
        if (singleton == null)
            throw new InternalError("DataMoverSource has not been initialized");
        return singleton;
    }
    
    public static synchronized void init(ObjectIDFactory oidFactory, InetSocketAddress listenInetPort, AFUNIXSocketAddress listenLocalAddr) throws IOException
    {
    	InetSocketAddress[] listenInetPorts;
    	if (listenInetPort != null)
    		listenInetPorts = new InetSocketAddress[]{listenInetPort};
    	else
    		listenInetPorts = null;
		init (oidFactory, listenInetPorts, listenLocalAddr);
    }
    
    public static synchronized void init(ObjectIDFactory oidFactory, InetSocketAddress [] listenInetPorts, AFUNIXSocketAddress listenLocalAddr) throws IOException
    {
        if (singleton == null)
        {
            singleton = new DataMoverSource(oidFactory, listenInetPorts, listenLocalAddr);
        }
        else
            throw new IllegalArgumentException("DataMoverSource already initialized!");
    }

	/**
     * This will render the DataMoverSource unusable until init() is called again.  Mostly useful for testing.
     */
    public static void shutdown()
    {
        singleton = null;
    }
    
    private DataMoverSource(ObjectIDFactory oidFactory, InetSocketAddress[] listenInetPorts, AFUNIXSocketAddress listenLocalAddr) throws IOException
    {
        super(oidFactory);
        sessions = new HashMap<DataMoverSessionID, DataMoverSession>();
        tcpServer = new DataMoverServer(this);
        afUnixServer = new DataMoverServer(this);
        if (listenInetPorts != null)
        {
        	for (InetSocketAddress addAddress:listenInetPorts)
        	{
        		AuthenticatedTargetSSLSetup sslSetup = new AuthenticatedTargetSSLSetup();
        		FirehoseTarget curTarget = new FirehoseTarget(addAddress, tcpServer, DataMoverReceiver.getDataMoverReceiver(), sslSetup);
        		// server maintains a list of targets so we don't
        	}
        }
        if (listenLocalAddr != null)
        {
        	AuthenticatedTargetSSLSetup sslSetup = new AuthenticatedTargetSSLSetup();
        	FirehoseTarget localTarget = new FirehoseTarget(listenLocalAddr, afUnixServer, DataMoverReceiver.getDataMoverReceiver(), sslSetup);
        	this.localTarget = localTarget;
        }
    }
    
    public void setAdvertiseAddresses(String advertiseAddressesString)
    {
    	ArrayList<InetSocketAddress>advertiseAddressesList = new ArrayList<InetSocketAddress>();
    	StringTokenizer tokenizer = new StringTokenizer(advertiseAddressesString, "!");
    	while (tokenizer.hasMoreElements())
    	{
    		String curAddressPortString = tokenizer.nextToken();
    		int curPortNum;
    		int colonPos = curAddressPortString.indexOf(':');
    		String curAddressString;
			if (colonPos > 0)
    		{
    			curAddressString = curAddressPortString.substring(0, colonPos);
    			String curPortString = curAddressPortString.substring(colonPos + 1);
    			curPortNum = Integer.parseInt(curPortString);
    		}
			else
			{
				curAddressString = curAddressPortString;
				curPortNum = tcpServer.getListenAddresses(new AddressFilter()
				{
					
					@Override
					public boolean add(InetSocketAddress checkAddress)
					{
						return (!(checkAddress instanceof AFUNIXSocketAddress));
					}
				})[0].getPort();	// If you're doing something tricky, specify the port numbers
			}
			try
			{
				InetAddress curAddress = InetAddress.getByName(curAddressString);
				InetSocketAddress curSocketAddress = new InetSocketAddress(curAddress, curPortNum);
				advertiseAddressesList.add(curSocketAddress);
			} catch (UnknownHostException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
    	}
    	InetSocketAddress [] setAddresses = advertiseAddressesList.toArray(new InetSocketAddress[advertiseAddressesList.size()]);
    	setAdvertiseAddresses(setAddresses);
    }
    
    public void setAdvertiseAddresses(InetSocketAddress [] advertiseAddresses)
    {
    	this.advertiseAddresses = advertiseAddresses;
    }
    
    public boolean openSession(DataMoverClientInfo clientInfo, EntityAuthentication[] clientAuthentications, DataMoverSessionID sessionID, SessionAuthentication checkAuthentication)
    {
		DataMoverSession openSession = getSession(sessionID);
		if (openSession != null)
		{
			if (openSession.isAuthorized(clientAuthentications, checkAuthentication))
			{
				clientInfo.addOpenSession(sessionID, openSession);
				logger.debug(new DebugLogMessage("Session {0} opened successfully", sessionID));
				return true;
			}
			else
			{
				logger.error(new ErrorLogMessage("Session {0} authentication failed", sessionID));
				return false;
			}
		}
		else
		{
			logger.error(new ErrorLogMessage("Could not find session ID {0} for OpenSession request", sessionID));
		}
		return false;
    }
    
    public DataDescriptor requestSend(DataMoverClientInfo clientInfo, DataMoverSessionID sessionID, NetworkDataDescriptorID descriptorID, int flags) throws IOException
    {
    	DataMoverSession session = clientInfo.getOpenSession(sessionID);
    	if (session == null)
    		throw new IOException("Session "+sessionID+" not opened for client");
    	DataDescriptor returnDescriptor = session.getDataDescriptor(descriptorID);
    	if (returnDescriptor == null)
    		throw new IOException("Could not find data descriptor "+descriptorID+" in session "+sessionID);
		if ((flags & DataRequestMessage.kReleaseDescriptor) != 0)
			session.removeDataDescriptor(descriptorID);
    	return returnDescriptor;
    }
    
	protected synchronized DataMoverSession getSession(DataMoverSessionID sessionID)
	{
		DataMoverSession openSession;
		openSession = sessions.get(sessionID);
		return openSession;
	}
    
    void abortConnection(InputStream inStreamToAbort, OutputStream outStreamToAbort) throws IOException
    {
        inStreamToAbort.close();
        outStreamToAbort.close();
    }

    public synchronized DataMoverSession createDataMoverSession(EntityID securityServerID)
    {
        if (securityServerID == null)
            throw new IllegalArgumentException("securityServerID cannot be null");
        DataMoverSession newSession ;
        DataMoverSessionID sessionID = (DataMoverSessionID) oidFactory.getNewOID(DataMoverSession.class);
        newSession = new DataMoverSession(sessionID, securityServerID, this, oidFactory);
        sessions.put(sessionID, newSession);
        logger.debug(new DebugLogMessage("Created session {0}", sessionID));
        return newSession;
    }

    protected synchronized void closeSession(DataMoverSession dataMoverSession)
    {
    	logger.debug(new DebugLogMessage("Closed session {0}", dataMoverSession.getSessionID()));
        sessions.remove(dataMoverSession.getSessionID());
    }

    public InetSocketAddress [] getListenNetworkAddresses(EntityID securityServerID)
    {
    	if (advertiseAddresses != null)
    		return advertiseAddresses;
    	return tcpServer.getListenAddresses(new AddressFilter()
		{
			
			@Override
			public boolean add(InetSocketAddress checkAddress)
			{
				return (!(checkAddress instanceof AFUNIXSocketAddress));
			}
		});
    }
    
    public File getLocalSocket(EntityID securityServerID)
    {
    	InetSocketAddress [] localAddress = afUnixServer.getListenAddresses(new AddressFilter()
		{
			
			@Override
			public boolean add(InetSocketAddress checkAddress)
			{
				return (checkAddress instanceof AFUNIXSocketAddress);
			}
		});
    	if (localAddress != null && localAddress.length > 0)
    		return new File(((AFUNIXSocketAddress)localAddress[0]).getSocketFile());
		return null;
    }
    
    public void openReverseConnection(EntityID securityServerID, InetAddress connectHost, int connectPort) throws IOException, AuthenticationFailureException
    {
    	InetSocketAddress initiateAddress = new InetSocketAddress(connectHost, connectPort);
    	AuthenticatedInitiatorSSLSetup sslSetup = new AuthenticatedInitiatorSSLSetup();
    	FirehoseInitiator.initiateServer(initiateAddress, tcpServer, sslSetup);
    }

	public void serverShutdown(DataMoverServer server)
	{
		server = null;
	}
	
	public String dump()
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("Data Mover Source:\n");
		if (advertiseAddresses != null)
		{
			returnBuffer.append("Advertise addresses: ");
			for (InetSocketAddress curAddress:advertiseAddresses)
			{
				returnBuffer.append(curAddress.toString());
				returnBuffer.append(", ");
			}
			returnBuffer.append("\n");
		}
		returnBuffer.append("Sessions:\n");
		DataMoverSession [] sessionInfo;
		synchronized (sessions)
		{
			sessionInfo = sessions.values().toArray(new DataMoverSession[0]);
		}
		for (DataMoverSession curSessionInfo:sessionInfo)
		{
			returnBuffer.append(curSessionInfo.dump());
			returnBuffer.append("\n");
		}
		returnBuffer.append("TCP server\n");
		returnBuffer.append(tcpServer.dump());
		returnBuffer.append("AFUnix server\n");
		returnBuffer.append(afUnixServer.dump());
		return returnBuffer.toString();
	}
}
