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
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.CRC32;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
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
import com.igeekinc.indelible.indeliblefs.security.afunix.AFUnixAuthenticatedServerSocket;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.IndelibleFSClientOIDs;
import com.igeekinc.indelible.oid.NetworkDataDescriptorID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.OSType;
import com.igeekinc.util.SystemInfo;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.DebugLogMessage;
import com.igeekinc.util.logging.ErrorLogMessage;

class ServerLoopRunnable implements Runnable
{
    DataMoverSource source;
    EntityID securityServerID;
    boolean connectLoopRunning = false;
    SocketAddress socketAddress;
    ServerSocket serverSocket;
    
    public ServerLoopRunnable(DataMoverSource source, EntityID securityServerID, ServerSocket serverSocket)
    {
        this.source = source;
        this.securityServerID = securityServerID;
        this.serverSocket = serverSocket;
    }
    
    public void run()
    {
    	Logger.getLogger(getClass()).error(new ErrorLogMessage("DataMoverSource connect loop starting port = {0}:{1}", serverSocket.getLocalSocketAddress(),serverSocket.getLocalPort()));
        try
        {
        	connectLoopRunning = true;
        	source.serverLoopStarted();
        	socketAddress = serverSocket.getLocalSocketAddress();
        	source.connectLoop(serverSocket);
        }
        finally
        {
        	Logger.getLogger(getClass()).error(new ErrorLogMessage("DataMoverSource connect loop exiting port = {0}:{1}", serverSocket.getLocalSocketAddress(),serverSocket.getLocalPort()));
        }
    }
    
    public boolean isConnectLoopRunning()
    {
        return connectLoopRunning;
    }
    
    public SocketAddress getSocketAddress()
    {
        return socketAddress;
    }
}
class ClientRunnable implements Runnable
{
    private String connName;
    private DataMoverSource server;
    private AuthenticatedConnection connection;
    
    public ClientRunnable(String connName, DataMoverSource server, AuthenticatedConnection connection) throws IOException
    {
        this.connName = connName;
        this.server = server;
        this.connection = connection;
    }
    
    public void run()
    {
    	Logger.getLogger(getClass()).error(new ErrorLogMessage("DataMoverSource server loop starting local port = {0}, remote addr = {1}, remote port = {2}",
    			connection.getLocalPort(), ((InetSocketAddress)connection.getRemotePort()).getAddress(), ((InetSocketAddress)connection.getRemotePort()).getPort()));
        try
		{
			server.serverLoop(this);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
        finally
        {
        	Logger.getLogger(getClass()).error(new ErrorLogMessage("DataMoverSource server loop exiting local port = {0}, remote addr = {1}, remote port = {2}",
    			connection.getLocalPort(), ((InetSocketAddress)connection.getRemotePort()).getAddress(), ((InetSocketAddress)connection.getRemotePort()).getPort()));
        }

    }

    public String getConnName()
    {
        return connName;
    }

    public DataMover getServer()
    {
        return server;
    }

    public InputStream getClientInputStream()
    {
        return connection.getInputStream();
    }

    public OutputStream getClientOutputStream()
    {
        return connection.getOutputStream();
    }
    
    public EntityAuthentication [] getAuthenticatedClients() throws SecurityException
    {
    	return connection.getAuthenticatedClients();
    }
}

public class DataMoverSource extends DataMover
{
	// These are the session initiation commands
    public static final char kNormalConnectionRequest	= 'N';
    public static final byte kReverseConnectionRequest = 'R';
    
	public static final int kDefaultMaxClients = 100;
    public static final byte kConnected = 'C';
    public static final byte kServerBusy = 'B';
    public static final byte kAuthenticationFailure = 'A';
    public static final byte kRequestSend = 'S';
    public static final byte kDataSend='D';
    public static final byte kFinished = 'F';
    public static final byte kOpenSession = 'O';
    public static final byte kConnectionAborted='A';
    public static final byte kCommandOK='K';
    public static final byte kCheckConnection = 'X';
    
    public static final int kCopyBufSize = 16*1024;
    
