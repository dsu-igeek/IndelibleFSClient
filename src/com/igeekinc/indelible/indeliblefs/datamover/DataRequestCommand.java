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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.NetworkDataDescriptorID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.logging.ErrorLogMessage;

public class DataRequestCommand extends DataMoverCommand
{
    public static final int kCommandSize = kCommandHeaderSize + ObjectID.kTotalBytes + ObjectID.kTotalBytes + 8 + 8 + 1;
    private DataMoverSessionID sessionID;
    private NetworkDataDescriptorID descriptorID;
    private long offset, bytesToRead;
    private ByteBuffer destBuffer;
    public static final byte kReleaseDescriptor = 0x01;
    private Logger logger = Logger.getLogger(getClass());
    public DataRequestCommand(DataMoverSessionID sessionID, NetworkDataDescriptorID descriptorID, long offset, long bytesToRead, byte flags, byte [] destBuffer, int destBufferOffset)
    {
    	this(sessionID, descriptorID, offset, bytesToRead, flags, ByteBuffer.wrap(destBuffer, destBufferOffset, (int)bytesToRead));
    }
    
    public DataRequestCommand(DataMoverSessionID sessionID, NetworkDataDescriptorID descriptorID, long offset, long bytesToRead, byte flags, ByteBuffer destBuffer)
    {
        super(DataMoverSource.kRequestSend);
        init(sessionID, descriptorID, offset, bytesToRead, flags, destBuffer);
    }

    public <A> DataRequestCommand(DataMoverSessionID sessionID, NetworkDataDescriptorID descriptorID, long offset, long bytesToRead, byte flags, 
    		ByteBuffer destBuffer, MoverFuture future)
    {
    	super(DataMoverSource.kRequestSend, future);
    	init(sessionID, descriptorID, offset, bytesToRead, flags, destBuffer);
    }
    
	private void init(DataMoverSessionID sessionID,
			NetworkDataDescriptorID descriptorID, long offset,
			long bytesToRead, byte flags, ByteBuffer destBuffer)
	{
		this.sessionID = sessionID;
        this.descriptorID = descriptorID;
        this.offset = offset;
        this.bytesToRead = bytesToRead;
        this.destBuffer = destBuffer.duplicate();
        commandBuf = new byte[kCommandSize];
        writeCommandHeader();
        int commandBufOffset = kCommandHeaderSize;
        this.sessionID.getBytes(commandBuf, commandBufOffset);
        commandBufOffset += ObjectID.kTotalBytes;
        descriptorID.getBytes(commandBuf, commandBufOffset);
        commandBufOffset += ObjectID.kTotalBytes;
        BitTwiddle.longToJavaByteArray(offset, commandBuf, commandBufOffset);
        commandBufOffset += 8;
        BitTwiddle.longToJavaByteArray(bytesToRead, commandBuf, commandBufOffset);
        commandBufOffset += 8;
        commandBuf[commandBufOffset] = flags;
	}
    
    
    @Override
    public int expectedOKBytes()
    {
        return 8 + ObjectID.kTotalBytes + 8 + 8 + 1;
    }
    @Override
    public void processOKResultInternal(byte[] okBytes, InputStream inputStream)
    throws IOException
    {
        int okBufOffset = 0;
        long checkCommandNum = BitTwiddle.javaByteArrayToLong(okBytes, okBufOffset);
        boolean commandCheckOK = false;
        if (checkCommandNum == commandNum)
        {
            okBufOffset += 8;
            if (okBytes[okBufOffset] == DataMoverSource.kDataSend)
            {
                okBufOffset ++;
                NetworkDataDescriptorID checkID = (NetworkDataDescriptorID) ObjectIDFactory.reconstituteFromBytes(okBytes, okBufOffset, okBytes.length - okBufOffset);
                if (checkID.equals(descriptorID))
                {
                    okBufOffset += ObjectID.kTotalBytes;
                    long checkOffset = BitTwiddle.javaByteArrayToLong(okBytes, okBufOffset);
                    if (checkOffset == offset)
                    {
                        okBufOffset += 8;
                        long checkBytesToRead = BitTwiddle.javaByteArrayToLong(okBytes, okBufOffset);
                        if (checkBytesToRead == bytesToRead)
                        {
                            commandCheckOK = true;
                            if (destBuffer != null)
                            {
                                long bytesRemainingToRead = bytesToRead;
                                int startPos = destBuffer.position();
                                if (inputStream instanceof FileInputStream)
                                {
                                	FileChannel readChannel = ((FileInputStream)inputStream).getChannel();
                                	ByteBuffer readBuffer = destBuffer.duplicate();
                                	while (bytesRemainingToRead > 0)
                                	{
                                		int bytesRead = readChannel.read(readBuffer);
                                		bytesRemainingToRead -= bytesRead;
                                	}
                                }
                                else
                                {
                                	int curBufferOffset;	// Where to read into the temp buffer
                                	byte [] readBuffer;
                                	boolean copyBack = false;
                                	if (destBuffer.hasArray())
                                	{
                                		curBufferOffset = destBuffer.position();
                                		readBuffer = destBuffer.array();
                                	}
                                	else
                                	{
                                		curBufferOffset = 0;	// We're going to start at the beginning of the data buffer
                                		readBuffer = new byte[(int)bytesToRead];
                                		copyBack = true;
                                	}
                                	while (bytesRemainingToRead > 0)
                                	{
                                		int bytesToReadNow = 0;
                                		if (bytesRemainingToRead > Integer.MAX_VALUE)
                                			bytesToReadNow = Integer.MAX_VALUE;
                                		else
                                			bytesToReadNow = (int)bytesRemainingToRead;
                                		int bytesRead = inputStream.read(readBuffer, curBufferOffset, bytesToReadNow);
                                		bytesRemainingToRead -= bytesRead;
                                		curBufferOffset += bytesRead;
                                	}
                                	
                                	if (copyBack)
                                	{
                                		destBuffer.put(readBuffer);
                                	}
                                	else
                                	{
                                		destBuffer.position(destBuffer.position() + (int)bytesToRead);
                                	}
                                }
                                CRC32 computedCRC = new CRC32();
                                ByteBuffer crcCheckBuffer = destBuffer.duplicate();
                                crcCheckBuffer.position(startPos);
                                int bytesRemaining = (int)bytesToRead;
                                while (bytesRemaining > 0)
                                {
                                	computedCRC.update(crcCheckBuffer.get());
                                	bytesRemaining --;
                                }
                                byte [] crcBuf = new byte[8];
                                for (int crcByteNum = 0; crcByteNum < crcBuf.length; crcByteNum++)
                                {
                                	int curByte = inputStream.read();
                                	if (curByte < 0)
                                	{
                                    	logger.error(new ErrorLogMessage("Did not receive full CRC"));
                                    	throw new IOException("Did not receive full CRC");
                                	}
                                	crcBuf[crcByteNum] = (byte)(curByte & 0xff);
                                }

                                long receivedCRCLong = BitTwiddle.javaByteArrayToLong(crcBuf, 0);
                                long computedCRCLong = computedCRC.getValue();
                                if (receivedCRCLong != computedCRCLong)
                                {
                                	throw new IOException("Got a bad CRC value, expected "+computedCRCLong+" for "+bytesToRead+" bytes, received "+receivedCRCLong);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!commandCheckOK)
        {
            // Throw a nice exception here and get the connection closed down
        	logger.error(new ErrorLogMessage("Did not get good command"));
        	throw new IOException("Did not get good command");
        }
    }
    
    
}