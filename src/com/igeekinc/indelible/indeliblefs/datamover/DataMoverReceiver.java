/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igeekinc.indelible.indeliblefs.datamover;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.perf4j.log4j.Log4JStopWatch;

import com.igeekinc.firehose.FirehoseChannel;
import com.igeekinc.firehose.ReverseClientManager;
import com.igeekinc.firehose.SSLFirehoseChannel;
import com.igeekinc.indelible.indeliblefs.security.AuthenticatedTargetSSLSetup;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.EthernetID;
import com.igeekinc.util.InterfaceAddressInfo;
import com.igeekinc.util.SystemInfo;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

public class DataMoverReceiver extends DataMover implements ReverseClientManager
{
    private static final int kWriteToStreamBufferSize = 16*1024;
    private static final int kWriteToChannelBufferSize = 1024*1024;
	
	private static DataMoverReceiver singleton;
	private InetAddress [] localAddresses;
	protected HashMap<EntityID, DataMoverServerInfo>servers = new HashMap<EntityID, DataMoverServerInfo>();
	protected HashMap<DataMoverSessionID, DataMoverSessionInfo>sessions = new HashMap<DataMoverSessionID, DataMoverSessionInfo>();
	protected HashMap<DataMoverSessionID, SessionAuthentication> sessionAuthentications = new HashMap<DataMoverSessionID, SessionAuthentication>();

	public static DataMoverReceiver getDataMoverReceiver()
	{
		if (singleton == null)
			throw new InternalError("DataMoverReceiver not initialized");
		return singleton;
	}

	public static synchronized void init(ObjectIDFactory oidFactory)
	{
		if (singleton == null)
			singleton = new DataMoverReceiver(oidFactory);
		else
			throw new IllegalArgumentException("DataMoverReceiver already initialized!");
	}

	/**
	 * This will render the DataMoverReceiver unusable until init() is called again.  Mostly useful for testing.
	 */
	public static void shutdown()
	{
		singleton = null;
	}

