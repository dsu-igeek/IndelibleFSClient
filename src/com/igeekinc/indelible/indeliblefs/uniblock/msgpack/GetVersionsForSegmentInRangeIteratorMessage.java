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
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.IndelibleVersionMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.IndelibleVersionIteratorProxy;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetVersionsForSegmentInRangeIteratorMessage<A> extends CASCollectionConnectionCommand<IndelibleVersionIterator, A, VersionIteratorReply>
{
	public ObjectIDMsgPack id;
	public IndelibleVersionMsgPack first, last;
	public GetVersionsForSegmentInRangeIteratorMessage()
	{
		// for message pack
	}

	public GetVersionsForSegmentInRangeIteratorMessage(CASServerFirehoseClient client, CASCollectionConnectionProxy connection,
			ObjectID id, IndelibleVersion first, IndelibleVersion last,
			AsyncCompletion<IndelibleVersionIterator, A> callerCompletionHandler, A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.id = new ObjectIDMsgPack(id);
		this.first = new IndelibleVersionMsgPack(first);
		this.last = new IndelibleVersionMsgPack(last);
	}

	@Override
	protected IndelibleVersionIterator getValueFromResult(VersionIteratorReply result)
	{
		IndelibleVersionIteratorProxy returnIterator = new IndelibleVersionIteratorProxy(getClient(), getCollectionConnection(), result.getHandle(), 
				result.getVersions(), result.hasMore());
		return returnIterator;
	}


	@Override
	public Class<VersionIteratorReply> getResultClass()
	{
		return VersionIteratorReply.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.listVersionsForSegment(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), id.getObjectID());
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kGetVersionsForSegmentInRangeIterator;
	}

}
