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

import java.io.IOException;

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASCollectionConnectionHandle;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASServerConnectionProxy;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class AddCollectionMessage<A> extends CASServerConnectionCommand<CASCollectionConnectionHandle, A, CASCollectionConnectionHandle>
{
	public ObjectIDMsgPack addCollectionID;
	public AddCollectionMessage()
	{

	}

	public AddCollectionMessage(CASServerFirehoseClient client, CASServerConnectionProxy connection,
			CASCollectionID addCollectionID,
			AsyncCompletion<CASCollectionConnectionHandle, A> callerCompletionHandler, A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.addCollectionID = new ObjectIDMsgPack(addCollectionID);
	}

	@Override
	protected CASCollectionConnectionHandle getValueFromResult(CASCollectionConnectionHandle result)
	{
		return result;
	}

	@Override
	public Class<CASCollectionConnectionHandle> getResultClass()
	{
		return CASCollectionConnectionHandle.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws IOException
	{
		return server.addCollection(clientInfo, getConnectionHandle(), (CASCollectionID)addCollectionID.getObjectID());
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kAddCollection;
	}
}
