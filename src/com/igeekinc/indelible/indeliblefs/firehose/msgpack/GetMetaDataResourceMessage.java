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

import java.util.Map;

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetMetaDataResourceMessage<A>
		extends
		IndelibleFSObjectCommandMessage<Map<String, Object>, A, GetMetaDataResourceReply>
{
	public String resourceName;
	public GetMetaDataResourceMessage()
	{
		// For message pack
	}

	public GetMetaDataResourceMessage(IndelibleFSFirehoseClient client, 
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSObjectHandle objectHandle,
			String resourceName,
			AsyncCompletion<Map<String, Object>, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, objectHandle, callerCompletionHandler, attachment);
		this.resourceName = resourceName;
	}

	@Override
	protected Map<String, Object> getValueFromResult(
			GetMetaDataResourceReply result)
	{
		return result.getResource();
	}

	@Override
	public Class<GetMetaDataResourceReply> getResultClass()
	{
		return GetMetaDataResourceReply.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.getMetaDataResource(clientInfo, connectionHandle, objectHandle.getObjectHandle(), resourceName);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kGetMetaDataResource;
	}
}
