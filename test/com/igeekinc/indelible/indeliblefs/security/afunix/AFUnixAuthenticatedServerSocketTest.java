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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.igeekinc.indelible.indeliblefs.IndelibleFSClient;
import com.igeekinc.indelible.indeliblefs.IndelibleFSClientPreferences;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServer;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.MonitoredProperties;
import com.igeekinc.util.SHA1HashID;
import com.igeekinc.util.logging.ErrorLogMessage;

class DummyServer implements Runnable
{
	private AFUNIXSocketAddress bindAddress;
	private EntityID securityServerID;
	private boolean passed = true;
	
	public DummyServer(AFUNIXSocketAddress bindAddress, EntityID securityServerID)
	{
		this.bindAddress = bindAddress;
		this.securityServerID = securityServerID;
	}

	@Override
	public void run()
	{
		try
		{
			AFUnixAuthenticatedServerSocket serverSocket = AFUnixAuthenticatedServerSocket.bindOn(bindAddress, securityServerID);
			Socket acceptSocket = serverSocket.accept();
			
			OutputStream writeStream = acceptSocket.getOutputStream();
			InputStream readStream = acceptSocket.getInputStream();
			
			PrintWriter writer = new PrintWriter(writeStream);
			writer.print(AFUnixAuthenticatedServerSocketTest.kBasicServerMessage+"\n");
			writer.flush();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(readStream));
			String clientString = reader.readLine();
			if (clientString == null || !clientString.equals(AFUnixAuthenticatedServerSocketTest.kBasicClientMessage))
				passed = false;
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			passed = false;
		}
		
	}
	
	public boolean checkPassed()
	{
		return passed;
	}
}

class BigDummyServer implements Runnable
{
	private AFUNIXSocketAddress bindAddress;
	private EntityID securityServerID;
	private boolean passed = true;
	
	public BigDummyServer(AFUNIXSocketAddress bindAddress, EntityID securityServerID)
	{
		this.bindAddress = bindAddress;
		this.securityServerID = securityServerID;
	}

	@Override
	public void run()
	{
		try
		{
			AFUnixAuthenticatedServerSocket serverSocket = AFUnixAuthenticatedServerSocket.bindOn(bindAddress, securityServerID);
			Socket acceptSocket = serverSocket.accept();
			
			OutputStream writeStream = acceptSocket.getOutputStream();
			InputStream readStream = acceptSocket.getInputStream();
			
			PrintWriter writer = new PrintWriter(writeStream);
			writer.print(AFUnixAuthenticatedServerSocketTest.kBasicServerMessage+"\n");
			writer.flush();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(readStream));
			String clientString = reader.readLine();
			if (clientString == null || !clientString.equals(AFUnixAuthenticatedServerSocketTest.kBasicClientMessage))
				passed = false;
			byte [] bigBuffer = new byte[AFUnixAuthenticatedServerSocketTest.kBigDummySize];
			AFUnixAuthenticatedServerSocketTest.writeTestDataToBuffer(bigBuffer, 0, AFUnixAuthenticatedServerSocketTest.kBigDummySize, 0);
			writeStream.write(bigBuffer);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			passed = false;
		}

		
	}
	
	public boolean checkPassed()
	{
		return passed;
	}
}
public class AFUnixAuthenticatedServerSocketTest extends TestCase
{
	public static final int	kBigDummySize	= 64*1024*1024;
	public static final String kBasicClientMessage = "basicClient";
	public static final String kBasicServerMessage = "basicServer";
	
