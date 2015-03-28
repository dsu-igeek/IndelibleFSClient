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
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.indelible.oid.CASSegmentID;
import com.igeekinc.indelible.oid.msgpack.ObjectIDMsgPack;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class BulkReleaseSegmentMessage<A> extends CASCollectionConnectionCommand<boolean [], A, BulkReleaseReply>
{
	public ObjectIDMsgPack [] segmentID;
	public BulkReleaseSegmentMessage()
	{
		// for message pack
	}

	public BulkReleaseSegmentMessage(CASServerFirehoseClient client,
			CASCollectionConnectionProxy collectionConnection,
			CASSegmentID [] segmentID,
			AsyncCompletion<boolean [], A> callerCompletionHandler, A attachment)
	{
		super(client, collectionConnection, callerCompletionHandler, attachment);
		this.segmentID = new ObjectIDMsgPack[segmentID.length];
		for (int curSegmentNum = 0; curSegmentNum < segmentID.length; curSegmentNum++)
		{
			this.segmentID[curSegmentNum] = new ObjectIDMsgPack(segmentID[curSegmentNum]);
		}
	}

	@Override
	protected boolean [] getValueFromResult(BulkReleaseReply result)
	{
		return result.getStatus();
	}

	@Override
	public Class<BulkReleaseReply> getResultClass()
	{
		return BulkReleaseReply.class;
	}

	@Override
	public CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws Exception
	{
		CASSegmentID [] releaseIDs = new CASSegmentID[segmentID.length];
		for (int curSegmentNum = 0; curSegmentNum < segmentID.length; curSegmentNum++)
		{
			releaseIDs[curSegmentNum] = (CASSegmentID) segmentID[curSegmentNum].getObjectID();
		}
		return server.bulkReleaseSegment(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), releaseIDs);
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kBulkReleaseSegment;
	}
}
