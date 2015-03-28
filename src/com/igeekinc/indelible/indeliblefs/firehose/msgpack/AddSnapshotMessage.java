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

import java.io.IOException;

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class AddSnapshotMessage<A>
		extends
		IndelibleFSConnectionCommandMessage<Void, A, Void>
{
	public IndelibleFSObjectHandleMsgPack volume;
	public IndelibleSnapshotInfoMsgPack snapshotInfo;
	public AddSnapshotMessage()
	{
		// for message pack
	}

	public AddSnapshotMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeHandle volume,
			IndelibleSnapshotInfo snapshotInfo,
			AsyncCompletion<Void, A> callerCompletionHandler,
			A attachment) throws IOException
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.volume = new IndelibleFSObjectHandleMsgPack(volume);
		this.snapshotInfo = new IndelibleSnapshotInfoMsgPack(snapshotInfo);
	}

	@Override
	protected Void getValueFromResult(
			Void result)
	{
		return null;
	}

	@Override
	public Class<Void> getResultClass()
	{
		return Void.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.addSnapshot(clientInfo, getConnectionHandle(), (IndelibleFSVolumeHandle)volume.getObjectHandle(), snapshotInfo);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kAddSnapshot;
	}

}
