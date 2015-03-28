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
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.IndelibleVersionMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.SegmentInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class RetrieveSegmentInfoMessage<A> extends CASCollectionConnectionCommand<SegmentInfo, A, SegmentInfoMsgPack>
{
	public ObjectIDMsgPack objectID;
	public IndelibleVersionMsgPack version;
	public int retrieveVersionFlags;
	
	public RetrieveSegmentInfoMessage()
	{
		// for message pack
	}

	public RetrieveSegmentInfoMessage(CASServerFirehoseClient client,
			CASCollectionConnectionProxy collectionConnection,
			ObjectID objectID, IndelibleVersion version, RetrieveVersionFlags retrieveVersionFlags,
			AsyncCompletion<SegmentInfo, A> callerCompletionHandler, A attachment)
	{
		super(client, collectionConnection, callerCompletionHandler, attachment);
		this.objectID = new ObjectIDMsgPack(objectID);
		this.version = new IndelibleVersionMsgPack(version);
		this.retrieveVersionFlags = retrieveVersionFlags.getFlagValue();
	}

	@Override
	protected SegmentInfo getValueFromResult(SegmentInfoMsgPack result)
	{
		return result.getSegmentInfo();
	}

	@Override
	public Class<SegmentInfoMsgPack> getResultClass()
	{
		return SegmentInfoMsgPack.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.retrieveSegmentByObjectIDAndVersion(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), objectID.getObjectID(), version.getIndelibleVersion(),
				RetrieveVersionFlags.getFlagForNum(retrieveVersionFlags));
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kRetrieveSegmentInfo;
	}
}
