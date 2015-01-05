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
 
package com.igeekinc.indelible.indeliblefs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.util.async.AsyncCompletion;

public interface IndelibleFSForkIF
{

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#getDataDescriptor(long, long)
	 */
	public abstract CASIDDataDescriptor getDataDescriptor(long offset, long length) throws IOException;

	public abstract Future<CASIDDataDescriptor>getDataDescriptorAsync(long offset, long length) throws IOException;
	public abstract <A> void getDataDescriptorAsync(long offset, long length, AsyncCompletion<CASIDDataDescriptor, A>completionHandler, A attachment) throws IOException;
	
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#read(long, byte[])
	 */
	public abstract int read(long offset, byte[] bytesToRead)
			throws IOException;

	public abstract int read(long offset, byte[] destBuffer,
			int destBufferOffset, int len) throws IOException;

	public abstract int read(long offset, ByteBuffer readBuffer) throws IOException;
	
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#writeDataDescriptor(long, com.igeekinc.indelible.indeliblefs.datamover.DataDescriptor)
	 */
	public abstract void writeDataDescriptor(long offset,
			CASIDDataDescriptor source) throws IOException;

	public abstract Future<Void>writeDataDescriptorAsync(long offset, CASIDDataDescriptor source) throws IOException;
	public abstract <A> void writeDataDescriptorAsync(long offset, CASIDDataDescriptor source, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException;
	public abstract void write(long offset, ByteBuffer writeBuffer) throws IOException;
	
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#write(long, byte[])
	 */
	public abstract void write(long offset, byte[] source) throws IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#write(long, byte[], int, int)
	 */
	public abstract void write(long offset, byte[] source, int sourceOffset,
			int length) throws IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#appendDataDescriptor(com.igeekinc.indelible.indeliblefs.datamover.DataDescriptor)
	 */
	public abstract void appendDataDescriptor(CASIDDataDescriptor source)
			throws IOException;
	public abstract Future<Void>appendDataDescriptorAsync(CASIDDataDescriptor source) throws IOException;
	public abstract <A> void appendDataDescriptorAsync(CASIDDataDescriptor source, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException;
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#append(byte[])
	 */
	public abstract void append(byte[] source) throws IOException;
	
	public abstract void append(ByteBuffer source) throws IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#append(byte[], int, int)
	 */
	public abstract void append(byte[] source, int sourceOffset, int length)
			throws IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSForkRemote#flush()
	 */
	public abstract void flush() throws IOException;

	/**
	 * Truncates the fork to truncateLength.  If truncateLength >= the current fork length no action
	 * @param truncateValue
	 * @throws IOException
	 * @returns the new length
	 */
	public abstract long truncate(long truncateLength) throws IOException;

	/**
	 * Extends the fork to extendLength.  New data area will return zero's.  If exendLength is
	 * less then or equal to the current fork length no action
	 * @param extendLength
	 * @throws IOException
	 * @returns the new length
	 */
	public abstract long extend(long extendLength) throws IOException;

	public abstract long length() throws IOException;

	public abstract CASIdentifier[] getSegmentIDs() throws IOException;

	public abstract String getName() throws IOException;

}