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

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandMessage;
import com.igeekinc.indelible.indeliblefs.datamover.msgpack.DataMoverSessionIDMsgPack;
import com.igeekinc.indelible.indeliblefs.datamover.msgpack.NetworkDataDescriptorIDMsgPack;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.NetworkDataDescriptorID;

@Message
public class DataRequestMessage extends CommandMessage
{
	/*
	 * Flags
	 */
    public static final byte kReleaseDescriptor = 0x01;

    public DataMoverSessionIDMsgPack sessionID;
    public NetworkDataDescriptorIDMsgPack descriptorID;
    public long offset;
    public int bytesToRead;
    public int flags;
	public DataRequestMessage(DataMoverSessionID sessionID, NetworkDataDescriptorID descriptorID, long offset, int bytesToRead, int flags)
	{
		this.sessionID = new DataMoverSessionIDMsgPack(sessionID);
		this.descriptorID = new NetworkDataDescriptorIDMsgPack(descriptorID);
		this.offset = offset;
		this.bytesToRead = bytesToRead;
		this.flags = flags;
	}

	public DataRequestMessage()
	{
		
	}

	public DataMoverSessionIDMsgPack getSessionID()
	{
		return sessionID;
	}

	public NetworkDataDescriptorIDMsgPack getDescriptorID()
	{
		return descriptorID;
	}

	public long getOffset()
	{
		return offset;
	}

	public int getBytesToRead()
	{
		return bytesToRead;
	}

	public int getFlags()
	{
		return flags;
	}

	@Override
	protected int getInitCommandCode()
	{
		return DataMoverClient.DataMoverCommand.kRequestSend.getCommandNum();
	}
}
