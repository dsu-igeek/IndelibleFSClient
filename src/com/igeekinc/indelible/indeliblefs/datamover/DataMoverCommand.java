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

import java.io.IOException;
import java.io.InputStream;

import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.async.AsyncCompletion;

public abstract class DataMoverCommand
{
    byte command;
    long commandNum;
    byte [] commandBuf;
    boolean finished;
    MoverFuture future;
    
    public static final int kCommandHeaderSize = 9;
    public DataMoverCommand(byte command)
    {
        this.command = command;
        future = new MoverFuture();
    }

    public <A> DataMoverCommand(byte command, MoverFuture future)
    {
        this.command = command;
        this.future = future;
    }
    
    public MoverFuture getFuture()
    {
    	return future;
    }
    
    public long getCommandNum()
    {
        return commandNum;
    }

    public void setCommandNum(long commandNum)
    {
        this.commandNum = commandNum;
        writeCommandHeader();
    }

    public byte getCommand()
    {
        return command;
    }
    
    public void writeCommandHeader()
    {
        commandBuf[0] = command;
        BitTwiddle.longToJavaByteArray(commandNum, commandBuf, 1);
    }
    
    public byte [] getCommandBuf()
    {
        return commandBuf;
    }
    
    public abstract int expectedOKBytes();
    
    public void processOKResult(byte [] okBytes, InputStream inputStream) throws IOException
    {
    	try
    	{
    		processOKResultInternal(okBytes, inputStream);
    		future.completed(null, future);
    	}
    	catch (Throwable t)
    	{
    		future.failed(t, future);
    	}
    }
    public abstract void processOKResultInternal(byte [] okBytes, InputStream inputStream) throws IOException;
    
    public synchronized void setFinished()
    {
    	if (!future.isDone())
    		future.completed(null, null);
    }
}
