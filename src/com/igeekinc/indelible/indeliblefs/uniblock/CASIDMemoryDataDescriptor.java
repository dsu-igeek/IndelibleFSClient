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
 
package com.igeekinc.indelible.indeliblefs.uniblock;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.igeekinc.util.SHA1HashID;
import com.igeekinc.util.datadescriptor.BasicDataDescriptor;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

class GeneratorManager
{
	public static final int kGenerateQueueSize = 32;
    private LinkedBlockingQueue<QueuedDescriptor>generateQueue = new LinkedBlockingQueue<QueuedDescriptor>(kGenerateQueueSize);
    private ArrayList<CASIDGeneratorRunnable>generatorRunnables = new ArrayList<CASIDGeneratorRunnable>();
    private int maxGeneratorThreads = 0;
    
    public GeneratorManager()
    {
    	
    }
    
    public void setMaxGeneratorThreads(int maxGeneratorThreads)
    {
    	this.maxGeneratorThreads = maxGeneratorThreads;
    }
    
    void generate(QueuedDescriptor putDescriptor)
    {
    	boolean queued = false;
    	while (!queued)
    	{
    		synchronized(this)
    		{
    			if (maxGeneratorThreads == 0)	// Don't run in the background
    			{
    				StopWatch generateCASIDWatch = new Log4JStopWatch(getClass().getName()+"generateCASID");
    				putDescriptor.getDescriptorToGenerate().generateCASID(putDescriptor.getLength());
    				generateCASIDWatch.stop();
    				return;
    			}
    			else
    			{
    				if (generatorRunnables.size() == 0 || ((generateQueue.size() > kGenerateQueueSize/2) && (generatorRunnables.size() < maxGeneratorThreads)))
    				{
    					CASIDGeneratorRunnable generatorRunnable = new CASIDGeneratorRunnable(this);
    					Thread generatorThread = new Thread(generatorRunnable, "CASID Generator");
    					generatorRunnables.add(generatorRunnable);
    					generatorThread.start();
    				}
    			}


    			try
    			{
    				if (generateQueue.offer(putDescriptor, 10, TimeUnit.MILLISECONDS))
    					queued = true;	// Queue was not full
    			} catch (InterruptedException e)
    			{
    				// TODO Auto-generated catch block
    				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			}
    		}
    	}
    }
    
    /**
     * polls the queue.  If the thread should exit, null will be returned
     * @param timeout
     * @param timeUnit
     * @return
     */
    QueuedDescriptor next(CASIDGeneratorRunnable runnable)
    {
    	boolean exit = false;
    	while (!exit)
    	{
    		try
			{
				QueuedDescriptor returnDescriptor = generateQueue.poll(10, TimeUnit.MILLISECONDS);
				if (returnDescriptor != null)
					return returnDescriptor;
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
    		synchronized(this)
    		{
    			if (generateQueue.size() == 0)
    			{
    				exit = true;
    				generatorRunnables.remove(runnable);
    			}
    		}
    	}
    	return null;
    }
}
class CASIDGeneratorRunnable implements Runnable
{
	private final GeneratorManager manager;
	public CASIDGeneratorRunnable(GeneratorManager manager)
	{
		this.manager = manager;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			QueuedDescriptor processDescriptor = manager.next(this);
			if (processDescriptor == null)
				break;	// We need to exit
			try
			{
				StopWatch generateCASIDWatch = new Log4JStopWatch(getClass().getName()+"generateCASID");
				processDescriptor.getDescriptorToGenerate().generateCASID(processDescriptor.getLength());
				generateCASIDWatch.stop();
			}
			catch(Throwable t)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), t);
			}
		}
	}
	
}

class QueuedDescriptor
{
	private CASIDMemoryDataDescriptor descriptorToGenerate;
	private int length;
	public QueuedDescriptor(CASIDMemoryDataDescriptor descriptorToGenerate, int length)
	{
		this.descriptorToGenerate = descriptorToGenerate;
		this.length = length;
	}
	
	CASIDMemoryDataDescriptor getDescriptorToGenerate()
	{
		return descriptorToGenerate;
	}
	
	int getLength()
	{
		return length;
	}
}

