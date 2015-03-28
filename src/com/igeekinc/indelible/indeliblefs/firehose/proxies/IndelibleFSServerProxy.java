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
 
package com.igeekinc.indelible.indeliblefs.firehose.proxies;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSServer;
import com.igeekinc.indelible.indeliblefs.IndelibleServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASServerProxy;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSServerProxy implements IndelibleFSServer
{
	private IndelibleFSFirehoseClient wrappedServer;
	private ArrayList<IndelibleServerConnectionIF>connections = new ArrayList<IndelibleServerConnectionIF>();
	private CASServerFirehoseClient	casServerClient;
	private CASServerProxy casServerProxy;
	private EntityID serverID = null;
	
	public IndelibleFSServerProxy(IndelibleFSFirehoseClient wrappedServer)
	{
		this.setWrappedServer(wrappedServer);
	}

	public synchronized IndelibleServerConnectionIF open() throws IOException, PermissionDeniedException, AuthenticationFailureException
	{
		IndelibleServerConnectionIF returnConnection = new IndelibleFSServerConnectionProxy(getWrappedServer(), getServerAddress());
		connections.add(returnConnection);
		return returnConnection;
	}
	
	protected synchronized void removeConnection(IndelibleFSServerConnectionProxy removeConnection)
	{
		connections.remove(removeConnection);
	}

	public EntityID getServerID() throws IOException
	{
		if (serverID == null)
		{
			ComboFutureBase<EntityID>getServerFuture = new ComboFutureBase<EntityID>();
			getWrappedServer().getServerIDAsync(getServerFuture, null);
			try
			{
				serverID = getServerFuture.get();
			} catch (InterruptedException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new IOException("getServerID timed out");
			} catch (ExecutionException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				if (e.getCause() instanceof IOException)
					throw ((IOException)e.getCause());
				throw new IOException("Got an unexpected error");
			}
		}
		return serverID;
	}

	public EntityID getSecurityServerID() throws IOException
	{
		return getWrappedServer().getConnectionAuthenticationServerID();
	}

	public InetAddress getServerAddress() throws IOException
	{
		SocketAddress serverSocketAddr = getWrappedServer().getServerAddress();
		return ((InetSocketAddress)serverSocketAddr).getAddress();
	}

	public int getServerPort() throws IOException
	{
		SocketAddress serverSocketAddr = getWrappedServer().getServerAddress();
		return ((InetSocketAddress)serverSocketAddr).getPort();
	}
	
	@Override
	public CASServerConnectionIF openCASServer() throws IOException, PermissionDeniedException
	{
		if (casServerClient == null)
		{
			ComboFutureBase<Integer>getCASServerFuture = new ComboFutureBase<Integer>();
			getWrappedServer().getCASServerPort(getCASServerFuture, null);
			try
			{
				int casServerPort = getCASServerFuture.get();
				SocketAddress casServerAddress = new InetSocketAddress(getServerAddress(), casServerPort);

				casServerClient = new CASServerFirehoseClient(casServerAddress);
				casServerProxy = new CASServerProxy(casServerClient);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			} catch (ExecutionException e)
			{
				// TODO Auto-generated catch block
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
		return casServerProxy.open();
	}
	
	public String toString()
	{
		try
		{
			return "IndelibleFSServerProxy "+getServerID()+" "+getServerAddress()+":"+getServerPort();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		return "IndelibleFSServerProxy - could not get additional info";
	}

	@Override
	public boolean isClosed()
	{
		return false;
	}

	private IndelibleFSFirehoseClient getWrappedServer()
	{
		if (wrappedServer.isClosed())
		{
			try
			{
				SocketAddress serverSocketAddr = wrappedServer.getServerAddress();
				EntityID oldServerID = wrappedServer.getServerEntityID();
				IndelibleFSFirehoseClient newServer = new IndelibleFSFirehoseClient(serverSocketAddr);
				if (oldServerID != null && oldServerID.equals(newServer.getServerEntityID()))
					wrappedServer = newServer;
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
		return wrappedServer;
	}

	private void setWrappedServer(IndelibleFSFirehoseClient wrappedServer)
	{
		this.wrappedServer = wrappedServer;
	}
}
