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
import java.util.Properties;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSVolumeProxy;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;
/*
 * This is a temporary place holder - uses Java serialization to flatten the HashMap.  Needs to be
 * replaced with proper Message Pack infrastructure!	TODO
 */
@Message
public class CreateVolumeMessage<A>
		extends
		IndelibleFSConnectionCommandMessage<IndelibleFSVolumeIF, A, IndelibleFSObjectHandleMsgPack>
{
	public byte [] volumeProperties;
	public CreateVolumeMessage()
	{
		// for message pack
	}

	public CreateVolumeMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			Properties volumeProperties,
			AsyncCompletion<IndelibleFSVolumeIF, A> callerCompletionHandler,
			A attachment) throws IOException
	{
		super(client, connection, callerCompletionHandler, attachment);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(volumeProperties);
		oos.close();
		this.volumeProperties = baos.toByteArray();
	}

	@Override
	protected IndelibleFSVolumeIF getValueFromResult(IndelibleFSObjectHandleMsgPack result)
	{
		return new IndelibleFSVolumeProxy(getClient(), getConnection(), (IndelibleFSVolumeHandle) result.getObjectHandle());
	}

	@Override
	public Class<IndelibleFSObjectHandleMsgPack> getResultClass()
	{
		return IndelibleFSObjectHandleMsgPack.class;
	}
	@SuppressWarnings("unchecked")
	public Properties getVolumeProperties()
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(volumeProperties);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Properties returnSnapshotMetadata;

			returnSnapshotMetadata = (Properties) ois.readObject();
			return returnSnapshotMetadata;

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
		return server.createVolume(clientInfo, connectionHandle, getVolumeProperties());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kCreateVolume;
	}
}
