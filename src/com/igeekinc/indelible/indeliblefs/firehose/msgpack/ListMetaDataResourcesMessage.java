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
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class ListMetaDataResourcesMessage<A> extends
		IndelibleFSConnectionCommandMessage<String[], A, ListMetaDataResourcesReply>
{
	public IndelibleFSObjectHandleMsgPack object;
	
	public ListMetaDataResourcesMessage()
	{
		// for message pack
	}

	public ListMetaDataResourcesMessage(IndelibleFSFirehoseClient client, 
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSObjectHandle object,
			AsyncCompletion<String[], A> callerCompletionHandler, A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.object = new IndelibleFSObjectHandleMsgPack(object);
	}

	@Override
	protected String[] getValueFromResult(ListMetaDataResourcesReply result)
	{
		return result.getResourceNames();
	}

	@Override
	public Class<ListMetaDataResourcesReply> getResultClass()
	{
		return ListMetaDataResourcesReply.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.listMetaDataResources(clientInfo, getConnectionHandle(), object.getObjectHandle());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kListMetaDataResources;
	}

}
