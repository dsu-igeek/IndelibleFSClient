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
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.msgpack.NetworkDataDescriptorMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.CASStoreInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class StoreSegmentMessage<A> extends CASCollectionConnectionCommand<CASStoreInfo, A, CASStoreInfoMsgPack>
{
	public NetworkDataDescriptorMsgPack dataDescriptor;
	public StoreSegmentMessage()
	{
		// for message pack
	}

	public StoreSegmentMessage(CASServerFirehoseClient client,
			CASCollectionConnectionProxy collectionConnection,
			NetworkDataDescriptor dataDescriptor,
			AsyncCompletion<CASStoreInfo, A> completionHandler, A attachment)
	{
		super(client, collectionConnection, completionHandler, attachment);
		this.dataDescriptor = new NetworkDataDescriptorMsgPack(dataDescriptor);
	}

	@Override
	protected CASStoreInfo getValueFromResult(CASStoreInfoMsgPack result)
	{
		return result.getCASStoreInfo();
	}

	@Override
	public Class<CASStoreInfoMsgPack> getResultClass()
	{
		return CASStoreInfoMsgPack.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.storeSegment(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), dataDescriptor.getNetworkDataDescriptor());
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kStoreSegment;
	}
}