public class CASIDMemoryDataDescriptor extends BasicDataDescriptor implements
        CASIDDataDescriptor
{
    private static final long serialVersionUID = 3652851741243637707L;
    private static final GeneratorManager generatorManager = new GeneratorManager();
    
    private CASIdentifier id;
    
    public static void setMaxGeneratorThreads(int maxGeneratorThreads)
    {
    	generatorManager.setMaxGeneratorThreads(maxGeneratorThreads);
    }
    
    /**
     * 
     * @param inData
     */
    public CASIDMemoryDataDescriptor(byte [] inData)
    {
        this(inData, 0, inData.length);
    }
    
    public CASIDMemoryDataDescriptor(byte [] inData, int offset, int length)
    {
        super(inData, offset, length);
    }
    
    public CASIDMemoryDataDescriptor(ByteBuffer buffer)
    {
    	super(buffer);
     	initCASID();
    }
    
    public CASIDMemoryDataDescriptor(InputStream inputStream, int length)
    throws IOException
    {
        super(inputStream, length);
        initCASID();
    }
    
    public static final int kPageSize = 1024*1024;	// Most places
    
    public CASIDMemoryDataDescriptor(FileChannel inputChannel, long position, int length) throws IOException
    {
    	super(inputChannel, position, length);
        if (getLength() != length)  // Check here for correct length.  BasicDataDescriptor will set the length to the number
            // of bytes actually read, which would be wrong for the CASIDEntifier
        	throw new IOException("Only read "+getLength()+" from input stream, not expected "+length+" bytes");
    	initCASID();
    }
    public CASIDMemoryDataDescriptor(DataDescriptor sourceDescriptor, long offset, int length) throws IOException
    {
        super(sourceDescriptor, offset, length);
        initCASID();
    }
    
    /**
     * This should only be used when the CASIdentifier is definitely the correct value for the data
     * @param cas
     * @param data
     * @throws IOException
     */
    public CASIDMemoryDataDescriptor(CASIdentifier cas, ByteBuffer data)
    throws IOException
    {
        super(data);
        id = cas;
    }
    
    protected CASIDMemoryDataDescriptor(CASIdentifier cas, InputStream inputStream, int length)
    throws IOException
    {
        super(inputStream, length);
        if (getLength() != length)  // Check here for correct length.  BasicDataDescriptor will set the length to the number
                                    // of bytes actually read, which would be wrong for the CASIDEntifier
            throw new IOException("Only read "+getLength()+" from input stream, not expected "+length+" bytes");
        id = cas;
    }
    
    protected CASIDMemoryDataDescriptor(CASIdentifier cas, FileInputStream inputStream, int length)
    throws IOException
    {
        super(inputStream, length);
        if (getLength() != length)  // Check here for correct length.  BasicDataDescriptor will set the length to the number
                                    // of bytes actually read, which would be wrong for the CASIDEntifier
            throw new IOException("Only read "+getLength()+" from input stream, not expected "+length+" bytes");
        id = cas;
    }
    
    protected void init(byte [] inData, int offset, int length)
    {
        super.init(inData, offset, length);
        initCASID();
    }
    
    /**
     * Protected constructor to make a BasicDataDescriptor for
     * some data that has already had its checksum computed.
     * Implemented to allow other packages to create BasicDataDescriptors
     * without having it recaculate the CASID
     * @param inData
     * @param inID
     */
    protected CASIDMemoryDataDescriptor(byte [] inData, CASIdentifier inID)
    {
        super(inData);
        id = inID;
    }
    
    /**
     * @param length
     * @throws InternalError
     */
    private void initCASID() throws InternalError 
    {
    	// The generatorManager may either invoice generateCASID immediately or queue it to
    	// a background thread, depending on how it is configured
    	generatorManager.generate(new QueuedDescriptor(this, (int)getLength()));
    }
    
    public synchronized CASIdentifier getCASIdentifier()
    {
    	while (id == null)
    	{
    		try
			{
				this.wait();
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
    	}
        return id;
    }
    
    synchronized void generateCASID(int length)
    {
    	if (id != null)
    		throw new IllegalStateException("CASID was already generated");
        SHA1HashID sha512 = new SHA1HashID(getByteBuffer());
        id = new CASIdentifier(sha512, length);
        this.notify();
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException
    {
    	getCASIdentifier();	// Make sure the CAS ID has been generated!
    	out.defaultWriteObject();	// Go ahead and write us the regular way
    }
}
