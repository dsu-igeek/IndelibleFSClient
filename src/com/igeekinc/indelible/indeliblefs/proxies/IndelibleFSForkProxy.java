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
 
package com.igeekinc.indelible.indeliblefs.proxies;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.concurrent.Future;

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSForkRemote;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.util.async.AsyncCompletion;


public class IndelibleFSForkProxy implements IndelibleFSForkIF
{
	private IndelibleFSServerConnectionProxy connection;
	private IndelibleFSForkRemote remote;
	private String name;
	public IndelibleFSForkProxy(IndelibleFSServerConnectionProxy connection, IndelibleFSForkRemote remote) throws RemoteException
	{
		this.connection = connection;
		this.remote = remote;
		name = remote.getName();
	}
	
	protected IndelibleFSForkRemote getRemoteFSFork()
	{
		return remote;
	}
	
	@Override
	public CASIDDataDescriptor getDataDescriptor(long offset, long length)
			throws IOException
	{
		return getRemoteFSFork().getDataDescriptor(offset, length);
	}

	@Override
	public int read(long offset, byte[] destBuffer) throws IOException
	{
		return read(offset, destBuffer, 0, destBuffer.length);
	}

	@Override
	public int read(long offset, byte[] destBuffer, int destBufferOffset,
			int length) throws IOException
	{
		ByteBuffer readBuffer = ByteBuffer.wrap(destBuffer, destBufferOffset, length).slice();	// Slice so that our position will be 0, even if there was an offset into the buffer
		return read(offset, readBuffer);
	}

	
	@Override
	public int read(long offset, ByteBuffer readBuffer) throws IOException
	{
		int length = readBuffer.limit() - readBuffer.position();
		CASIDDataDescriptor remoteDescriptor = getDataDescriptor(offset, length);
		return remoteDescriptor.getData(readBuffer, 0, length, true);
	}

	@Override
	public void writeDataDescriptor(long offset, CASIDDataDescriptor source)
			throws IOException
	{
		NetworkDataDescriptor writeDescriptor = connection.registerDataDescriptor(source);
		try
		{
			getRemoteFSFork().writeDataDescriptor(offset, writeDescriptor);
		}
		finally
		{
			connection.removeDataDescriptor(writeDescriptor);
			if (writeDescriptor != source)
				writeDescriptor.close();
		}
	}

	@Override
	public void write(long offset, byte[] source) throws IOException
	{
		write(offset, source, 0, source.length);
	}

	@Override
	public void write(long offset, byte[] source, int sourceOffset, int length)
			throws IOException
	{
		CASIDMemoryDataDescriptor writeDescriptor = new CASIDMemoryDataDescriptor(source, sourceOffset, length);
		writeDataDescriptor(offset, writeDescriptor);
	}

	
	@Override
	public void write(long offset, ByteBuffer writeBuffer) throws IOException
	{
		CASIDMemoryDataDescriptor writeDescriptor = new CASIDMemoryDataDescriptor(writeBuffer);
		writeDataDescriptor(offset, writeDescriptor);
	}

	@Override
	public void appendDataDescriptor(CASIDDataDescriptor source)
			throws IOException
	{
		NetworkDataDescriptor appendDescriptor = connection.registerDataDescriptor(source);
		try
		{
			getRemoteFSFork().appendDataDescriptor(appendDescriptor);
		}
		finally
		{
			connection.removeDataDescriptor(appendDescriptor);
			if (appendDescriptor != source)
				appendDescriptor.close();	// If we made our own network descriptor, close it
		}
	}

	@Override
	public void append(byte[] source) throws IOException
	{
		CASIDMemoryDataDescriptor appendDescriptor = new CASIDMemoryDataDescriptor(source);
		appendDataDescriptor(appendDescriptor);
	}

	@Override
	public void append(byte[] source, int sourceOffset, int length)
			throws IOException
	{
		CASIDMemoryDataDescriptor appendDescriptor = new CASIDMemoryDataDescriptor(source);
		appendDataDescriptor(appendDescriptor);
	}

	@Override
	public void flush() throws IOException
	{
		getRemoteFSFork().flush();
	}

	@Override
	public long truncate(long truncateLength) throws IOException
	{
		return getRemoteFSFork().truncate(truncateLength);
	}

	@Override
	public long extend(long extendLength) throws IOException
	{
		return getRemoteFSFork().extend(extendLength);
	}

	@Override
	public long length() throws IOException
	{
		return getRemoteFSFork().length();
	}

	@Override
	public CASIdentifier[] getSegmentIDs() throws IOException
	{
		return getRemoteFSFork().getSegmentIDs();
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Future<CASIDDataDescriptor> getDataDescriptorAsync(long offset,
			long length) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A> void getDataDescriptorAsync(long offset, long length,
			AsyncCompletion<Void, A> completionHandler, A attachment)
			throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Future<Void> writeDataDescriptorAsync(long offset,
			CASIDDataDescriptor source) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A> void writeDataDescriptorAsync(long offset,
			CASIDDataDescriptor source,
			AsyncCompletion<Void, A> completionHandler, A attachment)
			throws IOException
	{
		// TODO Auto-generated method stub
		
	}
}
