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
 
package com.igeekinc.indelible.indeliblefs.remote;

import java.io.IOException;
import java.io.OutputStream;

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;

public class IndelibleFSForkRemoteOutputStream extends OutputStream
{
    protected IndelibleFSForkIF outputFork;
    protected boolean closed = false;
    protected DataMoverSession session;
    
    public IndelibleFSForkRemoteOutputStream(IndelibleFSForkIF outputFork, boolean append, DataMoverSession session) throws IOException
    {
        this.outputFork = outputFork;
        if (!append)
            outputFork.truncate(0);
        this.session = session;
    }
    
    
    public void close() throws IOException
    {
        if (!closed)
        {
            flush();
            closed = true;
            outputFork = null;
        }
    }


    public void flush() throws IOException
    {
        if (closed)
            throw new IOException("Cannot flush closed output stream");
        outputFork.flush();
    }


    public void write(byte[] b, int off, int len) throws IOException
    {
        if (closed)
            throw new IOException("Cannot flush write to output stream");
        CASIDDataDescriptor writeDescriptor = new CASIDMemoryDataDescriptor(b, off, len);
        outputFork.appendDataDescriptor(writeDescriptor);
    }


    public void write(byte[] b) throws IOException
    {
        if (closed)
            throw new IOException("Cannot flush write to output stream");
        write(b, 0, b.length);
    }


    public void write(int b) throws IOException
    {
        if (closed)
            throw new IOException("Cannot flush write to output stream");
        write(new byte[]{(byte)b});
    }
}
