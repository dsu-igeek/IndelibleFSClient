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
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class PollForCollectionEventsMessage<A> extends CASServerConnectionCommand<CASCollectionQueuedEventMsgPack [], A, PollForCollectionEventsReply>
{
	int maxEvents;
	long timeout;
	
	public PollForCollectionEventsMessage()
	{
		// for message pack
	}

	public PollForCollectionEventsMessage(CASServerFirehoseClient client, CASServerConnectionProxy connection,
			int maxEvents, long timeout,
			AsyncCompletion<CASCollectionQueuedEventMsgPack [], A> callerCompletionHandler, A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.maxEvents = maxEvents;
		this.timeout = timeout;
	}

	@Override
	protected CASCollectionQueuedEventMsgPack [] getValueFromResult(PollForCollectionEventsReply result)
	{
		return result.getEvents();
	}


	@Override
	public Class<PollForCollectionEventsReply> getResultClass()
	{
		return PollForCollectionEventsReply.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.pollForCollectionEvents(clientInfo, getConnectionHandle(), maxEvents, timeout);
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kPollForCollectionEvents;
	}

}
