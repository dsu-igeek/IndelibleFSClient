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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.NetworkDataDescriptorID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.EthernetID;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.datadescriptor.DataDescriptorInputStream;
import com.igeekinc.util.logging.ErrorLogMessage;

class NetworkDataDescriptorFuture extends ComboFutureBase<Integer>
{
	private NetworkDataDescriptor descriptor;
	private boolean release;
	
	public NetworkDataDescriptorFuture(NetworkDataDescriptor descriptor, boolean release, int length)
	{
		this.descriptor = descriptor;
		this.release = release;
		this.value = length;
	}
	
	public <A>NetworkDataDescriptorFuture(NetworkDataDescriptor descriptor, boolean release, AsyncCompletion<Integer, ? super A>completionHandler, A attachment)
	{
		super(completionHandler, attachment);
		this.descriptor = descriptor;
		this.release = release;
	}

	@Override
	protected synchronized void setDone()
	{
		super.setDone();
		if (release)
			descriptor.setClosed();
	}
}

class NetworkDataDescriptorCompletionHandler implements AsyncCompletion<Integer, Void>
{
	private NetworkDataDescriptorFuture nddFuture;
	private int length;
	public NetworkDataDescriptorCompletionHandler(NetworkDataDescriptorFuture nddFuture, int length)
	{
		if (nddFuture == null)
			throw new IllegalArgumentException("Must supply a NetworkDataDescriptorFuture");
		this.nddFuture = nddFuture;
		this.length = length;
	}
	@Override
	public void completed(Integer result, Void attachment)
	{
		if (result == null)
			throw new IllegalArgumentException("Result cannot be null");
		nddFuture.completed(result, null);
	}

	@Override
	public void failed(Throwable exc, Void attachment)
	{
		nddFuture.failed(exc, null);
	}
	
}
public class NetworkDataDescriptor implements Serializable, CASIDDataDescriptor
{
    private static final long serialVersionUID = 853163961770841937L;
    public static void initMapping()
    {
    	ObjectIDFactory.addMapping(NetworkDataDescriptor.class, NetworkDataDescriptorID.class);
    }
    NetworkDataDescriptorID id;
    long dataLength;
    EthernetID sourceID;	// We pass this around so we can figure out if we're on the same host or not
    InetSocketAddress [] hostPorts;
    EntityID serverID;
    EntityID securityServerID;
    DataMoverSessionID sessionID;
    CASIdentifier casIdentifier;
    DataDescriptor shareableDescriptor;
    File localSocket;
    
    private transient DataMoverReceiver moverClient;
    private transient boolean closed;
    private transient boolean local;
    
    public NetworkDataDescriptor(EntityID serverID, EntityID securityServerID, DataMoverSessionID sessionID, NetworkDataDescriptorID id, 
            CASIdentifier casIdentifier, long dataLength, EthernetID sourceID, InetSocketAddress [] hostPorts, File localSocket, DataDescriptor originalDescriptor,
            boolean local)
    {
        if (serverID == null)
            throw new IllegalArgumentException("ServerID cannot be null");
        if (securityServerID == null)
            throw new IllegalArgumentException("SecurityServerID cannot be null");
        if (sessionID == null)
            throw new IllegalArgumentException("SessionID cannot be null");
        if (id == null)
            throw new IllegalArgumentException("id cannot be null");

        this.serverID = serverID;
        this.securityServerID = securityServerID;
        this.sessionID = sessionID;
        this.id = id;
        this.casIdentifier = casIdentifier;
        this.dataLength = dataLength;
        this.sourceID = sourceID;
        this.hostPorts = new InetSocketAddress[hostPorts.length];
        System.arraycopy(hostPorts, 0, this.hostPorts, 0, hostPorts.length);
        this.localSocket = localSocket;
        this.shareableDescriptor = originalDescriptor;
        this.local = local;
    }

    public NetworkDataDescriptorID getID()
    {
        return id;
    }

    public EntityID getServerID()
    {
        return serverID;
    }

    public EthernetID getSourceID()
	{
		return sourceID;
	}

	public DataMoverSessionID getSessionID()
    {
        return sessionID;
    }

    public EntityID getSecurityServerID()
    {
        return securityServerID;
    }

    public long getDataLength()
    {
        return dataLength;
    }

    public static long getSerialversionuid()
    {
        return serialVersionUID;
    }

    public InetSocketAddress [] getHostPorts()
    {
    	InetSocketAddress [] returnHostPorts = new InetSocketAddress[hostPorts.length];
        System.arraycopy(hostPorts, 0, returnHostPorts, 0, hostPorts.length);
        return returnHostPorts;
    }
    
    public File getLocalSocket()
    {
    	return localSocket;
    }
    
    public DataMoverSessionID SessionID()
    {
        return sessionID;
    }

    public CASIdentifier getCASIdentifier()
    {
        return casIdentifier;
    }

    public byte[] getData() throws IOException
    {
        if (moverClient == null)
            moverClient = DataMoverReceiver.getDataMoverReceiver();  
        byte [] buffer = new byte[(int)getDataLength()];
        getData(buffer, 0, 0L, (int)getDataLength(), true);
        return buffer;
    }

    public ByteBuffer getByteBuffer() throws IOException
    {
        if (moverClient == null)
            moverClient = DataMoverReceiver.getDataMoverReceiver();  
        ByteBuffer buffer = ByteBuffer.allocate((int)getDataLength());
        getData(buffer, 0, (int)getDataLength(), true);
        return buffer;
    }
    
