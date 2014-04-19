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
 
package com.igeekinc.indelible.indeliblefs;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.proxies.IndelibleFSServerProxy;
import com.igeekinc.indelible.indeliblefs.security.AuthenticatedServer;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClientListener;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerAppearedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerDisappearedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerTrustedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerUntrustedEvent;
import com.igeekinc.indelible.indeliblefs.server.IndelibleFSServerRemote;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.IndelibleFSClientOIDs;
import com.igeekinc.util.CheckCorrectDispatchThread;
import com.igeekinc.util.MonitoredProperties;
import com.igeekinc.util.OSType;
import com.igeekinc.util.SystemInfo;
import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.logging.InfoLogMessage;

/**
 * Finds IndelibleFSServers on the network and makes them available
 * @author David L. Smith-Uchida
 */

class ConnectToServerTimerTask extends TimerTask
{
	IndelibleFSClient client;
	String hostname;
	int port;
	
	public ConnectToServerTimerTask(IndelibleFSClient client, String hostname, int port)
	{
		this.client = client;
		this.hostname = hostname;
		this.port = port;
	}
	
	@Override
	public void run()
	{
		try
		{
			client.connectToServer(hostname, port);
		} catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NotBoundException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
}
public abstract class IndelibleFSClient 
{
    private static boolean started = false;
    private static IndelibleFSClient singleton;
    
    private CheckCorrectDispatchThread threadChecker;
    private IndelibleFSServerList serverList;
    protected ArrayList<AuthenticatedServer<IndelibleFSServerRemote>>allServerList;
    protected MonitoredProperties properties;
    protected Timer retryTimer = new Timer("IndelibleFSClient Server Connect Retry", true);
    protected Logger logger = Logger.getLogger(getClass());
	public static final String kIndelibleEntityAuthenticationClientConfigFileName = "entityAuthenticationClientInfo";
	public static final String kIndelibleCASServerRMIName = "IndelibleCASServer";
	public static final String kIndelibleFSServerRMIName = "IndelibleFSServer";
	static
	{
		IndelibleFSClientOIDs.initMappings();
	}
	
    protected static IndelibleFSClient getSingleton()
	{
		return singleton;
	}

	protected static void setSingleton(IndelibleFSClient singleton)
	{
		IndelibleFSClient.singleton = singleton;
	}

	public static void start(CheckCorrectDispatchThread threadChecker, MonitoredProperties properties)
    {
    	if (!started)
    	{

    		try
    		{
    			String  osName = System.getProperty("os.name"); //$NON-NLS-1$
    			String className = null;
    			if (SystemInfo.getSystemInfo().getOSType() == OSType.kWindows) //$NON-NLS-1$
    			{
    				className = "com.igeekinc.indelible.indeliblefs.windows.IndelibleFSClientWindows"; //$NON-NLS-1$
    			}

    			if (SystemInfo.getSystemInfo().getOSType() == OSType.kMacOSX) //$NON-NLS-1$
    			{
    				className = "com.igeekinc.indelible.indeliblefs.macosx.IndelibleFSClientMacOSX"; //$NON-NLS-1$
    			}

    			if (SystemInfo.getSystemInfo().getOSType() ==  OSType.kLinux) //$NON-NLS-1$
    			{
    				className = "com.igeekinc.indelible.indeliblefs.linux.IndelibleFSClientLinux";	//$NON-NLS-1$
    			}
    			if (className == null)
    				throw new InternalError("System type "+osName+" is unknown"); //$NON-NLS-1$ //$NON-NLS-2$
    			try
    			{
    				Class<? extends IndelibleFSClient> fsClientClass = (Class<? extends IndelibleFSClient>) Class.forName(className);

    				Class<?> [] constructorArgClasses = {CheckCorrectDispatchThread.class};
    				Constructor<? extends IndelibleFSClient> fsClientConstructor = fsClientClass.getConstructor(constructorArgClasses);
    				Object [] constructorArgs = {threadChecker};
    				setSingleton(fsClientConstructor.newInstance(constructorArgs));
    				getSingleton().setProperties(properties);
    				getSingleton().initializeBonjour();
    			}
    			catch (Exception e)
    			{
    				e.printStackTrace();
    				Logger.getLogger(IndelibleFSClient.class).error("Caught exception creating IndelibleFSClient", e); //$NON-NLS-1$
    				throw new InternalError("Caught exception creating IndelibleFSClient"); //$NON-NLS-1$
    			}
    		} 
    		catch (Exception e)
            {
            	Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
            }
    	}
    }
	
