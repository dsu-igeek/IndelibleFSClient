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

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFileHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetObjectByPathMessage<A>
		extends
		IndelibleFSVolumeCommandMessage<IndelibleFSFileHandle, A, IndelibleFSObjectHandleMsgPack>
{
	FilePathMsgPack path;
	public GetObjectByPathMessage()
	{
		// for message pack
	}

	public GetObjectByPathMessage(IndelibleFSFirehoseClient client, 
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeHandle volume, 
			FilePath path,
			AsyncCompletion<IndelibleFSFileHandle, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, volume, callerCompletionHandler, attachment);
		this.path = new FilePathMsgPack(path);
	}

	@Override
	protected IndelibleFSFileHandle getValueFromResult(
			IndelibleFSObjectHandleMsgPack result)
	{
		return (IndelibleFSFileHandle) result.getObjectHandle();
	}

	@Override
	public Class<IndelibleFSObjectHandleMsgPack> getResultClass()
	{
		return IndelibleFSObjectHandleMsgPack.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.getObjectByPath(clientInfo, connectionHandle, (IndelibleFSVolumeHandle)volume.getObjectHandle(), path.getFilePath());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kGetObjectByPath;
	}
}
