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
package com.igeekinc.indelible.indeliblefs.datamover;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.firehose.AuthenticatedFirehoseClient;
import com.igeekinc.indelible.indeliblefs.security.AuthenticatedTargetSSLSetup;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;


public class DataMoverClient extends AuthenticatedFirehoseClient
{
	public enum DataMoverCommand
	{
		kOpenSession(1),
	    kRequestSend(2),
	    kFinished(3),
	    kCheckConnection(4);
	    
		int commandNum;
		private DataMoverCommand(int commandNum)
		{
			this.commandNum = commandNum;
		}
		
		public int getCommandNum()
		{
			return commandNum;
		}
		
		public static DataMoverCommand getCommandForNum(int num)
		{
			switch(num)
			{
			case 1:
				return kOpenSession;
			case 2:
				return kRequestSend;
			case 3:
				return kFinished;
			case 4:
				return kCheckConnection;
			}
			throw new IllegalArgumentException();
		}
	}

	public DataMoverClient(AuthenticatedTargetSSLSetup sslSetup)
	{
		super(sslSetup);
	}
	
	public DataMoverClient(SocketAddress address) throws IOException
	{
		super(address);
	}
	
	public DataMoverClient(SocketAddress [] address) throws IOException
	{
		super(address);
	}
	
	@Override
	protected Class<? extends Object> getReturnClassForCommandCode(int commandCode)
	{
		switch(DataMoverCommand.getCommandForNum(commandCode))
		{
		case kCheckConnection:
			return Void.class;
		case kFinished:
			return Void.class;
		case kOpenSession:
			return Void.class;
		case kRequestSend:
			return DataRequestReply.class;
		default:
			break;
		
		}
		return null;
	}

	public void openSession(DataMoverSessionID openSessionID, SessionAuthentication sessionAuthentication) throws IOException
	{
		Future<Void> future = openSessionAsync(openSessionID, sessionAuthentication);
		try
		{
			future.get();
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
			{
				throw (IOException)e.getCause();
			}
			logger.error(new ErrorLogMessage("Got unexpected exception waiting for future"), e.getCause());
			throw new InternalError("Got unexpected exception "+e.getMessage());
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	public Future<Void> openSessionAsync(DataMoverSessionID openSessionID, SessionAuthentication sessionAuthentication) throws IOException
	{
		OpenSessionFuture future = new OpenSessionFuture();
		OpenSessionMessage openSessionMessage = new OpenSessionMessage(openSessionID, sessionAuthentication);
		sendMessage(openSessionMessage, future, null);
		return future;
	}
	public void getDataFromDescriptorAsync(DataMoverSessionID sessionID, NetworkDataDescriptor sourceDescriptor, ByteBuffer destination, long srcOffset, int length, int flags, boolean release,
    		DataRequestFuture future) throws IOException, AuthenticationFailureException
    {
		DataRequestMessage requestMessage = new DataRequestMessage(sessionID, sourceDescriptor.getID(), srcOffset, length, flags);
		sendMessage(requestMessage, destination, future, null);
    }
}

class DataMoverInternalAsyncCompletionHandler implements AsyncCompletion<DataRequestReply, Void>
{

	@Override
	public void completed(DataRequestReply result, Void attachment)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void failed(Throwable exc, Void attachment)
	{
		// TODO Auto-generated method stub
		
	}
	
}
class DataMoverInternalFuture extends ComboFutureBase<DataRequestReply>
{

	public DataMoverInternalFuture(DataRequestFuture externalFuture)
	{
		
	}
	@Override
	public synchronized void completed(DataRequestReply result,
			Object attachment)
	{
		// TODO Auto-generated method stub
		super.completed(result, attachment);
	}
	
	
}