	class ConnectToServerRunnable implements Runnable
	{
		private String hostStr;
		private int port;
		
		public ConnectToServerRunnable(String hostStr, int port)
		{
			this.hostStr = hostStr;
			this.port = port;
		}
		
		@Override
		public void run()
		{
			try
			{
				connectToServer(hostStr, port);
			} catch (RemoteException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception connecting to server {0}", new Serializable[]{hostStr+":" + port}), e);
			} catch (NotBoundException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception connecting to server {0}",new Serializable[]{hostStr+":" + port}), e);
			}
		}
	}
    
    private void setProperties(MonitoredProperties properties)
	{
		this.properties = properties;
		if (properties != null)
		{
			String listedServers = properties.getProperty("com.igeekinc.indeliblefs.client.servers");
			if (listedServers != null)
			{
				StringTokenizer tokenizer = new StringTokenizer(listedServers, ",");
				while (tokenizer.hasMoreElements())
				{
					String curHost = tokenizer.nextToken();
					String hostStr = curHost;
					int port = 0;
					try
					{
						int colonPos = hostStr.indexOf(":");
						if (colonPos > 0)
						{
							String portStr = hostStr.substring(colonPos + 1);
							port = Integer.parseInt(portStr);
							hostStr = hostStr.substring(0, colonPos);
						}
					} catch (Throwable e)
					{
						Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception parsing Indelible FS server {0}", new Serializable[]{curHost}));
					}
					Thread connectThread = new Thread(new ConnectToServerRunnable(hostStr, port), "Connect to "+curHost);
					connectThread.setDaemon(true);
					connectThread.start();
				}
			}
		}
	}

	protected IndelibleFSClient(CheckCorrectDispatchThread threadChecker) throws Exception
    {
    	serverList = new IndelibleFSServerList(threadChecker);
    	allServerList = new ArrayList<AuthenticatedServer<IndelibleFSServerRemote>>();
        started = true;
        EntityAuthenticationClient.getEntityAuthenticationClient().addEntityAuthenticationClientListener(new EntityAuthenticationClientListener() {
            public void entityAuthenticationServerUntrusted(
                    EntityAuthenticationServerUntrustedEvent untrustedEvent)
            {
                // TODO Auto-generated method stub
            }
            
            public void entityAuthenticationServerTrusted(EntityAuthenticationServerTrustedEvent trustedEvent)
            {
                IndelibleFSClient.securityServerTrusted(trustedEvent);
            }
            
            public void entityAuthenticationServerDisappeared(
                    EntityAuthenticationServerDisappearedEvent removedEvent)
            {
                // TODO Auto-generated method stub
            }
            
            public void entityAuthenticationServerAppeared(EntityAuthenticationServerAppearedEvent addedEvent)
            {
                // TODO Auto-generated method stub
            }
        });
    }
    
    protected abstract void initializeBonjour() throws Exception;
    
    public static IndelibleFSServerProxy[] listServers()
    {
    	return getSingleton().listServersInternal();
    }
    
    private IndelibleFSServerProxy[] listServersInternal()
    {
        if (!started)
            throw new IllegalArgumentException("IndelibleFSClient.start must be called first");
        IndelibleFSServerProxy[] returnList;

        returnList = new IndelibleFSServerProxy[serverList.size()];
        returnList = serverList.toArray(returnList);
        return returnList;
    }

    public static void addServer(IndelibleFSServerRemote addServer) throws RemoteException, AccessException
    {
    	getSingleton().addServerInternal(addServer);
    }
    
    public static void connectToServer(String hostname, int port) throws RemoteException, NotBoundException
    {
    	getSingleton().connectToServerInternal(hostname, port);
    }
    