    protected ArrayList<ClientRunnable>activeClients = new ArrayList<ClientRunnable>();
    protected HashMap<DataMoverSessionID, DataMoverSession>sessions;
    protected HashMap<EntityID, ServerLoopRunnable>sslServerLoopsForSecurityClientID = new HashMap<EntityID, ServerLoopRunnable>();
    protected HashMap<EntityID, ServerLoopRunnable>localServerLoopsForSecurityClientID = new HashMap<EntityID, ServerLoopRunnable>();

    int maxClients = kDefaultMaxClients;
    ArrayList<InetAddress> ourAddresses = new ArrayList<InetAddress>();
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
    
    public static synchronized void init(ObjectIDFactory oidFactory)
    {
        if (singleton == null)
            singleton = new DataMoverSource(oidFactory);
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
    
    private DataMoverSource(ObjectIDFactory oidFactory)
    {
        super(oidFactory);

        sessions = new HashMap<DataMoverSessionID, DataMoverSession>();
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
    
    void initializeForSecurityServer(EntityID securityServerID)
    {
        if (sslServerLoopsForSecurityClientID.get(securityServerID) != null)
            return;
        if (noServer)
        	return;
        SSLServerSocket networkServerSocket = initSSLServerSocket(securityServerID);
        ServerLoopRunnable networkServerLoopRunnable = new ServerLoopRunnable(this, securityServerID, networkServerSocket);
        Thread networkConnectLoopThread = new Thread(networkServerLoopRunnable, "DMS Connect "+securityServerID.toString());
        networkConnectLoopThread.setDaemon(true);
        sslServerLoopsForSecurityClientID.put(securityServerID, networkServerLoopRunnable);
        networkConnectLoopThread.start();
        while(networkServerLoopRunnable.isConnectLoopRunning() == false)
        {
            synchronized(this)
            {
                try
                {
                    wait(1000);
                } catch (InterruptedException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                }
            }
        }
        
        if (SystemInfo.getSystemInfo().getOSType() == OSType.kMacOSX || SystemInfo.getSystemInfo().getOSType() == OSType.kLinux)
        {
			AFUnixAuthenticatedServerSocket localServerSocket = initAFUnixAuthenticatedServerSocket(
					securityServerID, numSecurityServers++);
			ServerLoopRunnable localServerLoopRunnable = new ServerLoopRunnable(
					this, securityServerID, localServerSocket);
			Thread localConnectLoopThread = new Thread(localServerLoopRunnable,
					"DMS Local Connect " + securityServerID.toString());
			localConnectLoopThread.setDaemon(true);
			localServerLoopsForSecurityClientID.put(securityServerID,
					localServerLoopRunnable);
			localConnectLoopThread.start();
			while (localServerLoopRunnable.isConnectLoopRunning() == false) {
				synchronized (this) {
					try {
						wait(1000);
					} catch (InterruptedException e) {
						Logger.getLogger(getClass()).error(
								new ErrorLogMessage("Caught exception"), e);
					}
				}
			}
		}
    }
    
    public SSLServerSocket initSSLServerSocket(EntityID securityServerID)
    {
        SSLServerSocket serverSocket = null;
        try
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");


            sslContext.init(EntityAuthenticationClient.getEntityAuthenticationClient().getKeyManagers(securityServerID), EntityAuthenticationClient.getEntityAuthenticationClient().getTrustManagers(securityServerID), null);
            SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();
            serverSocket = (SSLServerSocket) socketFactory.createServerSocket(0);
            serverSocket.setNeedClientAuth(true);
            ourAddresses.add(InetAddress.getLocalHost());
            String rmiServerHostList = System.getProperty("java.rmi.server.hostname");
            if (rmiServerHostList != null)
            {
            	String[] hosts;
            	hosts = rmiServerHostList.split("!");
            	for (String curHost:hosts)
            	{
            		try
            		{
            			InetAddress curHostAddr = InetAddress.getByName(curHost);
            			if (!ourAddresses.contains(curHostAddr))
            				ourAddresses.add(curHostAddr);
            		} catch (Exception e)
            		{
            			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception resolving hostname {0}", new Object[]{curHost}), e);
            		}
            	}
            }
        } catch (IOException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (NoSuchAlgorithmException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (KeyManagementException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        }
        return serverSocket;
    }
    
    public AFUnixAuthenticatedServerSocket initAFUnixAuthenticatedServerSocket(EntityID securityServerID, int securityServerNum)
    {
    	AFUnixAuthenticatedServerSocket returnSocket = null;
    	try
		{
    	    String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();  
    	    int p = nameOfRunningVM.indexOf('@');  
    	    String pid = nameOfRunningVM.substring(0, p); 
    		String socketName = "dm-"+id.toString()+
    				securityServerNum+
    				pid;
			File socketFile = new File("/tmp/"+socketName);
			if (socketFile.exists())
				socketFile.delete();
			AFUNIXSocketAddress dataMoverAddress = new AFUNIXSocketAddress(socketFile);
			returnSocket = AFUnixAuthenticatedServerSocket.bindOn(dataMoverAddress, securityServerID);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
    	return returnSocket;
    }
    
    public synchronized void serverLoopStarted()
    {
    	notify();
    }
    public void connectLoop(ServerSocket serverSocket)
    {
        while(true)
        {
            try
            {
                Socket newClientSocket;
                try
                {
                    newClientSocket = serverSocket.accept();
                } catch (SSLException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    break;
                }
                AuthenticatedConnection authenticatedConnection = AuthenticatedConnection.getAuthenticatedConnection(newClientSocket);
                int connectionType = authenticatedConnection.getInputStream().read();
				if (connectionType == kNormalConnectionRequest)
                {
                	if (activeClients.size() < maxClients)
                	{
                		String threadName;
                		if (newClientSocket instanceof SSLSocket)
                			threadName = "DMC "+newClientSocket.getInetAddress().toString()+" - "+newClientSocket.getPort();
                		else
                			threadName = "DMC "+serverSocket.getLocalSocketAddress();
                		ClientRunnable runnableForSocket = new ClientRunnable(threadName, this, authenticatedConnection);

                		Thread newClientThread = new Thread(runnableForSocket, threadName);
                		newClientThread.start();
                	}
                	else
                	{
                		OutputStream newClientOutputStream = newClientSocket.getOutputStream();
                		newClientOutputStream.write(kServerBusy);
                		newClientOutputStream.close();
                	}
                }
				else
				{
					if (connectionType == 'R')
					{
						DataMoverReceiver.getDataMoverReceiver().reverseConnection(authenticatedConnection);
					}
					else
					{
						newClientSocket.close();
					}
				}
            } catch (IOException e)
            {
                Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            } catch (AuthenticationFailureException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
        }
    }
    
    public void serverLoop(ClientRunnable runnable) throws IOException
    {
        EntityAuthentication [] authenticatedClients;
        HashMap<DataMoverSessionID, DataMoverSession>openSessions = new HashMap<DataMoverSessionID, DataMoverSession>();
        try
        {
            OutputStream clientOutputStream = runnable.getClientOutputStream();
            InputStream clientInputStream = runnable.getClientInputStream();
            try
            {
                authenticatedClients = runnable.getAuthenticatedClients();
                
                int cmdByte;
                cmdByte = clientInputStream.read();
                if (cmdByte == DataMoverSource.kConnected)
                {
                    clientOutputStream.write(DataMoverSource.kConnected);
                    clientOutputStream.flush();
                    byte [] commandNumberBytes = new byte[8];
                    byte [] copyBuf = new byte[kCopyBufSize];
                    while((cmdByte = clientInputStream.read()) != -1)
                    {
                        if (clientInputStream.read(commandNumberBytes) != 8)
                        {
                            break;
                        }
                        long commandNum = BitTwiddle.javaByteArrayToLong(commandNumberBytes, 0);
                        logger.debug(new DebugLogMessage("Got command = {0}", commandNum));
                        switch(cmdByte)
                        {
                        case kRequestSend:
                        {
                        	logger.debug(new DebugLogMessage("Handling RequestSend"));
                        	Log4JStopWatch requestSendWatch = new Log4JStopWatch("DataMoverSource.serverLoop.requestSend");
                        	try
                        	{
                        		byte [] requestInfoBytes = new byte[DataRequestCommand.kCommandSize - DataMoverCommand.kCommandHeaderSize];	// Already got the header
                        		int requestInfoBytesRead = clientInputStream.read(requestInfoBytes);
                        		if (requestInfoBytesRead != requestInfoBytes.length)
                        		{
                        			abortConnection(clientInputStream, clientOutputStream);
                        			return;
                        		}
                        		int requestInfoBytesOffset = 0;
                        		DataMoverSessionID sessionID = (DataMoverSessionID)ObjectIDFactory.reconstituteFromBytes(requestInfoBytes, 0, ObjectID.kTotalBytes);
                        		requestInfoBytesOffset += ObjectID.kTotalBytes;
                        		DataMoverSession openSession;
                        		openSession = getSession(sessionID);
                        		if (openSession != null)
                        		{
                        			NetworkDataDescriptorID nddID = (NetworkDataDescriptorID)ObjectIDFactory.reconstituteFromBytes(requestInfoBytes, requestInfoBytesOffset, ObjectID.kTotalBytes);
                        			requestInfoBytesOffset += ObjectID.kTotalBytes;
                        			long offset = BitTwiddle.javaByteArrayToLong(requestInfoBytes, requestInfoBytesOffset);
                        			requestInfoBytesOffset += 8;
                        			long bytesToRead = BitTwiddle.javaByteArrayToLong(requestInfoBytes, requestInfoBytesOffset);
                        			requestInfoBytesOffset += 8;
                        			byte flags = requestInfoBytes[requestInfoBytesOffset];
                        			DataDescriptor localDataDescriptor = openSession.getDataDescriptor(nddID);
                        			if (localDataDescriptor != null)
                        			{
                        				if (offset < localDataDescriptor.getLength() || (offset == localDataDescriptor.getLength() && bytesToRead == 0))
                        				{
                        					Log4JStopWatch requestSendTotalWatch = new Log4JStopWatch("DataMoverSource.serverLoop.sendTotal");
                        					Log4JStopWatch requestSendCommandOKWatch = new Log4JStopWatch("DataMoverSource.serverLoop.commandOK");
                        					commandOK(commandNum, clientOutputStream);
                        					requestSendCommandOKWatch.stop();
                        					long actualBytesToRead;
                        					long availableBytes = localDataDescriptor.getLength() - offset;
                        					if (availableBytes > bytesToRead)
                        						actualBytesToRead = bytesToRead;
                        					else
                        						actualBytesToRead = availableBytes;
                        					byte [] dataStartHeaderBuf = new byte[ObjectID.kTotalBytes + 8 + 8 + 1];
                        					int bufOffset = 0;
                        					dataStartHeaderBuf[0] = kDataSend;
                        					bufOffset ++;
                        					nddID.getBytes(dataStartHeaderBuf, bufOffset);
                        					bufOffset += ObjectID.kTotalBytes;
                        					BitTwiddle.longToJavaByteArray(offset, dataStartHeaderBuf, bufOffset);
                        					bufOffset += 8;
                        					BitTwiddle.longToJavaByteArray(actualBytesToRead, dataStartHeaderBuf, bufOffset);
                        					Log4JStopWatch requestSendHeaderWatch = new Log4JStopWatch("DataMoverSource.serverLoop.sendHeader");
                        					clientOutputStream.write(dataStartHeaderBuf);
                        					requestSendHeaderWatch.stop();
                        					long bytesRemaining = actualBytesToRead;
                        					long srcOffset = offset;
                        					CRC32 dataCRC = new CRC32();
                        					while (bytesRemaining > 0)
                        					{
                        						int curBytesToCopy;
                        						if (bytesRemaining > kCopyBufSize)
                        							curBytesToCopy = kCopyBufSize;
                        						else
                        							curBytesToCopy = (int)bytesRemaining;
                        						boolean release = false;
                        						if (curBytesToCopy == bytesRemaining)
                        							release = true;
                        						Log4JStopWatch requestSendDataWatch = new Log4JStopWatch("DataMoverSource.serverLoop.data");
                        						localDataDescriptor.getData(copyBuf, 0, srcOffset, curBytesToCopy, release);
                        						clientOutputStream.write(copyBuf, 0, curBytesToCopy);
                        						dataCRC.update(copyBuf, 0, curBytesToCopy);
                        						requestSendDataWatch.stop();
                        						srcOffset += curBytesToCopy;
                        						bytesRemaining -= curBytesToCopy;
                        					}
                        					byte [] crcBuf = new byte[8];
                        					BitTwiddle.longToJavaByteArray(dataCRC.getValue(), crcBuf, 0);
                        					clientOutputStream.write(crcBuf, 0, crcBuf.length);
                        					logger.debug(new DebugLogMessage("Sent CRC = {0} for {1} bytes", dataCRC.getValue(), actualBytesToRead));
                        					requestSendTotalWatch.stop();
                        				}
                        				else
                        				{
                        					// Fail miserably - if we're getting a bad request here, there's something wrong on the other side,
                        					// or someone is trying to do something weird
                        					abortConnection(clientInputStream, clientOutputStream);
                        					return;
                        				}
                        				if ((flags & DataRequestCommand.kReleaseDescriptor) != 0)
                        					openSession.removeDataDescriptor(nddID);
                        			}
                        			else
                        			{
                        				logger.error(new ErrorLogMessage("Could not find local descriptor for {0}",
                        						nddID));
                        			}
                        		}
                        		else
                        		{
                        			logger.error(new ErrorLogMessage("Could not find session for {0}", sessionID));
                        		}
                        	}
                        	finally
                        	{
                        		requestSendWatch.stop();
                        	}
                        	break;
                        }
                        case kOpenSession:
                        {
                        	logger.debug(new DebugLogMessage("Handling OpenSession"));
                        	Log4JStopWatch openSessionWatch = new Log4JStopWatch("DataMoverSource.serverLoop.openSession");
                        	try
                        	{
                        		byte [] sessionIDBytes = new byte[ObjectID.kTotalBytes];
                        		int sessionIDBytesRead = clientInputStream.read(sessionIDBytes);
                        		if (sessionIDBytesRead != ObjectID.kTotalBytes)
                        		{
                        			abortConnection(clientInputStream, clientOutputStream);
                        			return;
                        		}
                        		DataMoverSessionID sessionID = (DataMoverSessionID)ObjectIDFactory.reconstituteFromBytes(sessionIDBytes);
                        		logger.debug(new DebugLogMessage("OpenSession request for {0} our ID = {1}", sessionID, getEntityID()));
                        		byte [] sessionAuthenticationLengthBytes = new byte[4];
                        		if (clientInputStream.read(sessionAuthenticationLengthBytes) != 4)
                        		{
                        			abortConnection(clientInputStream, clientOutputStream);
                        			return;
                        		}
                        		int sessionAuthenticationLength = BitTwiddle.javaByteArrayToInt(sessionAuthenticationLengthBytes, 0);
                        		byte [] sessionAuthenticationBytes = new byte[sessionAuthenticationLength];
                        		int sessionAuthenticationBytesRead = clientInputStream.read(sessionAuthenticationBytes);
                        		if (sessionAuthenticationBytesRead != sessionAuthenticationLength)
                        		{
                        			abortConnection(clientInputStream, clientOutputStream);
                        			return;
                        		}
                        		SessionAuthentication checkAuthentication = new SessionAuthentication(sessionAuthenticationBytes);
                        		DataMoverSession openSession = sessions.get(sessionID);
                        		if (openSession != null)
                        		{
                        			if (openSession.isAuthorized(authenticatedClients, checkAuthentication))
                        			{
                        				openSessions.put(sessionID, openSession);
                        				commandOK(commandNum, clientOutputStream);
                        				logger.debug(new DebugLogMessage("Session {0} opened successfully", sessionID));
                        			}
                        			else
                        			{
                        				logger.error(new ErrorLogMessage("Session {0} authentication failed", sessionID));
                        				authenticationFailed(commandNum, clientOutputStream);
                        			}
                        		}
                        		else
                        		{
                        			logger.error(new ErrorLogMessage("Could not find session ID {0} for OpenSession request", sessionID));
                        		}
                        	}
                        	finally
                        	{
                        		openSessionWatch.stop();
                        	}
                        	break;
                        }
                        case kFinished:
                        {
                        	logger.debug("Handling Finished");
                        	commandOK(commandNum, clientOutputStream);
                        	clientOutputStream.flush();
                        	break;
                        }
                        }
                        clientOutputStream.flush();
                    }
                }
            } catch (SSLPeerUnverifiedException e)
            {
                clientOutputStream.write(kAuthenticationFailure);
                clientOutputStream.close();

                Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            }
            finally
            {
                abortConnection(clientInputStream, clientOutputStream);
            }
        } catch (Throwable e1)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
        }
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
    
    void commandOK(long commandNum, OutputStream stream) throws IOException
    {
        byte [] okBuf = new byte[9];
        okBuf[0] = kCommandOK;
        BitTwiddle.longToJavaByteArray(commandNum, okBuf, 1);
        stream.write(okBuf);
        logger.debug(new DebugLogMessage("Sent command OK for command {0}", commandNum));
    }
    
    void authenticationFailed(long commandNum, OutputStream stream) throws IOException
    {
        byte [] okBuf = new byte[9];
        okBuf[0] = kAuthenticationFailure;
        BitTwiddle.longToJavaByteArray(commandNum, okBuf, 1);
        stream.write(okBuf);
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

    public InetSocketAddress [] getHostPorts(EntityID securityServerID)
    {
        ServerLoopRunnable runnableForSecurityServer = sslServerLoopsForSecurityClientID.get(securityServerID);
        if (runnableForSecurityServer == null)
            return new InetSocketAddress[0];		// We may be running in a reverse situation
        int portNum = ((InetSocketAddress)runnableForSecurityServer.getSocketAddress()).getPort();
        InetSocketAddress [] returnAddresses = new InetSocketAddress[ourAddresses.size()];
        for (int curAddressNum = 0; curAddressNum < returnAddresses.length; curAddressNum++)
        {
        	returnAddresses[curAddressNum] = new InetSocketAddress(ourAddresses.get(curAddressNum), portNum);
        }
        return returnAddresses;
    }
    
    public File getLocalSocket(EntityID securityServerID)
    {
        ServerLoopRunnable runnableForSecurityServer = localServerLoopsForSecurityClientID.get(securityServerID);
        if (runnableForSecurityServer == null)
            return null;	// No local loop
        return new File(((AFUNIXSocketAddress)runnableForSecurityServer.getSocketAddress()).getSocketFile());
    }
    
    public void openReverseConnection(EntityID securityServerID, InetAddress connectHost, int connectPort) throws IOException, AuthenticationFailureException
    {
    	Socket receiveSocket;
    	if (connectHost.equals(InetAddress.getLocalHost()))
    		connectHost = InetAddress.getByName("127.0.0.1");	// Certain network configuration do evil routing (packets out and back).  Use the loopback to avoid that
    	logger.error(new ErrorLogMessage("Connecting to server at {0} port {1}", new Serializable[]{connectHost.toString(), connectPort}));
    	if (activeClients.size() < maxClients)
    	{
    		SSLSocketFactory socketFactory = DataMoverReceiver.getDataMoverReceiver().getSocketFactoryForSecurityServer(securityServerID);
    		receiveSocket = socketFactory.createSocket(connectHost, connectPort);
    		AuthenticatedConnection connection = AuthenticatedConnection.getAuthenticatedConnection(receiveSocket);
    		connection.getOutputStream().write(DataMoverSource.kReverseConnectionRequest);
    		connection.getOutputStream().flush();
    		int okReturn = connection.getInputStream().read();
    		String threadName;
    		threadName = "DMC "+receiveSocket.getInetAddress().toString()+" - "+receiveSocket.getPort();

    		ClientRunnable runnableForSocket = new ClientRunnable(threadName, this, connection);

    		Thread newClientThread = new Thread(runnableForSocket, threadName);
    		newClientThread.start();
    	}
    }
}