    @Override
    public int getData(byte[] destination, int destOffset, long srcOffset,
            int length, boolean release) throws IOException
    {
    	ByteBuffer destinationBuffer = ByteBuffer.wrap(destination, destOffset, length);
    	return getData(destinationBuffer, srcOffset, length, release);
    }

    
    @Override
	public int getData(ByteBuffer destination, long srcOffset, int length, boolean release) throws IOException, NoMoverPathException
	{
        if (length + srcOffset > dataLength)
            length = (int) (dataLength - srcOffset);
        if (length == 0 && !release)
            return 0;
        if (moverClient == null)
            moverClient = DataMoverReceiver.getDataMoverReceiver();  
        try
        {
            Future<Integer>getDataFuture = getDataAsync(destination, srcOffset, length, release);
            int returnBytesRead = getDataFuture.get();
            if (release)
            	setClosed();
			return returnBytesRead;
        } catch (ExecutionException e)
        {
        	if (e.getCause() instanceof IOException)
        		throw (IOException)e.getCause();
        	if (e.getCause() instanceof AuthenticationFailureException)
        	{
        		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        		throw new IOException("Authentication failed connecting to mover");
        	}
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Got unexpected exception"), e);
        	throw new IOException("Got unexpected exception "+e.getCause().toString());
        } catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Interrupted while waiting for results");
		}
	}

	@Override
	public Future<Integer> getDataAsync(ByteBuffer destination, long srcOffset,
			int length, boolean release) throws IOException
	{
		NetworkDataDescriptorFuture future = new NetworkDataDescriptorFuture(this, release, length);
		getDataAsyncCommon(destination, srcOffset, length, release, future);
		return future;
	}

	@Override
	public <A> void getDataAsync(ByteBuffer destination, long srcOffset,
			int length, boolean release, A attachment,
			AsyncCompletion<Integer, ? super A> handler) throws IOException
	{
		NetworkDataDescriptorFuture future = new NetworkDataDescriptorFuture(this, release, handler, attachment);
		getDataAsyncCommon(destination, srcOffset, length, release, future);
	}

	private void getDataAsyncCommon(ByteBuffer destination, long srcOffset,
			int length, boolean release, NetworkDataDescriptorFuture future) throws IOException
	{
        if (length + srcOffset > dataLength)
            length = (int) (dataLength - srcOffset);
        if (length == 0 && !release)
            return;
        if (moverClient == null)
            moverClient = DataMoverReceiver.getDataMoverReceiver();  
        try
        {
            moverClient.getDataFromDescriptorAsync(this, destination, srcOffset, length, release, new NetworkDataDescriptorCompletionHandler(future, length), null);
            if (release)
            	setClosed();
        } catch (AuthenticationFailureException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            throw new IOException("Authentication failed connecting to mover");
        }
	}
	public InputStream getInputStream() throws IOException
    {
        DataDescriptorInputStream returnStream = new DataDescriptorInputStream(this);
        return returnStream;
    }

    public long getLength()
    {
        return dataLength;
    }

    public void writeData(OutputStream destinationStream) throws IOException
    {
        if (moverClient == null)
            moverClient = DataMoverReceiver.getDataMoverReceiver();
        try
        {
            moverClient.writeDescriptorToStream(this, destinationStream);
        } catch (AuthenticationFailureException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            throw new IOException("Authentication failed connecting to mover");
        }
    }
    
    public void writeData(FileOutputStream destinationStream) throws IOException
    {
        if (moverClient == null)
            moverClient = DataMoverReceiver.getDataMoverReceiver();
        try
        {
            moverClient.writeDescriptorToStream(this, destinationStream);
        } catch (AuthenticationFailureException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            throw new IOException("Authentication failed connecting to mover");
        }
    }
    
    public void setMoverClient(DataMoverReceiver moverClient)
    {
        this.moverClient = moverClient;
    }
    
    public DataDescriptor getShareableDescriptor()
    {
        return shareableDescriptor;
    }
    
    public boolean isAccessible()
    {
        return true;    // Always accessible
    }

    public boolean isShareableWithLocalProcess()
    {
        return true;    // We can serialize and send this somewhere else
    }

    public boolean isShareableWithRemoteProcess()
    {
        return true;    // We can serialize and send this somewhere else
    }

    public boolean descriptorContainsData()
    {
        return false;
    }
    
    public void close()
    throws IOException
    {

    	if (shareableDescriptor != null)
    		shareableDescriptor.close();
    	if (!closed && !local)
    	{
    		byte [] releaseBuf = new byte[0];
    		getData(releaseBuf, 0, 0L, 0, true);
    		setClosed();
    	}
    }
    
    protected void setClosed()
    {
    	closed = true;
    }
    
    public String toString()
    {
    	StringBuffer buildBuffer = new StringBuffer("id = ");
    	buildBuffer.append(id.toString());
    	buildBuffer.append(", length = ");
    	buildBuffer.append(Long.toString(dataLength));
    	buildBuffer.append(", hostPorts={");
    	for (InetSocketAddress curAddress:getHostPorts())
    	{
    		buildBuffer.append(curAddress.toString());
    		buildBuffer.append(" ");
    	}
    	buildBuffer.append("}, localSocket = ");
    	if (localSocket != null)
    		buildBuffer.append(localSocket.toString());
    	else
    		buildBuffer.append("<none>");
    	return buildBuffer.toString();
    }

	@Override
	public void release()
	{
		try
		{
			getData(new byte[0], 0, 0, 0, true); // Release the remote resources
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}	
	}
}
