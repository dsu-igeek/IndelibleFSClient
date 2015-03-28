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
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetInfoForSnapshotMessage<A>
		extends
		IndelibleFSConnectionCommandMessage<IndelibleSnapshotInfo, A, IndelibleSnapshotInfoMsgPack>
{
	public IndelibleFSObjectHandleMsgPack volume;
	public IndelibleVersionMsgPack retrieveSnapshotVersion;
	public GetInfoForSnapshotMessage()
	{
		// for message pack
	}

	public GetInfoForSnapshotMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeHandle volume,
			IndelibleVersion retrieveSnapshotVersion,
			AsyncCompletion<IndelibleSnapshotInfo, A> callerCompletionHandler,
			A attachment) throws IOException
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.volume = new IndelibleFSObjectHandleMsgPack(volume);
		this.retrieveSnapshotVersion = new IndelibleVersionMsgPack(retrieveSnapshotVersion);
	}

	@Override
	protected IndelibleSnapshotInfo getValueFromResult(
			IndelibleSnapshotInfoMsgPack result)
	{
		return result.getIndelibleSnapshotInfo();
	}

	@Override
	public Class<IndelibleSnapshotInfoMsgPack> getResultClass()
	{
		return IndelibleSnapshotInfoMsgPack.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.getSnapshotInfo(clientInfo, connectionHandle, (IndelibleFSVolumeHandle)volume.getObjectHandle(), retrieveSnapshotVersion);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kGetInfoForSnapshot;
	}

}
