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
import java.util.HashMap;

import com.igeekinc.firehose.CommandMessage;
import com.igeekinc.firehose.CommandResult;
import com.igeekinc.firehose.CommandToProcess;
import com.igeekinc.firehose.FirehoseChannel;
import com.igeekinc.firehose.FirehoseTarget;
import com.igeekinc.firehose.SSLFirehoseChannel;
import com.igeekinc.indelible.indeliblefs.firehose.AuthenticatedFirehoseServer;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.NetworkDataDescriptorID;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

class DataMoverClientInfo
{
	private int channelNum;
	private HashMap<DataMoverSessionID, DataMoverSession>openSessions = new HashMap<DataMoverSessionID, DataMoverSession>();
	
	public DataMoverClientInfo(int channelNum)
	{
		this.channelNum = channelNum;
	}
	
	public synchronized DataMoverSession addOpenSession(DataMoverSessionID sessionID, DataMoverSession openSession)
	{
		return openSessions.put(sessionID, openSession);
	}
	
	public synchronized DataMoverSession removeOpenSession(DataMoverSessionID removeSessionID)
	{
		return openSessions.remove(removeSessionID);
	}
	
	public synchronized DataMoverSession getOpenSession(DataMoverSessionID sessionID)
	{
		return openSessions.get(sessionID);
	}
	
	public int getChannelNum()
	{
		return channelNum;
	}
	
	public String toString()
	{
		String returnString = "Channel: "+channelNum+" - Open Sessions: ";
		synchronized(this)
		{
			if (openSessions.size() > 0)
			{
				for (DataMoverSessionID curSessionID:openSessions.keySet())
				{
					returnString = returnString + curSessionID.toString()+" ";
				}
			}
			else
			{
				returnString = returnString + "<none>";
			}
		}
		return returnString;
	}
}

public class DataMoverServer extends AuthenticatedFirehoseServer<DataMoverClientInfo>
{
	private DataMoverSource dataMoverSource;
	
	public DataMoverServer(DataMoverSource dataMoverSource)
	{
		super();
		this.dataMoverSource = dataMoverSource;
	}

	@Override
	public Thread createSelectLoopThread(Runnable selectLoopRunnable)
	{
		return new Thread(selectLoopRunnable, "DataMoverServer select loop");
	}

	@Override
	protected Class<? extends CommandMessage> getClassForCommandCode(int commandCode)
	{
		switch(DataMoverClient.DataMoverCommand.getCommandForNum(commandCode))
		{
		case kCheckConnection:
			break;
		case kFinished:
			break;
		case kOpenSession:
			return OpenSessionMessage.class;
		case kRequestSend:
			return DataRequestMessage.class;
		}
		return null;
	}

	@Override
	protected void processCommand(DataMoverClientInfo clientInfo, CommandToProcess commandToProcess)
			throws Exception
	{
		EntityAuthentication [] clientAuthentications = getAuthenticatedClients((SSLFirehoseChannel) commandToProcess.getChannel());
		int commandCode = commandToProcess.getCommandToProcess().getCommandCode();
		CommandResult result = null;
		switch(DataMoverClient.DataMoverCommand.getCommandForNum(commandCode))
		{
		case kCheckConnection:
			break;
		case kFinished:
			break;
		case kOpenSession:
		{
			OpenSessionMessage openSessionMessage = (OpenSessionMessage)commandToProcess.getCommandToProcess();
			result = openSession(clientInfo, clientAuthentications,
					openSessionMessage);
			break;
		}
		case kRequestSend:
			DataRequestMessage dataRequestMessage = (DataRequestMessage)commandToProcess.getCommandToProcess();
			NetworkDataDescriptorID dataDescriptorID = dataRequestMessage.getDescriptorID().getDataDescriptorID();
			DataMoverSessionID sessionID = dataRequestMessage.getSessionID().getDataMoverSessionID();
			long offset = dataRequestMessage.getOffset();
			int length = dataRequestMessage.getBytesToRead();
			int flags = dataRequestMessage.getFlags();
			result = dataRequest(clientInfo, sessionID, dataDescriptorID, offset, length, flags);
			break;
		default:
			break;
		}
		commandCompleted(commandToProcess, result);
	}

	private CommandResult openSession(DataMoverClientInfo clientInfo,
			EntityAuthentication[] clientAuthentications,
			OpenSessionMessage openSessionMessage)
	{
		CommandResult result;
		dataMoverSource.openSession(clientInfo, clientAuthentications, openSessionMessage.getOpenSessionID(), openSessionMessage.getSessionAuthentication());
		result = new CommandResult(0, null);
		return result;
	}

	private CommandResult dataRequest(DataMoverClientInfo clientInfo, DataMoverSessionID sessionID,
			NetworkDataDescriptorID dataDescriptorID, long offset, int length, int flags) throws IOException
	{
		CommandResult result;

		DataDescriptor bulkData = dataMoverSource.requestSend(clientInfo, sessionID, 
				dataDescriptorID, flags);
		DataRequestReply reply = new DataRequestReply(dataDescriptorID, offset, length);
		result = new CommandResult((int)bulkData.getLength(), reply, bulkData, offset, length);
		return result;
	}

	@Override
	protected Class<? extends Object> getReturnClassForCommandCode(int commandCode)
	{
		switch(DataMoverClient.DataMoverCommand.getCommandForNum(commandCode))
		{
		case kCheckConnection:
			return Void.class;
		case kFinished:
			return Void.class;
		case kOpenSession:
			return Void.class;
		case kRequestSend:
			return DataRequestReply.class;
		}
		return null;
	}

	@Override
	protected DataMoverClientInfo createClientInfo(FirehoseChannel channel)
	{
		DataMoverClientInfo returnInfo = new DataMoverClientInfo(channel.getChannelNum());
		return returnInfo;
	}

	@Override
	public void shutdown()
	{
		dataMoverSource.serverShutdown(this);
		super.shutdown();
	}
	
	@Override
	public boolean removeTarget(FirehoseTarget removeTarget) 
	{
		logger.error(new ErrorLogMessage("Removing target "+removeTarget));
		try
		{
			boolean removed = super.removeTarget(removeTarget);
			return removed;
		}
		finally
		{
			synchronized(targets)
			{
				if (targets.size() == 0)
				{
					logger.error(new ErrorLogMessage("All DataMoverServer targets have been removed - exiting!"));
					System.exit(1);
				}
			}
		}
	}

}
