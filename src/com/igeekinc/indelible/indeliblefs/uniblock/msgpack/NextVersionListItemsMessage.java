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

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleVersionIteratorHandle;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.NextVersionListItemsReply;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class NextVersionListItemsMessage<A>
		extends
		CASCollectionConnectionCommand<NextVersionListItemsReply, A, NextVersionListItemsReply>
{
	IndelibleVersionIteratorHandle handle;
	public NextVersionListItemsMessage()
	{
		// for message pack
	}

	public NextVersionListItemsMessage(
			CASServerFirehoseClient client,
			CASCollectionConnectionProxy connection,
			IndelibleVersionIteratorHandle handle,
			AsyncCompletion<NextVersionListItemsReply, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.handle = handle;
	}

	@Override
	protected NextVersionListItemsReply getValueFromResult(
			NextVersionListItemsReply result)
	{
		return result;
	}

	@Override
	public Class<NextVersionListItemsReply> getResultClass()
	{
		return NextVersionListItemsReply.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server,
			CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.nextVersionListItems(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), handle);
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kNextVersionListItems;
	}
}
