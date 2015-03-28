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
import com.igeekinc.indelible.indeliblefs.CreateDirectoryInfo;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSDirectoryHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleDirectoryNodeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSVolumeProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class CreateChildDirectoryMessage<A>
		extends
		IndelibleFSDirectoryCommandMessage<CreateDirectoryInfo, A, CreateFileInfoMsgPack>
{
	public String name;
	
	public CreateChildDirectoryMessage()
	{
		// for message pack
	}

	public CreateChildDirectoryMessage(IndelibleFSFirehoseClient client, 
			IndelibleFSServerConnectionProxy connection,
			IndelibleDirectoryNodeProxy parent,
			String name,
			AsyncCompletion<CreateDirectoryInfo, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, parent, callerCompletionHandler, attachment);
		this.name = name;
	}

	@Override
	protected CreateDirectoryInfo getValueFromResult(
			CreateFileInfoMsgPack result)
	{
		IndelibleDirectoryNodeProxy directoryProxy = new IndelibleDirectoryNodeProxy(getClient(), getConnection(), 
				(IndelibleFSVolumeProxy)directory.getVolume(), result.getDirectoryNode());
		IndelibleDirectoryNodeProxy createProxy = new IndelibleDirectoryNodeProxy(getClient(), getConnection(), 
				(IndelibleFSVolumeProxy)directory.getVolume(), (IndelibleFSDirectoryHandle) result.getCreateNode());
		CreateDirectoryInfo returnInfo = new CreateDirectoryInfo(directoryProxy, createProxy);
		return returnInfo;
	}

	@Override
	public Class<CreateFileInfoMsgPack> getResultClass()
	{
		return CreateFileInfoMsgPack.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.createChildDirectory(clientInfo, getConnectionHandle(), (IndelibleFSDirectoryHandle)directoryHandle.getObjectHandle(), name);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kCreateDirectory;
	}
}
