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
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleVersionIteratorProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class ListVersionsMessage<A> extends IndelibleFSObjectCommandMessage<IndelibleVersionIterator, A, ListVersionsReply>
{

	public ListVersionsMessage()
	{
		// for message pack
	}

	public ListVersionsMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSObjectHandle objectHandle,
			AsyncCompletion<IndelibleVersionIterator, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, objectHandle, callerCompletionHandler,
				attachment);
	}

	@Override
	protected IndelibleVersionIterator getValueFromResult(
			ListVersionsReply result)
	{
		IndelibleVersionIteratorProxy returnIterator = new IndelibleVersionIteratorProxy(getClient(), getConnection(), 
				result.getHandle(), result.getVersions(), result.hasMore());
		return returnIterator;
	}

	@Override
	public Class<ListVersionsReply> getResultClass()
	{
		return ListVersionsReply.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.listVersions(clientInfo, getConnectionHandle(), getFileHandle());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kListVersions;
	}
}
