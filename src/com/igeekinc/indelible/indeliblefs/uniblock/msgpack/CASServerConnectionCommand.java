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
package com.igeekinc.indelible.indeliblefs.uniblock.msgpack;

import org.msgpack.annotation.Ignore;
import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerConnectionHandle;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public abstract class CASServerConnectionCommand<V, A, R> extends CASServerCommandMessage<V, A, R>
{
	public CASServerConnectionHandle connectionHandle;
	
	@Ignore
	private CASServerFirehoseClient client;				// We need this so we can construct new proxies on the client side.  
	@Ignore												// Does not get transmitted to the server
	private CASServerConnectionProxy connection;		// Ditto

	public CASServerConnectionCommand()
	{
		// for message pack
	}
	
	public CASServerConnectionCommand(CASServerFirehoseClient client, CASServerConnectionProxy connection,
			AsyncCompletion<V, A> callerCompletionHandler, A attachment)
	{
		super(callerCompletionHandler, attachment);
		this.client = client;
		this.connection = connection;
		this.connectionHandle = connection.getCASServerConnectionHandle();
	}
	
	public CASServerConnectionHandle getConnectionHandle()
	{
		return connectionHandle;
	}
	
	public CASServerFirehoseClient getClient()
	{
		return client;
	}
	
	public CASServerConnectionProxy getConnection()
	{
		return connection;
	}
}
