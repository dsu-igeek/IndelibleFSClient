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
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerConnectionHandle;

@Message
public class ReleaseHandleMessage<A> extends IndelibleFSCommandMessage<Void, A, Void>
{
	public IndelibleFSServerConnectionHandle connection;
	public IndelibleFSObjectHandleMsgPack [] handles;
	
	public ReleaseHandleMessage()
	{
		// for message pack
	}

	public ReleaseHandleMessage(IndelibleFSServerConnectionHandle connection, IndelibleFSObjectHandle [] handles)
	{
		super(null, null);
		this.connection = connection;
		IndelibleFSObjectHandleMsgPack [] msgpackHandles = new IndelibleFSObjectHandleMsgPack[handles.length];
		for (int curHandleNum = 0; curHandleNum < handles.length; curHandleNum++)
			msgpackHandles[curHandleNum] = new IndelibleFSObjectHandleMsgPack(handles[curHandleNum]);
		this.handles = msgpackHandles;
	}
	
	@Override
	protected Void getValueFromResult(Void result)
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
		IndelibleFSObjectHandle [] localHandles = new IndelibleFSObjectHandle[handles.length];
		for (int curHandleNum = 0; curHandleNum < handles.length; curHandleNum++)
			localHandles[curHandleNum] = handles[curHandleNum].getObjectHandle();
		return server.releaseHandle(clientInfo, connection, localHandles);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kReleaseHandle;
	}

}