	private DataMoverReceiver(ObjectIDFactory oidFactory)
	{
		super(oidFactory);
		ArrayList<InetAddress>localAddressList = new ArrayList<InetAddress>();
		try
		{

			Enumeration<NetworkInterface>interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements())
			{
				NetworkInterface curInterface = interfaces.nextElement();
				Enumeration<InetAddress>curAddresses = curInterface.getInetAddresses();
				while (curAddresses.hasMoreElements())
				{
					localAddressList.add(curAddresses.nextElement());
				}
			}
		} catch (SocketException e1)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);

		}
		localAddresses = new InetAddress[localAddressList.size()];
		localAddresses = localAddressList.toArray(localAddresses);
		ethernetID = SystemInfo.getSystemInfo().getEthernetID();
	}

    private int getDataFromDescriptor(NetworkDataDescriptor sourceDescriptor,
			byte[] buffer, int destinationOffset, long srcOffset, int length,
			boolean release) throws IOException, AuthenticationFailureException
	{
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
		byteBuffer.position(destinationOffset);
		if (byteBuffer.remaining() < length)
			throw new IllegalArgumentException("buffer length (" + buffer.length+") - offset ("+destinationOffset+") < requested length ("+length+")");
		return getDataFromDescriptor(sourceDescriptor, byteBuffer, srcOffset, length, release);
	}
    
	public int getDataFromDescriptor(NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, boolean release)
			throws IOException, AuthenticationFailureException
	{
		DataRequestFuture future = getDataFromDescriptorAsync(sourceDescriptor, destination, srcOffset, length, release);
		try
		{
			return future.get();
		}
		catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
        		throw (IOException)e.getCause();
        	if (e.getCause() instanceof AuthenticationFailureException)
        	{
        		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        		throw new IOException("Authentication failed connecting to mover");
        	}
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Got unexpected exception"), e);
        	throw new IOException("Got unexpected exception "+e.getCause().toString());
        } catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Interrupted while waiting for results");
		}
	}

	public DataRequestFuture getDataFromDescriptorAsync(NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, boolean release)
			throws IOException, AuthenticationFailureException
	{
		DataRequestFuture returnFuture = new DataRequestFuture();
		getDataFromDescriptorAsync(sourceDescriptor, destination, srcOffset, length, release, returnFuture);
		return returnFuture;
	}

	public <A> void getDataFromDescriptorAsync(NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, boolean release,
			AsyncCompletion<Integer, ? super A>completionHandler, A attachment)
					throws IOException, AuthenticationFailureException
	{
		DataRequestFuture future = new DataRequestFuture(completionHandler, attachment);
		getDataFromDescriptorAsync(sourceDescriptor, destination, srcOffset, length, release, future);
	}

	public boolean sameHost(NetworkDataDescriptor checkDescriptor)
	{
		return checkDescriptor.getSourceID().equals(ethernetID);
	}

	public static final int kMaxTries = 5;

	private void getDataFromDescriptorAsync(NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, boolean release,
			DataRequestFuture future) throws IOException, AuthenticationFailureException
			{
		Log4JStopWatch getDataWatch = new Log4JStopWatch("DataMoverReceiver.getDataFromDescriptor.total");
		try
		{
			// Network descriptors may also contain a "shareable" descriptor.  This is a descriptor that can be used directly
			// instead of using the mover-to-mover channel.  Small data transfers, for example, will be bundled into a BasicDataDescriptor (memory buffer)
			// that is put inside of the NetworkDataDescriptor.  Network descriptors based on a FileDataDescriptor will get the FileDataDescriptor
			// bundled.  If we're on the same host we can try using the FileDataDescriptor.  The data may not be accessible via the descriptor (for example,
			// the FileDataDescriptor points to a file that this process does not have read authorization for) in which case we will use the mover-to-mover 
			// channel instead
			DataDescriptor shareableDescriptor = sourceDescriptor.getShareableDescriptor();
			if (shareableDescriptor != null)
			{
				if ((sameHost(sourceDescriptor) || shareableDescriptor.isShareableWithRemoteProcess()) && shareableDescriptor.isAccessible())
				{
					try
					{
						int bytesRead = shareableDescriptor.getData(destination, srcOffset, length, release);
						future.completed(bytesRead, future);
					}
					catch (Throwable t)
					{
						future.failed(t, future);
					}
				}
			}
			else
			{
				boolean commandSent = false;
				int timesTried = 0;
				DataMoverSessionInfo sessionInfo = null;

				while (!commandSent && timesTried < kMaxTries)
				{
					try
					{
						timesTried ++;
						Log4JStopWatch getSessionWatch = new Log4JStopWatch("DataMoverReceiver.getDataFromDescriptor.getSession");
						sessionInfo = getSession(sourceDescriptor);
						getSessionWatch.stop();
						byte flags = 0;
						if (release)
							flags |= DataRequestMessage.kReleaseDescriptor;
						Log4JStopWatch receiveDataWatch = new Log4JStopWatch("DataMoverReceiver.getDataFromDescriptor.receiveData");
						sessionInfo.getServerInfo().getDataMoverClient().getDataFromDescriptorAsync(sessionInfo.getSessionID(), sourceDescriptor, destination, srcOffset, length, flags, release, future);
						receiveDataWatch.stop();
						commandSent = true;
					}
					catch (NoMoverPathException e)
					{
						throw e;
					}
					catch(IOException e)
					{
						logger.error(new ErrorLogMessage("Got IOException sending DataMover command"), e);
						if (sessionInfo != null)
							sessionFailed(sessionInfo);
					}
				}
				if (!commandSent)
					throw new IOException("Could not send command");
			}

		}
		finally
		{
			getDataWatch.stop();
		}
			}

	protected DataMoverSessionInfo getSession(NetworkDataDescriptor sourceDescriptor) throws IOException, AuthenticationFailureException
	{
		DataMoverSessionID sessionID = sourceDescriptor.getSessionID();
		DataMoverSessionInfo returnInfo;
		boolean openSession = false;
		synchronized (sessions)
		{
			boolean foundGood = false;
			returnInfo = sessions.get(sessionID);
			if (returnInfo != null)
			{
				DataMoverServerInfo serverInfo = returnInfo.getServerInfo();
				{
					if (serverInfo != null)
					{
						DataMoverClient dataMoverClient = serverInfo.getDataMoverClient();
						if (dataMoverClient != null && !dataMoverClient.isClosed())
							foundGood = true;
					}
				}
			}
			if (!foundGood)
			{
				openSession = true;
				returnInfo = new DataMoverSessionInfo(sessionID);
				sessions.put(sessionID, returnInfo);
			}
		}
		if (openSession)
		{
			boolean keepTrying = false;
			boolean sessionOpenedSuccessfully = false;
			try
			{
				do
				{
					keepTrying = false;
					// OK, need to open the session and possibly a connection to the remote mover
					DataMoverServerInfo serverInfo = getServerInfo(sourceDescriptor);
					SessionAuthentication sessionAuthentication = sessionAuthentications.get(sessionID);
					if (sessionAuthentication == null)
						throw new AuthenticationFailureException("No session authentication registered");
					try
					{
						serverInfo.getDataMoverClient().openSession(sessionID, sessionAuthentication);
						returnInfo.setServerInfo(serverInfo);
						sessionOpenedSuccessfully = true;
					}
					catch (NoMoverPathException e)
					{
						throw e;	// No hope, bail and the client can set up a reverse connection
					}
					catch(IOException e)
					{
						if (!keepTrying)	// If keepTrying is set, we've already been down here
						{

							EntityID dataMoverServerID = sourceDescriptor.getServerID();
							synchronized(servers)
							{
								if (servers.get(dataMoverServerID) == serverInfo)
								{
									logger.error(new ErrorLogMessage("Got IOException connection to {0}, removing from cache and retrying", dataMoverServerID), e);
									servers.remove(dataMoverServerID);
									keepTrying = true;
								}
								else
									throw e;	// Can't recover so throw the IOException
							}
						}
						else
						{
							throw e;	// Already came through here once, bounce out
						}
					}
				} while (keepTrying);
			}
			finally
			{
				if (!sessionOpenedSuccessfully)
				{
					synchronized(sessions)
					{
						sessions.remove(sessionID);
					}
				}
			}
		}
		else
		{
			returnInfo.checkConnection(60 * 1000);
		}
		return returnInfo;
	}

	protected DataMoverServerInfo getServerInfo(NetworkDataDescriptor sourceDescriptor) throws IOException, AuthenticationFailureException
	{
		EntityID dataMoverServerID = sourceDescriptor.getServerID();
		boolean doConnect = false;
		DataMoverServerInfo returnInfo;
		synchronized (servers)
		{
			returnInfo = servers.get(dataMoverServerID);
			if (returnInfo == null || returnInfo.getDataMoverClient() == null || returnInfo.getDataMoverClient().isClosed())
			{
				if (returnInfo != null)
					logger.debug("Recovering from closed client");
				doConnect = true;
				returnInfo = new DataMoverServerInfo(dataMoverServerID);
				servers.put(dataMoverServerID, returnInfo);
			}
		}
		if (!doConnect && !returnInfo.checkConnection(60 * 1000))
			doConnect = true;
		if (doConnect)
		{
			boolean connectedSuccessful = false;
			try
			{
				connectToServerForDescriptor(sourceDescriptor, returnInfo);
				connectedSuccessful = true;
			} finally
			{
				if (!connectedSuccessful)
				{
					synchronized(servers)
					{
						servers.remove(dataMoverServerID);
					}
				}
			}
		}

		return returnInfo;
	}

	protected void connectToServerForDescriptor(NetworkDataDescriptor descriptorFromServer, DataMoverServerInfo fillInInfo) throws IOException, AuthenticationFailureException, NoMoverPathException
	{
		Socket receiveSocket = null;
		if (sameHost(descriptorFromServer))
		{
			try
			{
				// We're on the same machine - let's see if we can talk via a local socket
				File localSocketFile = descriptorFromServer.getLocalSocket();
				if (localSocketFile != null)
				{
					AFUNIXSocketAddress connectAddress = new AFUNIXSocketAddress(localSocketFile);
					try
					{
						DataMoverClient client = new DataMoverClient(connectAddress);
						fillInInfo.setConnection(client);
						return;
					}
					catch (Throwable t)
					{
						logger.error(new ErrorLogMessage("Got error connection to mover at {0}", new Serializable[]{connectAddress}), t);
					}				}
			} catch (Exception e)
			{
				// Hmmmm - well, we'll see if we can connect via the TCP interface
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
		if (receiveSocket == null)
		{
			// Couldn't make a local socket connection for whatever reason
			InetSocketAddress [] connectAddresses = descriptorFromServer.getHostPorts();
			
			InetSocketAddress [] cleanedAddresses = new InetSocketAddress[connectAddresses.length];
			for (int curAddressNum = 0; curAddressNum < connectAddresses.length; curAddressNum++)
			{
				InetSocketAddress tryAddress = connectAddresses[curAddressNum];
				InetAddress connectHost = tryAddress.getAddress();
				if (connectHost == null)	// Other side is not running a connect loop
					throw new NoMoverPathException();
				if (connectHost.equals(InetAddress.getLocalHost()))
					connectHost = InetAddress.getByName("127.0.0.1");	// Certain network configuration do evil routing (packets out and back).  Use the loopback to avoid that
				int connectPort = tryAddress.getPort();
				logger.error(new ErrorLogMessage("Connecting to server at {0} port {1}", new Serializable[]{connectHost.toString(), connectPort}));
				cleanedAddresses[curAddressNum] = new InetSocketAddress(connectHost, connectPort);
			}
			if (cleanedAddresses.length > 0)
			{
				// Order the addresses for same net, then public IP's, etc.
				updateOurAddresses();
				Arrays.sort(cleanedAddresses, new Comparator<InetSocketAddress>()
						{

					@Override
					public int compare(InetSocketAddress o1, InetSocketAddress o2)
					{
						int o1Priority = getSortPriority(o1.getAddress());
						int o2Priority = getSortPriority(o2.getAddress());
						return o2Priority - o1Priority;	// We want the results sorted high to low
					}
						});
				try
				{
					DataMoverClient client = new DataMoverClient(cleanedAddresses);
					fillInInfo.setConnection(client);
					return;
				}
				catch (Throwable t)
				{
					logger.error(new ErrorLogMessage("Got error connection to mover at {0}", new Serializable[]{connectAddresses[0]}), t);
				}
			}
			throw new NoMoverPathException();	// We tried everything
		}
	}
	
	private InterfaceAddressInfo [] ourAddresses;
	
	private void updateOurAddresses() throws SocketException
	{
		ourAddresses = SystemInfo.getSystemInfo().getActiveAddresses();
	}
	
	public static final int kSameNetworkPriority = 50;
	public static final int kPublicIPPriority = 40;
	public static final int kIPV6Priority = -30;	// For now, we give priority to IPV4 addresses
	public static final int kPrivateIPPriority = 1;
	private EthernetID	ethernetID;
	
	/**
	 * Assigns a priority to each address
	 * Same network
	 * Public IP
	 * Private IP
	 * @param address
	 * @return
	 */
	private int getSortPriority(InetAddress address)
	{
		for (InterfaceAddressInfo ourCheckAddress:ourAddresses)
		{
			if (ourCheckAddress.sameNetwork(address))
			{
				if (address instanceof Inet6Address)
					return kSameNetworkPriority + kIPV6Priority;
				return kSameNetworkPriority;
			}
		}
		if (address.isSiteLocalAddress())
		{
			if (address instanceof Inet6Address)
				return kPrivateIPPriority + kIPV6Priority;
			return kPrivateIPPriority;
		}
		if (address instanceof Inet6Address)
			return kPublicIPPriority + kIPV6Priority;
		return kPublicIPPriority;
	}

	private void sessionFailed(DataMoverSessionInfo sessionInfo)
	{
		// TODO Auto-generated method stub

	}
	
	public void writeDescriptorToStream(
            NetworkDataDescriptor sourceDescriptor,
            OutputStream destinationStream) throws IOException, AuthenticationFailureException
    {
        byte [] buffer = new byte[kWriteToStreamBufferSize];
        long offset = 0;
        long bytesToCopy = sourceDescriptor.getLength();
        while (bytesToCopy > 0)
        {
            int bytesToCopyNow = kWriteToStreamBufferSize;
            if (bytesToCopyNow > bytesToCopy)
                bytesToCopyNow = (int)bytesToCopy;
            boolean releaseDescriptor = false;
            if (bytesToCopy == bytesToCopyNow)
            	releaseDescriptor = true;	// At the end, let it go
            int bytesRead = getDataFromDescriptor(sourceDescriptor, buffer, 0, offset, bytesToCopyNow, releaseDescriptor);
            if (releaseDescriptor)
            	sourceDescriptor.setClosed();
            bytesToCopy -= bytesRead;
            if (bytesRead != bytesToCopyNow)
                throw new IOException("Expected to get "+sourceDescriptor.getLength()+" bytes, read only "+(sourceDescriptor.getLength() - bytesToCopy));
            destinationStream.write(buffer, 0, bytesRead);
            offset += bytesRead;
        }
        sourceDescriptor.close();
    }

	public void writeDescriptorToStream(
            NetworkDataDescriptor sourceDescriptor,
            FileOutputStream destinationStream) throws IOException, AuthenticationFailureException
    {
        FileChannel destinationChannel = destinationStream.getChannel();
        long offset = 0;
        long bytesToCopy = sourceDescriptor.getLength();
        while (bytesToCopy > 0)
        {
            int bytesToCopyNow = kWriteToChannelBufferSize;
            if (bytesToCopyNow > bytesToCopy)
                bytesToCopyNow = (int)bytesToCopy;
            boolean releaseDescriptor = false;
            if (bytesToCopy == bytesToCopyNow)
            	releaseDescriptor = true;	// At the end, let it go
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytesToCopyNow);
            int bytesRead = getDataFromDescriptor(sourceDescriptor, writeBuffer, offset, bytesToCopyNow, releaseDescriptor);
            if (releaseDescriptor)
            	sourceDescriptor.setClosed();
            bytesToCopy -= bytesRead;
            if (bytesRead != bytesToCopyNow)
                throw new IOException("Expected to get "+sourceDescriptor.getLength()+" bytes, read only "+(sourceDescriptor.getLength() - bytesToCopy));
            destinationChannel.write(writeBuffer);
            offset += bytesRead;
        }
        sourceDescriptor.close();
    }
	
    /**
     * Registers a SessionAuthentication for use - if a data descriptor triggers the use of the session this authenticates
     * for, the SessionAuthentication will be used to authenticate us to the remote mover.
     * @param sessionAuthenticationToAdd
     */
    public void addSessionAuthentication(SessionAuthentication sessionAuthenticationToAdd)
    {
    	EntityID authenticationEntityID = sessionAuthenticationToAdd.getAuthenticatedEntityID();
		EntityID ourEntityID = EntityAuthenticationClient.getEntityAuthenticationClient().getEntityID();
		if (!authenticationEntityID.equals(ourEntityID))
    		throw new IllegalArgumentException("sessionAuthenticationToAdd does not authenticate this server");
    	synchronized(sessionAuthentications)
    	{
    		sessionAuthentications.put(sessionAuthenticationToAdd.getSessionID(), sessionAuthenticationToAdd);
    	}
    }

	@Override
	public void addClientChannel(FirehoseChannel newTargetChannel) throws IOException
	{
		DataMoverClient reverseClient = new DataMoverClient((AuthenticatedTargetSSLSetup) ((SSLFirehoseChannel)newTargetChannel).getSSLSetup());
		reverseClient.addChannel(newTargetChannel);
		EntityID serverID = reverseClient.getServerEntityID();
		DataMoverServerInfo serverInfo = new DataMoverServerInfo(serverID);
		serverInfo.setConnection(reverseClient);
		synchronized(servers)
		{
			DataMoverServerInfo oldInfo = servers.put(serverID, serverInfo);
			if (oldInfo != null)
			{
				DataMoverClient dataMoverClient = oldInfo.getDataMoverClient();
				if (dataMoverClient != null)
					dataMoverClient.close();
			}
		}
	}

	@Override
	public void shutdownClientManager()
	{
		// TODO Auto-generated method stub
		
	}
	
	public String dump()
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("Data Mover Receiver:\n");
		returnBuffer.append("Local addresses: ");
		for (InetAddress curAddress:localAddresses)
		{
			returnBuffer.append(curAddress.toString());
			returnBuffer.append(", ");
		}
		returnBuffer.append("\n");
		if (ourAddresses != null)
		{
			returnBuffer.append("Our addresses: ");
			for (InterfaceAddressInfo curAddress:ourAddresses)
			{
				returnBuffer.append(curAddress.toString());
				returnBuffer.append(", ");
			}
			returnBuffer.append("\n");
		}
		returnBuffer.append("Sessions:\n");
		Map.Entry<DataMoverSessionID,DataMoverSessionInfo>[] sessionInfo;
		synchronized (sessions)
		{
			sessionInfo = sessions.entrySet().toArray(new Map.Entry[0]);
		}
		for (Map.Entry<DataMoverSessionID,DataMoverSessionInfo>curSessionInfo:sessionInfo)
		{
			returnBuffer.append(curSessionInfo.getKey().toString());
			returnBuffer.append(" = ");
			returnBuffer.append(curSessionInfo.getValue().dump());
			returnBuffer.append("\n");
		}
		return returnBuffer.toString();
	}
}
