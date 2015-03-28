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
package com.igeekinc.indelible.indeliblefs.firehose.proxies;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSForkHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerConnectionHandle;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSForkProxy implements IndelibleFSForkIF
{
	private final IndelibleFSFirehoseClient client;
	private final IndelibleFSServerConnectionProxy connection;
	private final IndelibleFSForkHandle handle;
	
	public IndelibleFSForkProxy(IndelibleFSFirehoseClient client, IndelibleFSServerConnectionProxy connection, IndelibleFSForkHandle handle)
	{
		this.client = client;
		this.connection = connection;
		this.handle = handle;
	}

	public IndelibleFSServerConnectionHandle getConnectionHandle()
	{
		return connection.getHandle();
	}
	
	public IndelibleFSServerConnectionProxy getConnection()
	{
		return connection;
	}
	@Override
	public CASIDDataDescriptor getDataDescriptor(long offset, long length)
			throws IOException
	{
		Future<CASIDDataDescriptor>future = getDataDescriptorAsync(offset, length);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public Future<CASIDDataDescriptor> getDataDescriptorAsync(long offset,
			long length) throws IOException
	{
		ComboFutureBase<CASIDDataDescriptor>returnFuture = new ComboFutureBase<CASIDDataDescriptor>();
		client.getDataDescriptorAsync(getConnection(), handle, offset, length, returnFuture, null);
		return returnFuture;
	}

	@Override
	public <A> void getDataDescriptorAsync(long offset, long length,
			AsyncCompletion<CASIDDataDescriptor, A> completionHandler, A attachment)
			throws IOException
	{
		client.getDataDescriptorAsync(getConnection(), handle, offset, length, completionHandler, attachment);
	}

	@Override
	public int read(long offset, byte[] bytesToRead) throws IOException
	{
		return read(offset, ByteBuffer.wrap(bytesToRead));
	}

	@Override
	public int read(long offset, byte[] destBuffer, int destBufferOffset,
			int len) throws IOException
	{
		return read(offset, ByteBuffer.wrap(destBuffer, destBufferOffset, len));
	}

	@Override
	public int read(long offset, ByteBuffer readBuffer) throws IOException
	{
		CASIDDataDescriptor dataDescriptor = getDataDescriptor(offset, readBuffer.remaining());
		return dataDescriptor.getData(readBuffer, 0, readBuffer.remaining(), true);
	}

	@Override
	public void writeDataDescriptor(long offset, CASIDDataDescriptor source)
			throws IOException
	{
		Future<Void>future = writeDataDescriptorAsync(offset, source);
		try
		{
			future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public Future<Void> writeDataDescriptorAsync(long offset,
			CASIDDataDescriptor source) throws IOException
	{
		ComboFutureBase<Void>writeDataFuture = new ComboFutureBase<Void>();
		writeDataDescriptorAsync(offset, source, writeDataFuture, null);
		return writeDataFuture;
	}

	@Override
	public <A> void writeDataDescriptorAsync(long offset,
			CASIDDataDescriptor source,
			AsyncCompletion<Void, A> completionHandler, A attachment)
			throws IOException
	{
		NetworkDataDescriptor nddSource = connection.registerDataDescriptor(source);
		ReleaseDataDescriptorCompletionHandler<A> releaseHandler = new ReleaseDataDescriptorCompletionHandler<A>(connection, completionHandler, nddSource);
		client.writeDataDescriptorAsync(getConnection(), handle, offset, nddSource, releaseHandler, attachment);
	}

	@Override
	public void write(long offset, ByteBuffer writeBuffer) throws IOException
	{
		CASIDDataDescriptor writeDescriptor = new CASIDMemoryDataDescriptor(writeBuffer);
		writeDataDescriptor(offset, writeDescriptor);
	}

	@Override
	public void write(long offset, byte[] source) throws IOException
	{
		write(offset, ByteBuffer.wrap(source));
	}

	@Override
	public void write(long offset, byte[] source, int sourceOffset, int length)
			throws IOException
	{
		write(offset, ByteBuffer.wrap(source, sourceOffset, length));
	}

	@Override
	public void appendDataDescriptor(CASIDDataDescriptor source)
			throws IOException
	{
		Future<Void>future = appendDataDescriptorAsync(source);
		try
		{
			future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public Future<Void> appendDataDescriptorAsync(
			CASIDDataDescriptor source) throws IOException
	{
		ComboFutureBase<Void>appendDataFuture = new ComboFutureBase<Void>();
		appendDataDescriptorAsync(source, appendDataFuture, null);
		return appendDataFuture;
	}

	class ReleaseDataDescriptorCompletionHandler<A> implements AsyncCompletion<Void, A>
	{
		private IndelibleFSServerConnectionProxy connection;
		private AsyncCompletion<Void, A>completionHandler;
		private NetworkDataDescriptor releaseDescriptor;

		public ReleaseDataDescriptorCompletionHandler(IndelibleFSServerConnectionProxy connection, 
				AsyncCompletion<Void, A>completionHandler, NetworkDataDescriptor releaseDescriptor)
		{
			this.connection = connection;
			this.completionHandler = completionHandler;
			this.releaseDescriptor = releaseDescriptor;
		}
		@Override
		public void completed(Void result, A attachment)
		{
			connection.removeDataDescriptor(releaseDescriptor);
			completionHandler.completed(null, attachment);
		}

		@Override
		public void failed(Throwable exc, A attachment)
		{
			connection.removeDataDescriptor(releaseDescriptor);
			completionHandler.failed(exc, attachment);
		}
		
	}
	@Override
	public <A> void appendDataDescriptorAsync(
			CASIDDataDescriptor source,
			AsyncCompletion<Void, A> completionHandler, A attachment)
			throws IOException
	{
		NetworkDataDescriptor nddSource = connection.registerDataDescriptor(source);
		ReleaseDataDescriptorCompletionHandler<A> releaseHandler = new ReleaseDataDescriptorCompletionHandler<A>(connection, completionHandler, nddSource);
		client.appendDataDescriptorAsync(getConnection(), handle, nddSource, releaseHandler, attachment);
	}

	@Override
	public void append(byte[] source) throws IOException
	{
		append(ByteBuffer.wrap(source));
	}

	@Override
	public void append(byte[] source, int sourceOffset, int length)
			throws IOException
	{
		append(ByteBuffer.wrap(source, sourceOffset, length));
	}

	@Override
	public void append(ByteBuffer source) throws IOException
	{
		CASIDDataDescriptor appendDescriptor = new CASIDMemoryDataDescriptor(source);
		appendDataDescriptor(appendDescriptor);
	}

	@Override
	public void flush() throws IOException
	{
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		client.flushAsync(getConnection(), handle, future, null);
		try
		{
			future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public long truncate(long truncateLength) throws IOException
	{
		ComboFutureBase<Long>future = new ComboFutureBase<Long>();
		client.truncateAsync(getConnection(), handle, truncateLength, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public long extend(long extendLength) throws IOException
	{
		ComboFutureBase<Long>future = new ComboFutureBase<Long>();
		client.extendAsync(getConnection(), handle, extendLength, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public long length() throws IOException
	{
		ComboFutureBase<Long>future = new ComboFutureBase<Long>();
		client.lengthAsync(getConnection(), handle, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public CASIdentifier[] getSegmentIDs() throws IOException
	{
		ComboFutureBase<CASIdentifier[]>future = new ComboFutureBase<CASIdentifier[]>();
		client.getSegmentIDsAsync(getConnection(), handle, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

	@Override
	public String getName() throws IOException
	{
		ComboFutureBase<String>future = new ComboFutureBase<String>();
		client.getName(getConnection(), handle, future, null);
		try
		{
			return future.get();
		} catch (InterruptedException e)
		{
			throw new IOException("Operation was interrupted");
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected error "+e.getCause().toString());
		}
	}

}
