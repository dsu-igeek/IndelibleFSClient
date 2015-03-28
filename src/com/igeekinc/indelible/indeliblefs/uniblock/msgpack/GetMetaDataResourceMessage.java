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

import java.io.Serializable;
import java.util.Map;

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetMetaDataResourceMessage<A>
		extends
		CASCollectionConnectionCommand<Map<String, Serializable>, A, GetMetaDataResourceReply>
{
	public String mdResourceName;
	public GetMetaDataResourceMessage()
	{
		// For message pack
	}

	public GetMetaDataResourceMessage(CASServerFirehoseClient client, CASCollectionConnectionProxy connection,
			String mdResourceName, AsyncCompletion<Map<String, Serializable>, A> completionHandler, A attachment)
	{
		super(client, connection, completionHandler, attachment);
		this.mdResourceName = mdResourceName;
	}

	@Override
	protected Map<String, Serializable> getValueFromResult(
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
	public CommandResult execute(CASServerFirehoseServerIF server,
			CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.getMetaDataResource(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), mdResourceName);
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kGetMetaDataResource;
	}
}
