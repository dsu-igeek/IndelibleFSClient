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
 
package com.igeekinc.indelible.indeliblefs.proxies;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverReceiver;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSource;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.NoMoverPathException;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession.DataDescriptorAvailability;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.server.IndelibleFSServerConnectionRemote;
import com.igeekinc.indelible.indeliblefs.server.IndelibleFSServerRemote;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.DebugLogMessage;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSServerConnectionProxy implements IndelibleServerConnectionIF
{
	private IndelibleFSServerConnectionRemote wrappedConnection;
	private DataMoverSession moverSession;
	private ArrayList<IndelibleFSObjectRemote>releaseList = new ArrayList<IndelibleFSObjectRemote>();
	private Logger logger = Logger.getLogger(getClass());
	public IndelibleFSServerConnectionProxy(IndelibleFSServerRemote wrappedServer, InetAddress preferredServerAddress) throws IOException
	{
		this.wrappedConnection = wrappedServer.open();
        EntityAuthentication serverAuthentication = wrappedConnection.getServerEntityAuthentication();
        EntityID securityServerID = wrappedServer.getSecurityServerID();
		moverSession = DataMoverSource.getDataMoverSource().createDataMoverSession(securityServerID);
        try
		{
        	/*
        	 * Authorize the server to read data from us
        	 */
			SessionAuthentication sessionAuthentication = moverSession.addAuthorizedClient(serverAuthentication);
			wrappedConnection.addClientSessionAuthentication(sessionAuthentication);
			
			/*
			 * Now, get the authentication that allows us to read data from the server
			 */
			SessionAuthentication remoteAuthentication = wrappedConnection.getSessionAuthentication();
			DataMoverReceiver.getDataMoverReceiver().addSessionAuthentication(remoteAuthentication);
			
			/*
			 * Ideally, the mover would simply establish connections on demand.  However, things like firewalls
			 * and/or NAT can interfere with our ability to establish a TCP connection.  Furthermore, the way
			 * firewalls are typically setup today, the connection is not quickly rejected but instead must
			 * timeout.  So, we try the local connection first.  If that fails, then we try establishing a reverse connection
			 * because if we're in a NAT situation, we are probably on the NAT side (since we're the client) and should
			 * be able to establish a connection.  If the reverse connection fails, then just to be certain there is no
			 * connectivity, we try to estable a normal connection.
			 */
			byte [] testBuffer = new byte[32*1024];
			CASIDDataDescriptor testDescriptor = new CASIDMemoryDataDescriptor(testBuffer);
			NetworkDataDescriptor testNetworkDescriptor = moverSession.registerDataDescriptor(testDescriptor, DataDescriptorAvailability.kLocalOnlyAccess);
			try
			{
				logger.error(new ErrorLogMessage("Attempting to establish local connection"));
				wrappedServer.testReverseConnection(testNetworkDescriptor);
				logger.error(new ErrorLogMessage("Local connection succeeded"));
			}
			catch (NoMoverPathException e)
			{
				logger.debug(new DebugLogMessage("No local connection, trying reverse network connection first"));
				InetSocketAddress [] moverAddresses = wrappedServer.getMoverAddresses(securityServerID);
				if (moverAddresses.length == 0)
					throw e;
				try
				{
					logger.error(new ErrorLogMessage("Attempting to establish reverse connection"));
					DataMoverSource.getDataMoverSource().openReverseConnection(securityServerID, preferredServerAddress, moverAddresses[0].getPort());
					wrappedServer.testReverseConnection(testNetworkDescriptor);
					logger.error(new ErrorLogMessage("Reverse connection succeeded"));
				} catch (Throwable t)
				{
					logger.error(new ErrorLogMessage("Reverse connection failed, trying forward connection"));
					testNetworkDescriptor = moverSession.registerDataDescriptor(testDescriptor, DataDescriptorAvailability.kAllAccess);
					wrappedServer.testReverseConnection(testNetworkDescriptor);
				}
			}
		} catch (Throwable t)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), t);
			throw new InternalError("Could not authorize client to mover");
		}
	}
	
	@Override
	public IndelibleFSVolumeIF createVolume(Properties volumeProperties)
			throws IOException, PermissionDeniedException
	{
		return new IndelibleFSVolumeProxy(this, wrappedConnection.createVolume(volumeProperties));
	}

	@Override
	public IndelibleFSVolumeIF retrieveVolume(
			IndelibleFSObjectID retrieveVolumeID)
			throws VolumeNotFoundException, IOException
	{
		return new IndelibleFSVolumeProxy(this, wrappedConnection.retrieveVolume(retrieveVolumeID));
	}

	@Override
	public IndelibleFSObjectID[] listVolumes() throws IOException
	{
		return wrappedConnection.listVolumes();
	}

	@Override
	public void startTransaction() throws IOException
	{
		wrappedConnection.startTransaction();
	}

	@Override
	public boolean inTransaction() throws IOException
	{
		return wrappedConnection.inTransaction();
	}

	@Override
	public IndelibleVersion commit() throws IOException
	{
		return wrappedConnection.commit();
	}

	@Override
	public IndelibleVersion commitAndSnapshot(HashMap<String, Serializable>snapshotMetadata) throws IOException, PermissionDeniedException
	{
		return wrappedConnection.commitAndSnapshot(snapshotMetadata);
	}

	@Override
	public void rollback() throws IOException
	{
		wrappedConnection.rollback();
	}

	@Override
	public void close() throws IOException
	{
		wrappedConnection.close();
	}

	@Override
	public EntityAuthentication getClientEntityAuthentication() throws IOException
	{
		return wrappedConnection.getClientEntityAuthentication();
	}

	@Override
	public EntityAuthentication getServerEntityAuthentication() throws IOException
	{
		return wrappedConnection.getServerEntityAuthentication();
	}
	
	@Override
	public void addClientSessionAuthentication(SessionAuthentication sessionAuthentication) throws IOException
	{
		wrappedConnection.addClientSessionAuthentication(sessionAuthentication);
	}

	@Override
	public SessionAuthentication getSessionAuthentication() throws IOException
	{
		return wrappedConnection.getSessionAuthentication();
	}

	@Override
	public NetworkDataDescriptor registerDataDescriptor(DataDescriptor localDataDescriptor)
	{
		return moverSession.registerDataDescriptor(localDataDescriptor);
	}
	
	@Override
	public NetworkDataDescriptor registerNetworkDescriptor(byte [] bytes)
	{
		return registerNetworkDescriptor(bytes, 0, bytes.length);
	}
	
	@Override
	public NetworkDataDescriptor registerNetworkDescriptor(byte [] bytes, int offset, int length)
	{
		return registerNetworkDescriptor(ByteBuffer.wrap(bytes, offset, length));
	}
	
	@Override
	public NetworkDataDescriptor registerNetworkDescriptor(ByteBuffer byteBuffer)
	{
		CASIDMemoryDataDescriptor baseDescriptor = new CASIDMemoryDataDescriptor(byteBuffer);
		return moverSession.registerDataDescriptor(baseDescriptor);
	}
	
	@Override
	public void removeDataDescriptor(DataDescriptor writeDescriptor)
	{
		moverSession.removeDataDescriptor(writeDescriptor);
	}
	
	@Override
	public DataMoverSession getMoverSession()
	{
		return moverSession;
	}
	
	/**
	 * The connection on the server sides maintains hard references to all objects so that long running or
	 * stalled transactions don't result in garbage collected objects.  We release the objects here when they
	 * get finalized on our side
	 * @param releaseProxy
	 */
	void proxyFinalized(IndelibleFSObjectProxy releaseProxy)
	{
		IndelibleFSObjectRemote [] releaseArray = null;
		synchronized(releaseList)
		{
			releaseList.add(releaseProxy.getRemote());
			if (releaseList.size() > 32)
			{
				releaseArray = new IndelibleFSObjectRemote[releaseList.size()];
				releaseArray = releaseList.toArray(releaseArray);
				releaseList.clear();
			}
		}
		if (releaseArray != null)
		{
			try
			{
				for (IndelibleFSObjectRemote releaseObject:releaseArray)
				{
					if (releaseObject != null)
						releaseObject.release();
				}
			} catch (Throwable e)
			{
				// TODO Auto-generated catch block
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
	}
}
