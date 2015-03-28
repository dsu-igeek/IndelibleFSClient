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
package com.igeekinc.indelible.indeliblefs.firehose.msgpack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;

@Message
public class SetMetaDataResource<A>
		extends
		IndelibleFSObjectCommandMessage<IndelibleFSObjectHandle, A, IndelibleFSObjectHandleMsgPack>
{
	public String mdResourceName;
	public byte [] serializedResource;
	public SetMetaDataResource()
	{
		// for message pack
	}

	public SetMetaDataResource(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSObjectHandle objectHandle,
			String mdResourceName,
			Map<String, Object> resource,
			AsyncCompletion<IndelibleFSObjectHandle, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, objectHandle, callerCompletionHandler,
				attachment);
		this.mdResourceName = mdResourceName;
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(resource);
			oos.close();
			serializedResource = baos.toByteArray();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Could not serialize resource");
		}
	}

	@Override
	protected IndelibleFSObjectHandle getValueFromResult(
			IndelibleFSObjectHandleMsgPack result)
	{
		return result.getObjectHandle();
	}

	@Override
	public Class<IndelibleFSObjectHandleMsgPack> getResultClass()
	{
		return IndelibleFSObjectHandleMsgPack.class;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object>getResource()
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(serializedResource);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Map<String, Object> returnResource;

			returnResource = (Map<String, Object>) ois.readObject();
			return returnResource;

		} catch (ClassNotFoundException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Unknown class when deserializing");
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("IOException when deserializing");

		}
	}
	
	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.setMetaDataResource(clientInfo, getConnectionHandle(), getFileHandle(), mdResourceName, getResource());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kSetMetaDataResource;
	}	
}
