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

import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASCollectionConnectionHandle;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public abstract class CASCollectionConnectionCommand<V, A, R> extends CASServerConnectionCommand<V, A, R>
{
	public CASCollectionConnectionHandle collectionConnectionHandle;
	
	@Ignore												// Does not get transmitted to the server
	private CASCollectionConnectionProxy collectionConnection;		// Ditto

	public CASCollectionConnectionCommand()
	{
		// for message pack
	}
	
	public CASCollectionConnectionCommand(CASServerFirehoseClient client, CASCollectionConnectionProxy connection,
			AsyncCompletion<V, A> callerCompletionHandler, A attachment)
	{
		super(client, connection.getServerConnection(), callerCompletionHandler, attachment);
		this.collectionConnection = connection;
		this.collectionConnectionHandle = connection.getCASCollectionConnectionHandle();
	}
	
	public CASCollectionConnectionHandle getCollectionConnectionHandle()
	{
		return collectionConnectionHandle;
	}
	
	public CASCollectionConnectionProxy getCollectionConnection()
	{
		return collectionConnection;
	}
}
