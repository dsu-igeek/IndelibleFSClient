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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;
/*
 * This is a temporary place holder - uses Java serialization to flatten the HashMap.  Needs to be
 * replaced with proper Message Pack infrastructure!	TODO
 */
@Message
public class CommitTransactionAndSnapshotMessage<A>
		extends
		IndelibleFSConnectionCommandMessage<IndelibleVersion, A, IndelibleVersionMsgPack>
{
	public byte [] snapshotMetadata;
	public CommitTransactionAndSnapshotMessage()
	{
		// for message pack
	}

	public CommitTransactionAndSnapshotMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			Map<String, Serializable>snapshotMetadata,
			AsyncCompletion<IndelibleVersion, A> callerCompletionHandler,
			A attachment) throws IOException
	{
		super(client, connection, callerCompletionHandler, attachment);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(snapshotMetadata);
		oos.close();
		this.snapshotMetadata = baos.toByteArray();
	}

	@Override
	protected IndelibleVersion getValueFromResult(IndelibleVersionMsgPack result)
	{
		return result.getIndelibleVersion();
	}

	@Override
	public Class<IndelibleVersionMsgPack> getResultClass()
	{
		return IndelibleVersionMsgPack.class;
	}
	@SuppressWarnings("unchecked")
	public HashMap<String, Serializable>getSnapshotMetadata()
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(snapshotMetadata);
			ObjectInputStream ois = new ObjectInputStream(bais);
			HashMap<String, Serializable> returnSnapshotMetadata;

			returnSnapshotMetadata = (HashMap<String, Serializable>) ois.readObject();
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
		return server.commitTransactionAndSnapshot(clientInfo, connectionHandle, getSnapshotMetadata());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kCommitTransactionAndSnapshot;
	}

}
