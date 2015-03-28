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
import com.igeekinc.indelible.indeliblefs.CreateSymlinkInfo;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSDirectoryHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleDirectoryNodeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSVolumeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleSymlinkNodeProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class CreateChildSymlinkMessage<A>
		extends
		IndelibleFSDirectoryCommandMessage<CreateSymlinkInfo, A, CreateSymlinkInfoMsgPack>
{
	public String name;
	public String targetPath;
	public boolean exclusive;
	
	public CreateChildSymlinkMessage()
	{
		// for message pack
	}

	public CreateChildSymlinkMessage(IndelibleFSFirehoseClient client, 
			IndelibleFSServerConnectionProxy connection,
			IndelibleDirectoryNodeProxy parent,
			String name,
			String target,
			boolean exclusive,
			AsyncCompletion<CreateSymlinkInfo, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, parent, callerCompletionHandler, attachment);
		this.name = name;
		this.targetPath = target;
		this.exclusive = exclusive;
	}

	@Override
	protected CreateSymlinkInfo getValueFromResult(
			CreateSymlinkInfoMsgPack result)
	{
		IndelibleDirectoryNodeProxy directoryProxy = new IndelibleDirectoryNodeProxy(getClient(), getConnection(), 
				(IndelibleFSVolumeProxy)directory.getVolume(), result.getDirectoryNode());
		IndelibleSymlinkNodeProxy createProxy = new IndelibleSymlinkNodeProxy(getClient(), getConnection(), 
				(IndelibleFSVolumeProxy)directory.getVolume(), result.getCreateNode());
		CreateSymlinkInfo returnInfo = new CreateSymlinkInfo(directoryProxy, createProxy);
		return returnInfo;
	}

	@Override
	public Class<CreateSymlinkInfoMsgPack> getResultClass()
	{
		return CreateSymlinkInfoMsgPack.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.createChildSymlink(clientInfo, getConnectionHandle(), 
				(IndelibleFSDirectoryHandle)directoryHandle.getObjectHandle(), name, targetPath,
				exclusive);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kCreateChildSymlink;
	}

}
