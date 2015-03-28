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

import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSDirectoryHandle;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFileHandle;

@Message
public class CreateFileInfoMsgPack
{
    private IndelibleFSObjectHandleMsgPack directoryNode;
    private IndelibleFSObjectHandleMsgPack createNode;
	public CreateFileInfoMsgPack()
	{
		// for message pack
	}

	public CreateFileInfoMsgPack(IndelibleFSDirectoryHandle directoryNode, IndelibleFSFileHandle createNode)
	{
		this.directoryNode = new IndelibleFSObjectHandleMsgPack(directoryNode);
		this.createNode = new IndelibleFSObjectHandleMsgPack(createNode);
	}

	public IndelibleFSDirectoryHandle getDirectoryNode()
	{
		return (IndelibleFSDirectoryHandle) directoryNode.getObjectHandle();
	}

	public IndelibleFSFileHandle getCreateNode()
	{
		return (IndelibleFSFileHandle) createNode.getObjectHandle();
	}
}
