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
package com.igeekinc.indelible.indeliblefs.datamover.msgpack;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import junit.framework.TestCase;

public class InetSocketAddressMsgPackTest extends TestCase
{
	public static int kNumRepititions = 1000;
	public void testPackUnPackSpeed() throws UnknownHostException, SocketException
	{
		Enumeration<NetworkInterface>interfaces = NetworkInterface.getNetworkInterfaces();
		while(interfaces.hasMoreElements())
		{
			NetworkInterface curInterface = interfaces.nextElement();
			Enumeration<InetAddress> interfaceAddresses = curInterface.getInetAddresses();
			while (interfaceAddresses.hasMoreElements())
			{
				long startTime = System.currentTimeMillis();
				InetAddress localAddress = interfaceAddresses.nextElement();

				for (int count = 0; count < kNumRepititions; count++)
				{
					InetSocketAddress socketAddress = new InetSocketAddress(localAddress, 0);
					InetSocketAddressMsgPack testMsgPack = new InetSocketAddressMsgPack(socketAddress);
				}
				long endTime = System.currentTimeMillis();
				long elapsedTime = endTime - startTime;
				System.out.println(kNumRepititions+" reps for "+localAddress+" in "+elapsedTime+"ms");
			}

		}

	}
}
