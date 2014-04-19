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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.perf4j.log4j.Log4JStopWatch;

import com.igeekinc.indelible.indeliblefs.security.AuthenticatedConnection;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClientListener;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServer;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerAppearedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerDisappearedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerTrustedEvent;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerUntrustedEvent;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.indeliblefs.security.afunix.AFUnixAuthenticatedSocket;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

public class DataMoverReceiver extends DataMover
{
    private static final int kWriteToStreamBufferSize = 16*1024;
    private static final int kWriteToChannelBufferSize = 1024*1024;
    protected HashMap<EntityID, DataMoverServerInfo>servers = new HashMap<EntityID, DataMoverServerInfo>();
    protected HashMap<DataMoverSessionID, DataMoverSessionInfo>sessions = new HashMap<DataMoverSessionID, DataMoverSessionInfo>();
    protected HashMap<EntityID, SSLSocketFactory> socketFactories = new HashMap<EntityID, SSLSocketFactory>();
    protected HashMap<DataMoverSessionID, SessionAuthentication> sessionAuthentications = new HashMap<DataMoverSessionID, SessionAuthentication>();
    private static DataMoverReceiver singleton;
    private InetAddress [] localAddresses;
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
        /*
         * First, we'll setup a listener so that if any security servers are trusted later
         * we will be able to service them
         */
        EntityAuthenticationClient.getEntityAuthenticationClient().addEntityAuthenticationClientListener(new EntityAuthenticationClientListener() {
            public void entityAuthenticationServerUntrusted(
                    EntityAuthenticationServerUntrustedEvent untrustedEvent)
            {
                // TODO Auto-generated method stub
            }
            
            public void entityAuthenticationServerTrusted(EntityAuthenticationServerTrustedEvent trustedEvent)
            {
                try
                {
                    initializeForSecurityServer(trustedEvent.getAddedServer().getEntityID());
                } catch (RemoteException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                }
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
        
        /*
         * Then, list out the trusted servers and prepare to service them
         */
        EntityAuthenticationServer [] trustedSecurityServers = EntityAuthenticationClient.getEntityAuthenticationClient().listTrustedServers();
        for (EntityAuthenticationServer curTrusted:trustedSecurityServers)
        {
            try
            {
                initializeForSecurityServer(curTrusted.getEntityID());
            } catch (RemoteException e)
            {
                Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            }
        }
        
    }
    protected void initializeForSecurityServer(EntityID securityServerID)
    {
        SSLContext sslContext;
        try
        {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(EntityAuthenticationClient.getEntityAuthenticationClient().getKeyManagers(securityServerID), EntityAuthenticationClient.getEntityAuthenticationClient().getTrustManagers(securityServerID), null);
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            socketFactories.put(securityServerID, socketFactory);
        } catch (NoSuchAlgorithmException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            throw new InternalError("Cannot initialize TLS algorithm");
            
        } catch (KeyManagementException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            throw new InternalError("Cannot initialize TLS algorithm");
        }
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
					receiveSocket = AFUnixAuthenticatedSocket.connectTo(connectAddress, descriptorFromServer.getSecurityServerID());
				}
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
    		boolean connected = false;
    		for (InetSocketAddress tryAddress:connectAddresses)
    		{
    			InetAddress connectHost = tryAddress.getAddress();
    			if (connectHost == null)	// Other side is not running a connect loop
    				throw new NoMoverPathException();
    			if (connectHost.equals(InetAddress.getLocalHost()))
    				connectHost = InetAddress.getByName("127.0.0.1");	// Certain network configuration do evil routing (packets out and back).  Use the loopback to avoid that
    			int connectPort = tryAddress.getPort();
    			logger.error(new ErrorLogMessage("Connecting to server at {0} port {1}", new Serializable[]{connectHost.toString(), connectPort}));
    			SSLSocketFactory socketFactory = socketFactories.get(descriptorFromServer.getSecurityServerID());
    			try
    			{
    				receiveSocket = socketFactory.createSocket(connectHost, connectPort);
    				connected = true;
    				break;	// OK, we're good!
    			} catch (Exception e)
    			{
    				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			}
    		}
    		if (!connected)
				throw new NoMoverPathException();	// We tried everything
    	}
    	AuthenticatedConnection connection = AuthenticatedConnection.getAuthenticatedConnection(receiveSocket);
    	connection.getOutputStream().write(DataMoverSource.kNormalConnectionRequest);
    	connection.getOutputStream().flush();
    	fillInDataMoverServerInfoForConnection(connection, fillInInfo);
    }