	private static boolean securityAndMoverInitialized = false;
    private static EntityAuthenticationServer securityServer;
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        if (!securityAndMoverInitialized)
        {
        	IndelibleFSClientPreferences.initPreferences(null);

            MonitoredProperties serverProperties = IndelibleFSClientPreferences.getProperties();
            File preferencesDir = new File(serverProperties.getProperty(IndelibleFSClientPreferences.kPreferencesDirPropertyName));
            File securityClientKeystoreFile = new File(preferencesDir, IndelibleFSClient.kIndelibleEntityAuthenticationClientConfigFileName);
            EntityAuthenticationClient.initializeEntityAuthenticationClient(securityClientKeystoreFile, null, serverProperties);
            EntityAuthenticationClient.startSearchForServers();
            EntityAuthenticationServer [] securityServers = new EntityAuthenticationServer[0];
            while(securityServers.length == 0)
            {
                securityServers = EntityAuthenticationClient.listEntityAuthenticationServers();
                if (securityServers.length == 0)
                    Thread.sleep(1000);
            }
            
            securityServer = securityServers[0];
            
            EntityAuthenticationClient.getEntityAuthenticationClient().trustServer(securityServer);
        }
    }
	public void testBasic() throws IOException, InterruptedException
	{
		File socketFile = new File("/tmp/AFUnixAuthenticatedServerSocketTest.socket");
		if (socketFile.exists())
			assertTrue(socketFile.delete());
		AFUNIXSocketAddress address = new AFUNIXSocketAddress(socketFile);
		EntityID securityServerID = EntityAuthenticationClient.listEntityAuthenticationServers()[0].getEntityID();
		DummyServer dummyServer = new DummyServer(address, securityServerID);
		Thread serverThread = new Thread(dummyServer);
		serverThread.start();
		
		while (!socketFile.exists())
		{
			Thread.sleep(500);
		}

		AFUnixAuthenticatedSocket clientSocket = AFUnixAuthenticatedSocket.connectTo(address, securityServerID);
		OutputStream writeStream = clientSocket.getOutputStream();
		InputStream readStream = clientSocket.getInputStream();

		PrintWriter writer = new PrintWriter(writeStream);
		writer.print(AFUnixAuthenticatedServerSocketTest.kBasicClientMessage+"\n");
		writer.flush();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(readStream));
		String serverString = reader.readLine();
		assertNotNull(serverString);
		assertTrue(serverString.equals(AFUnixAuthenticatedServerSocketTest.kBasicServerMessage));
		
		serverThread.join();
		assertTrue(dummyServer.checkPassed());
	}
	
	public void testBigData() throws IOException, InterruptedException
	{
		File socketFile = new File("/tmp/AFUnixAuthenticatedServerSocketTest.socket");
		if (socketFile.exists())
			assertTrue(socketFile.delete());
		AFUNIXSocketAddress address = new AFUNIXSocketAddress(socketFile);
		EntityID securityServerID = EntityAuthenticationClient.listEntityAuthenticationServers()[0].getEntityID();
		BigDummyServer dummyServer = new BigDummyServer(address, securityServerID);
		Thread serverThread = new Thread(dummyServer);
		serverThread.start();
		
		while (!socketFile.exists())
		{
			Thread.sleep(500);
		}

		AFUnixAuthenticatedSocket clientSocket = AFUnixAuthenticatedSocket.connectTo(address, securityServerID);
		OutputStream writeStream = clientSocket.getOutputStream();
		InputStream readStream = clientSocket.getInputStream();

		PrintWriter writer = new PrintWriter(writeStream);
		writer.print(AFUnixAuthenticatedServerSocketTest.kBasicClientMessage+"\n");
		writer.flush();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(readStream));
		String serverString = reader.readLine();
		assertNotNull(serverString);
		assertTrue(serverString.equals(AFUnixAuthenticatedServerSocketTest.kBasicServerMessage));
		
		byte [] receiveBuffer = new byte[kBigDummySize];
		long startTime = System.currentTimeMillis();
		int bytesRemaining = receiveBuffer.length;
		int offset = 0;
		boolean useSmallBuffer = false;
		if (useSmallBuffer)
		{
		byte [] smallBuffer = new byte[8192];
		while (bytesRemaining > 0)
		{
			int bytesToRead = smallBuffer.length;
			if (bytesToRead > bytesRemaining)
				bytesToRead = bytesRemaining;
			int bytesRead = readStream.read(smallBuffer, 0, bytesToRead);
			bytesRemaining -= bytesRead;
			//Date now = new Date();
			//System.out.println("Read "+bytesRead+" bytes "+now);
		}
		}
		else
		{
			while (bytesRemaining > 0)
			{
				int bytesRead = readStream.read(receiveBuffer, receiveBuffer.length - bytesRemaining, bytesRemaining);
				bytesRemaining -= bytesRead;
				//Date now = new Date();
				//System.out.println("Read "+bytesRead+" bytes "+now);
			}
		}
		long endTime = System.currentTimeMillis();
		long elapsedTime = endTime - startTime;
		System.out.println("Read "+receiveBuffer.length+" bytes in "+elapsedTime);
		
		serverThread.join();
		assertTrue(dummyServer.checkPassed());
	}
	
	public static void writeTestDataToBuffer(byte [] buffer, int bufferOffset, int size, int sequence)
    {
    	BitTwiddle.intToJavaByteArray(sequence, buffer, bufferOffset);
    	int bytesRemaining = size - 4;
    	int curBufOffset = bufferOffset;
    	curBufOffset += 4;
    	long curRandom = ((long)sequence << 32) | size;	// We need quick pseudo-random numbers (sequence should reproduce).  This
    												// algorithm is quick and, also, supposed to be quite random
    												// http://javamex.com/tutorials/random_numbers/xorshift.shtml

    	// Not aligned so we have to do it the slow way
    	for (int curByteNum = 0; curByteNum < bytesRemaining; curByteNum+=8)
    	{
    		curRandom ^= (curRandom << 21);
    		curRandom ^= (curRandom >>> 35);
    		curRandom ^= (curRandom << 4);
    		bytesRemaining -= 8;
    		if (bytesRemaining > 8)
    		{
    			BitTwiddle.longToJavaByteArray(curRandom, buffer, curBufOffset+curByteNum);
    		}
    		else
    		{
    			// Handle the less than 1 long space at the end
    			byte [] curRandomBytes = new byte[8];
    			BitTwiddle.longToJavaByteArray(curRandom, curRandomBytes, 0);
    			System.arraycopy(curRandomBytes, 0, buffer, curBufOffset + curByteNum, bytesRemaining);
    		}
    	}
    	return;
    }
    
    public static boolean verifyBuffer(byte [] checkBuffer, SHA1HashID originalHash, int bufferOffset, int checkSize, int sequence)
    {
    	if (BitTwiddle.javaByteArrayToInt(checkBuffer,  bufferOffset) != sequence)
    		return false;	// out of sequence
    	SHA1HashID checkHash = new SHA1HashID();
    	checkHash.update(checkBuffer, bufferOffset, checkSize);
    	checkHash.finalizeHash();
    	if (originalHash.equals(checkHash))
    		return true;
    	return false;
    }
}
