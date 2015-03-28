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
package com.igeekinc.indelible.indeliblefs.firehose.msgpack;

import java.net.InetSocketAddress;

import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.datamover.msgpack.InetSocketAddressMsgPack;

@Message
public class GetMoverAddressesReply
{
	public InetSocketAddressMsgPack [] moverAddresses;
	public GetMoverAddressesReply()
	{
		// for message pack
	}
	public GetMoverAddressesReply(InetSocketAddress [] moverAddresses)
	{
		this.moverAddresses = new InetSocketAddressMsgPack[moverAddresses.length];
		for(int curAddrNum = 0; curAddrNum < moverAddresses.length; curAddrNum++)
		{
			this.moverAddresses[curAddrNum] = new InetSocketAddressMsgPack(moverAddresses[curAddrNum]);
		}
	}
	
	public InetSocketAddress [] getMoverAddresses()
	{
		InetSocketAddress [] returnMoverAddresses = new InetSocketAddress[moverAddresses.length];
		for(int curAddrNum = 0; curAddrNum < moverAddresses.length; curAddrNum++)
		{
			returnMoverAddresses[curAddrNum] = moverAddresses[curAddrNum].getInetSocketAddress();
		}
		return returnMoverAddresses;
	}
}