	private void fillInDataMoverServerInfoForConnection(
			AuthenticatedConnection connection, DataMoverServerInfo fillInInfo)
			throws AuthenticationFailureException, IOException,
			ServerTooBusyException
	{
		connection.validateSocket(fillInInfo.getServerID());
        OutputStream receiveSocketOS = connection.getOutputStream();
        InputStream receiveSocketIS = connection.getInputStream();
        receiveSocketOS.write(DataMoverSource.kConnected);
        int reply = receiveSocketIS.read();
        if (reply < 0)
            throw new IOException("Server socket closed prematurely");
        if (reply == DataMoverSource.kServerBusy)
            throw new ServerTooBusyException("Server busy");

        fillInInfo.setConnection(connection);
	}

    protected DataMoverSessionInfo getSession(NetworkDataDescriptor sourceDescriptor) throws IOException, AuthenticationFailureException
    {
        DataMoverSessionID sessionID = sourceDescriptor.getSessionID();
        DataMoverSessionInfo returnInfo;
        boolean openSession = false;
		synchronized (sessions)
		{
			returnInfo = sessions.get(sessionID);
			if (returnInfo == null)
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
					OpenSessionCommand openSessionCommand = new OpenSessionCommand(sessionID, sessionAuthentication);
					try
					{
						serverInfo.sendSynchronousCommand(openSessionCommand);
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
    		if (returnInfo == null)
    		{
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
    
    protected void reverseConnection(AuthenticatedConnection connection) throws ServerTooBusyException, AuthenticationFailureException, IOException
    {
    	EntityAuthentication [] clients = connection.getAuthenticatedClients();
    	if (clients != null && clients.length > 0)
    	{
    		EntityID dataMoverServerID = clients[0].getEntityID();
    		DataMoverServerInfo returnInfo;
    		synchronized(servers)
    		{
    			returnInfo = servers.get(dataMoverServerID);    // Make sure no one beat us in here
    		}
    		try
    		{
    			if (returnInfo != null)
    			{
    				if (returnInfo.checkConnection(1))
    				{
    					returnInfo.sendSynchronousCommand(new CheckConnectionCommand());
    				}
    				else
    				{
    					synchronized(servers)
    					{
    						servers.remove(dataMoverServerID);
    						returnInfo = null;
    					}
    				}
    			}
    		} catch (IOException e)
    		{
    			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			synchronized(servers)
    			{
    				servers.remove(dataMoverServerID);
    			}
    			returnInfo = null;
    		}
    		boolean doConnect = false;
    		synchronized(servers)
    		{
    			returnInfo = servers.get(dataMoverServerID);
    			if (returnInfo == null)
    			{
    				doConnect = true;
    				returnInfo = new DataMoverServerInfo(dataMoverServerID);
        			servers.put(dataMoverServerID, returnInfo);
    			}
    			else
    			{
    				connection.getInputStream().close();
    				connection.getOutputStream().close();
    			}
    		}
    		if (doConnect)
    		{
    			connection.getOutputStream().write(DataMoverSource.kConnected);	// Let the other side continue on
    			fillInDataMoverServerInfoForConnection(connection, returnInfo);
    		}
    	}
    	else
    	{
    		throw new AuthenticationFailureException("No authenticated clients");
    	}
    }
    
    protected void sessionFailed(DataMoverSessionInfo failedSession)
    {
        synchronized(sessions)
        {
            synchronized(servers)
            {
                sessions.remove(failedSession.getSessionID());
                
                servers.remove(failedSession.getServerInfo().getServerID());
            }
        }
    }
    public boolean sameHost(NetworkDataDescriptor checkDescriptor)
    {
    	InetSocketAddress [] checkAddresses = checkDescriptor.getHostPorts();
    	for (InetAddress curLocalAddress:localAddresses)
    	{
    		for (InetSocketAddress checkAddress:checkAddresses)
    		{
    			if (curLocalAddress.equals(checkAddress.getAddress()))
    				return true;
    		}
    	}
    	return false;
    }
    
    public static final int kMaxTries = 5;
    
    public int getDataFromDescriptor(NetworkDataDescriptor sourceDescriptor, byte [] buffer, int destOffset, long srcOffset, int length, boolean release)
    throws IOException, AuthenticationFailureException
    {
    	return getDataFromDescriptor(sourceDescriptor, ByteBuffer.wrap(buffer, destOffset, length), srcOffset, length, release);
    }
    
    public int getDataFromDescriptor(NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, boolean release)
    throws IOException, AuthenticationFailureException
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
    				return shareableDescriptor.getData(destination, srcOffset, length, release);
    			}
    		}

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
    					flags |= DataRequestCommand.kReleaseDescriptor;
    				DataRequestCommand readCommand = new DataRequestCommand(sessionInfo.getSessionID(), sourceDescriptor.getID(), 
    						srcOffset, length, flags, destination);
    				Log4JStopWatch receiveDataWatch = new Log4JStopWatch("DataMoverReceiver.getDataFromDescriptor.receiveData");
    				sessionInfo.sendSynchronousCommand(readCommand);
    				receiveDataWatch.stop();
    				commandSent = true;
    			}
    			catch (NoMoverPathException e)
    			{
    				throw e;	// Just exit
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
    		return length;
    	}
    	finally
    	{
    		getDataWatch.stop();
    	}
    }
    
