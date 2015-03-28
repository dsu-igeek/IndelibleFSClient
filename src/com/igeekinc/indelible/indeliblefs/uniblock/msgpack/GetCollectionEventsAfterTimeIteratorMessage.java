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
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionEventIteratorProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetCollectionEventsAfterTimeIteratorMessage<A> extends CASCollectionConnectionCommand<IndelibleEventIterator, A, CASCollectionEventIteratorReply>
{
	long timestamp;
	
	public GetCollectionEventsAfterTimeIteratorMessage()
	{
		// for message pack
	}

	public GetCollectionEventsAfterTimeIteratorMessage(CASServerFirehoseClient client, CASCollectionConnectionProxy connection,
			long timestamp,
			AsyncCompletion<IndelibleEventIterator, A> callerCompletionHandler, A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.timestamp = timestamp;
	}

	@Override
	protected IndelibleEventIterator getValueFromResult(CASCollectionEventIteratorReply result)
	{
		CASCollectionEventIteratorProxy returnIterator = new CASCollectionEventIteratorProxy(getClient(), getCollectionConnection(), result.getHandle(), 
				result.getEvents(), result.hasMore());
		return returnIterator;
	}

	@Override
	public Class<CASCollectionEventIteratorReply> getResultClass()
	{
		return CASCollectionEventIteratorReply.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.collectionEventsAfterTimeIterator(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), timestamp);
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kGetCollectionEventsAfterTimeIterator;
	}
}
