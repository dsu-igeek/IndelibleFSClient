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
package com.igeekinc.indelible.indeliblefs.uniblock.msgpack;

import java.net.InetSocketAddress;

import org.msgpack.annotation.Message;

@Message
public class GetMoverAddressesReply
{
	String [] hostnames;
	int [] ports;
	
	public GetMoverAddressesReply()
	{
		// for message pack
	}
	
	public GetMoverAddressesReply(InetSocketAddress [] moverAddresses)
	{
		this.hostnames = new String[moverAddresses.length];
		this.ports = new int[moverAddresses.length];
		for (int curMoverAddressNum = 0; curMoverAddressNum < moverAddresses.length; curMoverAddressNum++)
		{
			this.hostnames[curMoverAddressNum] = moverAddresses[curMoverAddressNum].getAddress().getHostName();
			this.ports[curMoverAddressNum] = moverAddresses[curMoverAddressNum].getPort();
		}
	}
	
	public InetSocketAddress [] getMoverAddresses()
	{
		InetSocketAddress [] returnAddresses = new InetSocketAddress[hostnames.length];
		for (int curMoverAddressNum = 0; curMoverAddressNum < hostnames.length; curMoverAddressNum++)
		{
			returnAddresses[curMoverAddressNum] = new InetSocketAddress(hostnames[curMoverAddressNum], ports[curMoverAddressNum]);
		}
		return returnAddresses;
	}
}