    public MoverFuture getDataFromDescriptorAsync(NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, boolean release)
    		throws IOException, AuthenticationFailureException
    {
    	MoverFuture returnFuture = new MoverFuture();
    	getDataFromDescriptorAsync(sourceDescriptor, destination, srcOffset, length, release, returnFuture);
    	return returnFuture;
    }
    
    public <A> void getDataFromDescriptorAsync(NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, boolean release,
    		AsyncCompletion<Void, ? super A>completionHandler, A attachment)
    throws IOException, AuthenticationFailureException
    {
    	MoverFuture future = new MoverFuture(completionHandler, attachment);
    	getDataFromDescriptorAsync(sourceDescriptor, destination, srcOffset, length, release, future);
    }
    
    private void getDataFromDescriptorAsync(NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, boolean release,
    		MoverFuture future) throws IOException, AuthenticationFailureException
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
    					future.completed(null, future);
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
    						flags |= DataRequestCommand.kReleaseDescriptor;
    					DataRequestCommand readCommand = new DataRequestCommand(sessionInfo.getSessionID(), sourceDescriptor.getID(), 
    							srcOffset, length, flags, destination, future);
    					Log4JStopWatch receiveDataWatch = new Log4JStopWatch("DataMoverReceiver.getDataFromDescriptor.receiveData");
    					sessionInfo.sendAsynchronousCommand(readCommand);
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
    
    protected SSLSocketFactory getSocketFactoryForSecurityServer(EntityID securityServerID)
    {
    	return socketFactories.get(securityServerID);
    }

	protected void closeServer(DataMoverServerInfo dataMoverServerInfo)
	{
		synchronized(servers)
		{
			servers.remove(dataMoverServerInfo.getServerID());
		}
	}
}
