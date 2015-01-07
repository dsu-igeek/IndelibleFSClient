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
 
package com.igeekinc.indelible.indeliblefs.firehose;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Timer;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSServer;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerProxy;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClientListener;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerAppearedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerDisappearedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerTrustedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerUntrustedEvent;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.IndelibleFSClientOIDs;
import com.igeekinc.util.CheckCorrectDispatchThread;
import com.igeekinc.util.MonitoredProperties;
import com.igeekinc.util.OSType;
import com.igeekinc.util.SystemInfo;
import com.igeekinc.util.logging.DebugLogMessage;
import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.logging.InfoLogMessage;

/**
 * Finds IndelibleFSServers on the network and makes them available
 * @author David L. Smith-Uchida
 */

public abstract class IndelibleFSClient 
{
	private static boolean started = false;
	private static IndelibleFSClient singleton;

	private IndelibleFSServerList serverList;
	protected ArrayList<InetSocketAddress>allServerList;	// All of the servers we know about including those we couldn't connect to for authentication reasons
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
					className = "com.igeekinc.indelible.indeliblefs.firehose.windows.IndelibleFSClientWindows"; //$NON-NLS-1$
				}

				if (SystemInfo.getSystemInfo().getOSType() == OSType.kMacOSX) //$NON-NLS-1$
				{
					className = "com.igeekinc.indelible.indeliblefs.firehose.macosx.IndelibleFSClientMacOSX"; //$NON-NLS-1$
				}

				if (SystemInfo.getSystemInfo().getOSType() ==  OSType.kLinux) //$NON-NLS-1$
				{
					className = "com.igeekinc.indelible.indeliblefs.firehose.linux.IndelibleFSClientLinux";	//$NON-NLS-1$
				}
				if (className == null)
					throw new InternalError("System type "+osName+" is unknown"); //$NON-NLS-1$ //$NON-NLS-2$
				try
				{
					@SuppressWarnings("unchecked")
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
			Throwable lastThrowable = null;
			while(true)
			{
				try
				{
					connectToServer(hostStr, port);
					return;
				} catch (Throwable e)
				{
					if (lastThrowable == null || !lastThrowable.getClass().equals(e.getClass()))
						Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception connecting to server {0}", new Serializable[]{hostStr+":" + port}), e);
					lastThrowable = e;
				}
				try
				{
					Thread.sleep(1000);
				} catch (InterruptedException e)
				{
					Logger.getLogger(getClass()).debug(new DebugLogMessage("Caught exception"), e);
				}
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
		allServerList = new ArrayList<InetSocketAddress>();
		started = true;
		EntityAuthenticationClient.getEntityAuthenticationClient().addEntityAuthenticationClientListener(new EntityAuthenticationClientListener() {
			public void entityAuthenticationServerUntrusted(
					EntityAuthenticationServerUntrustedEvent untrustedEvent)
			{
				// TODO Auto-generated method stub
			}

			public void entityAuthenticationServerTrusted(EntityAuthenticationServerTrustedEvent trustedEvent)
			{
				IndelibleFSClient.this.securityServerTrustedInternal(trustedEvent);
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

	public static IndelibleFSServer[] listServers()
	{
		return getSingleton().listServersInternal();
	}

	private IndelibleFSServer[] listServersInternal()
	{
		if (!started)
			throw new IllegalArgumentException("IndelibleFSClient.start must be called first");
		IndelibleFSServer[] returnList;

		returnList = new IndelibleFSServerProxy[serverList.size()];
		returnList = serverList.toArray(returnList);
		return returnList;
	}

	public static void addServer(IndelibleFSFirehoseClient addServer) throws AccessException, AuthenticationFailureException
	{
		getSingleton().addServerInternal(addServer);
	}

	public static IndelibleFSServer connectToServer(String hostname, int port) throws IOException, AuthenticationFailureException
	{
		return getSingleton().connectToServerInternal(hostname, port);
	}

	protected synchronized IndelibleFSServer addServerInternal(IndelibleFSFirehoseClient addServer) throws AuthenticationFailureException
	{
		IndelibleFSServerProxy addServerProxy = new IndelibleFSServerProxy(addServer);
		for (IndelibleFSServer checkServer:serverList)
		{
			try
			{
				EntityID addServerID = addServerProxy.getServerID();
				if (checkServer.getServerID().equals(addServerID))
				{
					addServer.close();
					return checkServer;
				}
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
		serverList.add(addServerProxy);
		return addServerProxy;
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

	protected void securityServerTrustedInternal(EntityAuthenticationServerTrustedEvent trustedEvent)
	{
		try
		{
			EntityID trustedServerID = trustedEvent.getTrustedServer().getEntityID();
			InetSocketAddress [] allServers = allServerList.toArray(new InetSocketAddress[allServerList.size()]);
			for (InetSocketAddress checkServer:allServers)
			{
				try
				{
					IndelibleFSFirehoseClient newTrustedServer = new IndelibleFSFirehoseClient(checkServer);
					addServerInternal(newTrustedServer);
				} catch (Throwable e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Could not connect to IndelibleFS server at {0}", new Serializable[]{checkServer.toString()}), e);
				} 
			}
		} catch (RemoteException e)
		{
			Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
		}
	}


	private IndelibleFSServer connectToServerInternal(String hostname, int port) throws IOException, AuthenticationFailureException
	{
		logger.info(new InfoLogMessage("Adding server {0}:{1}", new Serializable[]{hostname, port}));
		InetSocketAddress connectAddress = new InetSocketAddress(hostname, port);
		if (!allServerList.contains(connectAddress))
			allServerList.add(connectAddress);

		IndelibleFSFirehoseClient addServer = new IndelibleFSFirehoseClient(connectAddress);

		EntityID authenticationServerID = addServer.getConnectionAuthenticationServerID();
		IndelibleFSServer addProxy = null;
		if (EntityAuthenticationClient.getEntityAuthenticationClient().isTrusted(authenticationServerID))
		{
			addProxy = addServerInternal(addServer);
			Logger.getLogger(IndelibleFSClient.class).error("Adding server for "+hostname + " port = "+port+" for AuthenticationServer "+authenticationServerID);
		}
		else
		{
			Logger.getLogger(IndelibleFSClient.class).error("Server "+hostname+" port = "+port+" not authenticated - skipping");
		}

		return addProxy;
	}
}
