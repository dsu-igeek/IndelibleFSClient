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

import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.datamover.msgpack.NetworkDataDescriptorIDMsgPack;
import com.igeekinc.indelible.oid.NetworkDataDescriptorID;

/**
 * A DataRequestReply will be sent as a reply to a DataRequest.  For efficiency, the DataRequestReply tells how
 * long the data will be but the data is then sent immediately after as raw bytes.
 * @author David L. Smith-Uchida
 *
 */
@Message
public class DataRequestReply
{
	public NetworkDataDescriptorIDMsgPack	descriptorID;
	public long								offset;
	public int								dataLength;		// Msgpack blows up with a long field named "length"
	
	public DataRequestReply()
	{
	}

	public DataRequestReply(NetworkDataDescriptorID descriptorID, long offset, int dataLength)
	{
		this.descriptorID = new NetworkDataDescriptorIDMsgPack(descriptorID);
		this.offset = offset;
		this.dataLength = dataLength;
	}

	public NetworkDataDescriptorIDMsgPack getDescriptorID()
	{
		return descriptorID;
	}

	public long getOffset()
	{
		return offset;
	}

	public int getDataLength()
	{
		return dataLength;
	}
}
