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

import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSForkHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public abstract class IndelibleFSForkCommandMessage<V, A, R> extends
		IndelibleFSConnectionCommandMessage<V, A, R>
{
	public IndelibleFSForkHandle forkHandle;
	public IndelibleFSForkCommandMessage()
	{
		// for message pack
	}

	public IndelibleFSForkCommandMessage(IndelibleFSFirehoseClient client, 
			IndelibleFSServerConnectionProxy connection, 
			IndelibleFSForkHandle forkHandle,
			AsyncCompletion<V, A> callerCompletionHandler, A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.forkHandle = forkHandle;
	}
	
	public IndelibleFSForkHandle getForkHandle()
	{
		return forkHandle;
	}
}
