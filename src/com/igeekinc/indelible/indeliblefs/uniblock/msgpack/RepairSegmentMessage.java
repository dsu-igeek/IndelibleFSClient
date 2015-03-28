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
import com.igeekinc.indelible.indeliblefs.uniblock.DataVersionInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class RepairSegmentMessage<A> extends CASCollectionConnectionCommand<Void, A, Void>
{
	public ObjectIDMsgPack checkSegmentID;
	public IndelibleVersionMsgPack transactionVersion;
	public NetworkDataDescriptorMsgPack masterDataDescriptor;
	public IndelibleVersionMsgPack version;
	
	public RepairSegmentMessage()
	{
		// for message pack
	}

	public RepairSegmentMessage(CASServerFirehoseClient client,
			CASCollectionConnectionProxy collectionConnection,
			ObjectID checkSegmentID, IndelibleVersion transactionVersion, NetworkDataDescriptor masterDataDescriptor,
			IndelibleVersion masterVersion,
			AsyncCompletion<Void, A> completionHandler, A attachment)
	{
		super(client, collectionConnection, completionHandler, attachment);
		this.checkSegmentID = new ObjectIDMsgPack(checkSegmentID);
		this.transactionVersion = new IndelibleVersionMsgPack(transactionVersion);
		this.masterDataDescriptor = new NetworkDataDescriptorMsgPack(masterDataDescriptor);
		this.version = new IndelibleVersionMsgPack(masterVersion);
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
		DataVersionInfo masterData = new DataVersionInfo(masterDataDescriptor.getNetworkDataDescriptor(), version.getIndelibleVersion());
		return server.repairSegment(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), checkSegmentID.getObjectID(),
				transactionVersion.getIndelibleVersion(), masterData);
		//return server.storeSegment(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), dataDescriptor.getNetworkDataDescriptor());
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kRepairSegment;
	}
}
