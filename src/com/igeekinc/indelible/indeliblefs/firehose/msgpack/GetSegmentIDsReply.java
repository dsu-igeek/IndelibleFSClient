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

import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CASIdentifierMsgPack;

@Message
public class GetSegmentIDsReply
{
	public CASIdentifierMsgPack [] segmentIDs;
	public GetSegmentIDsReply()
	{
		// for message pack
	}

	public GetSegmentIDsReply(CASIdentifier [] segmentIDs)
	{
		this.segmentIDs = new CASIdentifierMsgPack[segmentIDs.length];
		for (int curSegmentNum = 0; curSegmentNum < segmentIDs.length; curSegmentNum++)
		{
			this.segmentIDs[curSegmentNum] = new CASIdentifierMsgPack(segmentIDs[curSegmentNum]);
		}
	}

	public CASIdentifier [] getSegmentIDs()
	{
		CASIdentifier [] returnSegmentIDs = new CASIdentifier[segmentIDs.length];
		
		for (int curSegmentNum = 0; curSegmentNum < segmentIDs.length; curSegmentNum++)
		{
			returnSegmentIDs[curSegmentNum] = segmentIDs[curSegmentNum].getCASIdentifier();
		}
		return returnSegmentIDs;
	}
}
