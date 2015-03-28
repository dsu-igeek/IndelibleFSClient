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
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSDirectoryHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleDirectoryNodeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetChildNodeInfoMessage<A>
		extends
		IndelibleFSDirectoryCommandMessage<NetworkDataDescriptor, A, GetChildNodeInfoReply>
{
	public String[] mdToRetrieve;
	public GetChildNodeInfoMessage()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	public GetChildNodeInfoMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleDirectoryNodeProxy directory,
			String[] mdToRetrieve,
			AsyncCompletion<NetworkDataDescriptor, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, directory, callerCompletionHandler,
				attachment);
		this.mdToRetrieve = mdToRetrieve;
	}

	@Override
	protected NetworkDataDescriptor getValueFromResult(
			GetChildNodeInfoReply result)
	{
		return result.getChildNodeInfo();
	}

	@Override
	public Class<GetChildNodeInfoReply> getResultClass()
	{
		return GetChildNodeInfoReply.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.getChildNodeInfo(clientInfo, getConnectionHandle(), (IndelibleFSDirectoryHandle)directoryHandle.getObjectHandle(), mdToRetrieve);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kGetChildNodeInfo;
	}

}
