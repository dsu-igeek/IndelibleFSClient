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

import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.util.BitTwiddle;

public class OpenSessionCommand extends DataMoverCommand
{
    public static final int kCommandSize = kCommandHeaderSize + ObjectID.kTotalBytes + 4 /* Session authentication length */;
    private DataMoverSessionID openSessionID;
    public OpenSessionCommand(DataMoverSessionID openSessionID, SessionAuthentication sessionAuthentication)
    {
        super(DataMoverSource.kOpenSession);
        if (!openSessionID.equals(sessionAuthentication.getSessionID()))
        	throw new IllegalArgumentException("openSessionID "+openSessionID+" does not match session authentication sessionID "+sessionAuthentication.getSessionID());
        this.openSessionID = openSessionID;
        byte [] sessionAuthenticationBytes = sessionAuthentication.toBytes();
        commandBuf = new byte[kCommandSize + sessionAuthenticationBytes.length];
        
        writeCommandHeader();
        int commandBufOffset = kCommandHeaderSize;
        openSessionID.getBytes(commandBuf, commandBufOffset);
        commandBufOffset += ObjectID.kTotalBytes;
        BitTwiddle.intToJavaByteArray(sessionAuthenticationBytes.length, commandBuf, commandBufOffset);
        commandBufOffset += 4;
        System.arraycopy(sessionAuthenticationBytes, 0, commandBuf, commandBufOffset, sessionAuthenticationBytes.length);
    }

    @Override
    public int expectedOKBytes()
    {
        return 8;
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
            commandCheckOK = true;
        }
        if (!commandCheckOK)
        {
            
        }
    }
    
}