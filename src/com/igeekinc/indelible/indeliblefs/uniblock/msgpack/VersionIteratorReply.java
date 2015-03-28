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

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleVersionIteratorHandle;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.IndelibleVersionMsgPack;

@Message
public class VersionIteratorReply
{
	public IndelibleVersionIteratorHandle handle;
	public IndelibleVersionMsgPack [] versions;
	public boolean hasMore;			// Has more after what's been returned in this message
	
	public static final int kMaxReturnEvents = 64;

	public VersionIteratorReply()
	{
		// for message pack
	}

	public VersionIteratorReply(IndelibleVersionIteratorHandle handle, IndelibleVersion [] versions, boolean hasMore)
	{
		this.handle = handle;
		this.versions = new IndelibleVersionMsgPack[versions.length];
		for (int eventNum = 0; eventNum < versions.length; eventNum++)
		{
			this.versions[eventNum] = new IndelibleVersionMsgPack(versions[eventNum]);
		}
		this.hasMore = hasMore;
	}
	
	public IndelibleVersionIteratorHandle getHandle()
	{
		return handle;
	}
	
	public IndelibleVersion [] getVersions()
	{
		IndelibleVersion [] returnVersions = new IndelibleVersion[versions.length];
		for (int versionsNum = 0; versionsNum < versions.length; versionsNum++)
		{
			returnVersions[versionsNum] = versions[versionsNum].getIndelibleVersion();
		}
		return returnVersions;
	}
	
	public boolean hasMore()
	{
		return hasMore;
	}
}
