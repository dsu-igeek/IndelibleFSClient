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
package com.igeekinc.indelible.indeliblefs.firehose;

import java.io.IOException;

import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

public class IndelibleSnapshotIteratorHandle implements org.msgpack.MessagePackable
{
	private long iteratorHandle;
	
	public IndelibleSnapshotIteratorHandle()
	{
		// for message pack
	}
	
	public IndelibleSnapshotIteratorHandle(long iteratorHandle)
	{
		this.iteratorHandle = iteratorHandle;
	}
	@Override
	public void writeTo(Packer pk) throws IOException
	{
		pk.write(iteratorHandle);
	}
	@Override
	public void readFrom(Unpacker u) throws IOException
	{
		iteratorHandle = u.readLong();
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (iteratorHandle ^ (iteratorHandle >>> 32));
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
		IndelibleSnapshotIteratorHandle other = (IndelibleSnapshotIteratorHandle) obj;
		if (iteratorHandle != other.iteratorHandle)
			return false;
		return true;
	}
}