    private void connectToServerInternal(String hostname, int port)
	{
    	try
    	{
    		Registry locateRegistry = LocateRegistry.getRegistry(hostname, port);
    		Logger.getLogger(IndelibleFSClient.class).error("Adding specified registry "+hostname + " port = "+port);
    		addRegistry(hostname, port, locateRegistry);
    	}
    	catch (RemoteException e)
    	{
    		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    		retryTimer.schedule(new ConnectToServerTimerTask(this, hostname, port), 30000L);
    		
    	} catch (NotBoundException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);    		
    		retryTimer.schedule(new ConnectToServerTimerTask(this, hostname, port), 30000L);
		}
	}

	protected synchronized void addServerInternal(IndelibleFSServerRemote addServer) throws RemoteException, AccessException
    {
    	boolean serverPresent = false;
    	for (IndelibleFSServerProxy checkServer:serverList)
    	{
    		try
			{
				if (checkServer.getServerID().equals(addServer.getServerID()))
				{
					serverPresent = true;
					break;
				}
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
    	}
    	if (!serverPresent)
    	{
    		serverList.add(new IndelibleFSServerProxy(addServer));
    	}
    }
    
    public static void addIndelibleFSServerListListener(IndelibleFSServerListListener listener)
    {
    	getSingleton().addIndelibleFSServerListListenerInternal(listener);
    }
    
    protected void addIndelibleFSServerListListenerInternal(IndelibleFSServerListListener listener)
    {
        serverList.addIndelibleFSServerListListener(listener);
    }
    
    public static void removeIndelibleFSServerListListener(IndelibleFSServerListListener listener)
    {
    	getSingleton().removeIndelibleFSServerListListenerInternal(listener);
    }
    
    protected void removeIndelibleFSServerListListenerInternal(IndelibleFSServerListListener listener)
    {
        serverList.removeIndelibleFSServerListListener(listener);
    }
    
    public static void securityServerTrusted(EntityAuthenticationServerTrustedEvent trustedEvent)
    {
    	getSingleton().securityServerTrustedInternal(trustedEvent);
    }
    
    protected void securityServerTrustedInternal(EntityAuthenticationServerTrustedEvent trustedEvent)
    {
        try
        {
            EntityID trustedServerID = trustedEvent.getAddedServer().getEntityID();
            for (AuthenticatedServer<IndelibleFSServerRemote>checkServer:allServerList)
            {
            	IndelibleFSServerRemote newTrustedServer;
            	if ((newTrustedServer = checkServer.getServerInstanceForAuthenticationServer(trustedServerID)) != null)
            	{
            		addServerInternal(newTrustedServer);
            	}
            }
        } catch (RemoteException e)
        {
            Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
        }
    }

	protected IndelibleFSServerRemote [] addRegistry(String hostname, int port, Registry addRegistry)
			throws RemoteException, NotBoundException, AccessException
	{
		ArrayList<IndelibleFSServerRemote>returnServersList = new ArrayList<IndelibleFSServerRemote>();
		String [] bound = addRegistry.list();
		logger.info(new InfoLogMessage("Adding registry {0}:{1}", new Serializable[]{hostname, port}));
		for (String curBound:bound)
		{
			logger.info(new InfoLogMessage(curBound));
		}
		AuthenticatedServer<IndelibleFSServerRemote>server = (AuthenticatedServer<IndelibleFSServerRemote>) addRegistry.lookup(kIndelibleFSServerRMIName);
		allServerList.add(server);
		for (EntityID authenticationServerID:server.listAuthenticationServers())
		{
			if (EntityAuthenticationClient.getEntityAuthenticationClient().isTrusted(authenticationServerID))
			{
				IndelibleFSServerRemote addServer = server.getServerInstanceForAuthenticationServer(authenticationServerID);
				addServerInternal(addServer);
				returnServersList.add(addServer);
				Logger.getLogger(IndelibleFSClient.class).error("Adding server for "+hostname + " port = "+port+" for AuthenticationServer "+authenticationServerID);
			}
			else
			{
				Logger.getLogger(IndelibleFSClient.class).error("Server "+hostname+" port = "+port+" not authenticated - skipping");
			}
		}
		IndelibleFSServerRemote [] returnServers = new IndelibleFSServerRemote[returnServersList.size()];
		returnServers = returnServersList.toArray(returnServers);
		return returnServers;
	}
    
}
