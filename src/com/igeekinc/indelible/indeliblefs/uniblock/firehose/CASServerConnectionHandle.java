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
package com.igeekinc.indelible.indeliblefs.uniblock.firehose;

import java.io.IOException;

import org.msgpack.MessagePackable;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

public class CASServerConnectionHandle implements MessagePackable
{
	private long connectionHandle;

	public CASServerConnectionHandle()
	{
		// for message pack
	}
	
	public CASServerConnectionHandle(long connectionHandle)
	{
		this.connectionHandle = connectionHandle;
	}
	public long getConnectionHandle()
	{
		return connectionHandle;
	}
	
	@Override
	public void writeTo(Packer pk) throws IOException
	{
		pk.write(connectionHandle);
	}
	@Override
	public void readFrom(Unpacker u) throws IOException
	{
		connectionHandle = u.readLong();
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (connectionHandle ^ (connectionHandle >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CASServerConnectionHandle other = (CASServerConnectionHandle) obj;
		if (connectionHandle != other.connectionHandle)
			return false;
		return true;
	}
}
