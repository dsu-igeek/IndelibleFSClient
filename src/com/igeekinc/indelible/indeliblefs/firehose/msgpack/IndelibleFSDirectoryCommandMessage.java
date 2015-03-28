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

import org.msgpack.annotation.Ignore;
import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleDirectoryNodeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public abstract class IndelibleFSDirectoryCommandMessage<V, A, R> extends
		IndelibleFSConnectionCommandMessage<V, A, R>
{
	public IndelibleFSObjectHandleMsgPack directoryHandle;
	
	@Ignore
	protected IndelibleDirectoryNodeProxy directory;	// We hold on to this to create proxy objects
	
	public IndelibleFSDirectoryCommandMessage()
	{
		// for message pack
	}

	public IndelibleFSDirectoryCommandMessage(IndelibleFSFirehoseClient client, 
			IndelibleFSServerConnectionProxy connection, 
			IndelibleDirectoryNodeProxy directory,
			AsyncCompletion<V, A> callerCompletionHandler, A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.directory = directory;
		this.directoryHandle = new IndelibleFSObjectHandleMsgPack(directory.getHandle());
	}
}
