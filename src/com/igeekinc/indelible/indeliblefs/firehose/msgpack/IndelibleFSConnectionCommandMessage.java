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
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerConnectionHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public abstract class IndelibleFSConnectionCommandMessage<V, A, R> extends
		IndelibleFSCommandMessage<V, A, R>
{
	public IndelibleFSServerConnectionHandle connectionHandle;
	
	@Ignore
	private IndelibleFSFirehoseClient client;				// We need this so we can construct new proxies on the client side.  
	@Ignore													// Does not get transmitted to the server
	private IndelibleFSServerConnectionProxy connection;	// Ditto
	public IndelibleFSConnectionCommandMessage()
	{
		// for message pack
	}

	public IndelibleFSConnectionCommandMessage(IndelibleFSFirehoseClient client, IndelibleFSServerConnectionProxy connection,
			AsyncCompletion<V, A> callerCompletionHandler, A attachment)
	{
		super(callerCompletionHandler, attachment);
		this.client = client;
		this.connectionHandle = connection.getHandle();
		this.connection = connection;
	}

	public IndelibleFSServerConnectionHandle getConnectionHandle()
	{
		return connectionHandle;
	}
	
	public IndelibleFSFirehoseClient getClient()
	{
		return client;
	}
	
	public IndelibleFSServerConnectionProxy getConnection()
	{
		return connection;
	}
}
