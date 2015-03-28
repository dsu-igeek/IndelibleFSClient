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
import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFileHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSForkHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSForkProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetForkMessage<A> extends IndelibleFSFileCommandMessage<IndelibleFSForkIF, A, IndelibleFSForkHandle>
{
	String forkName;
	boolean createIfNecessary;
	public GetForkMessage()
	{
		// for message pack
	}

	public GetForkMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSFileHandle fileHandle,
			String forkName,
			boolean createIfNecessary,
			AsyncCompletion<IndelibleFSForkIF, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, fileHandle, callerCompletionHandler,
				attachment);
		this.forkName = forkName;
		this.createIfNecessary = createIfNecessary;
	}

	@Override
	protected IndelibleFSForkIF getValueFromResult(
			IndelibleFSForkHandle result)
	{
		IndelibleFSForkProxy returnProxy = new IndelibleFSForkProxy(getClient(), getConnection(), result);
		return returnProxy;
	}

	@Override
	public Class<IndelibleFSForkHandle> getResultClass()
	{
		return IndelibleFSForkHandle.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.getFork(clientInfo, getConnectionHandle(), getFileHandle(), forkName, createIfNecessary);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kGetFork;
	}

}
