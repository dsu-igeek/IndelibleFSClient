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
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.msgpack.NetworkDataDescriptorMsgPack;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.IndelibleVersionMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class StoreReplicatedSegmentMessage<A> extends CASCollectionConnectionCommand<Void, A, Void>
{
	public ObjectIDMsgPack replicateSegmentID;
	public IndelibleVersionMsgPack replicateVersion;
	public NetworkDataDescriptorMsgPack dataDescriptor;
	public CASCollectionEventMsgPack replicateEvent; 
	public StoreReplicatedSegmentMessage()
	{
		// for message pack
	}

	public StoreReplicatedSegmentMessage(CASServerFirehoseClient client,
			CASCollectionConnectionProxy collectionConnection,
			ObjectID replicateSegmentID, IndelibleVersion replicateVersion,
			NetworkDataDescriptor dataDescriptor,
			CASCollectionEvent replicateEvent,
			AsyncCompletion<Void, A> completionHandler, A attachment)
	{
		super(client, collectionConnection, completionHandler, attachment);
		this.replicateSegmentID = new ObjectIDMsgPack(replicateSegmentID);
		this.replicateVersion = new IndelibleVersionMsgPack(replicateVersion);
		this.dataDescriptor = new NetworkDataDescriptorMsgPack(dataDescriptor);
		this.replicateEvent = new CASCollectionEventMsgPack(replicateEvent);
	}

	@Override
	protected Void getValueFromResult(Void result)
	{
		return result;
	}

	@Override
	public Class<Void> getResultClass()
	{
		return Void.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.storeReplicatedSegment(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), 
				replicateSegmentID.getObjectID(), replicateVersion.getIndelibleVersion(), dataDescriptor.getNetworkDataDescriptor(),
				replicateEvent.getEvent());
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kStoreReplicatedSegment;
	}
}
