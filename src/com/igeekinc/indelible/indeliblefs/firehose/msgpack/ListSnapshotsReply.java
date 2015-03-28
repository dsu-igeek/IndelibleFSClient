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

import java.io.IOException;

import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleSnapshotIteratorHandle;

@Message
public class ListSnapshotsReply
{
	public IndelibleSnapshotIteratorHandle handle;
	public IndelibleSnapshotInfoMsgPack [] snapshots;
	public boolean hasMore;			// Has more after what's been returned in this message
	
	public static final int kMaxReturnVersions = 64;
	
	public ListSnapshotsReply()
	{
		// for message pack
	}
	
	public ListSnapshotsReply(IndelibleSnapshotIteratorHandle handle, IndelibleSnapshotInfo[] snapshotInfo, boolean hasMore) throws IOException
	{
		this.handle = handle;
		this.snapshots = new IndelibleSnapshotInfoMsgPack[snapshotInfo.length];
		for (int snapshotNum = 0; snapshotNum < snapshotInfo.length; snapshotNum++)
		{
			this.snapshots[snapshotNum] = new IndelibleSnapshotInfoMsgPack(snapshotInfo[snapshotNum]);
		}
		this.hasMore = hasMore;
	}
	
	public IndelibleSnapshotIteratorHandle getHandle()
	{
		return handle;
	}
	
	public IndelibleSnapshotInfo [] getVersions()
	{
		IndelibleSnapshotInfo [] returnSnapshots = new IndelibleSnapshotInfo[snapshots.length];
		for (int versionNum = 0; versionNum < snapshots.length; versionNum++)
		{
			returnSnapshots[versionNum] = snapshots[versionNum].getIndelibleSnapshotInfo();
		}
		return returnSnapshots;
	}
	
	public boolean hasMore()
	{
		return hasMore;
	}
}
