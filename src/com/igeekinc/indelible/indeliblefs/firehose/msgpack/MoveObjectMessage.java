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

import org.msgpack.annotation.Ignore;
import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.CreateFileInfo;
import com.igeekinc.indelible.indeliblefs.DeleteFileInfo;
import com.igeekinc.indelible.indeliblefs.MoveObjectInfo;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleDirectoryNodeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSVolumeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFileNodeProxy;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class MoveObjectMessage<A>
		extends
		IndelibleFSConnectionCommandMessage<MoveObjectInfo, A, MoveObjectInfoMsgPack>
{
	IndelibleFSObjectHandleMsgPack volume;
	FilePathMsgPack sourcePath, destinationPath;
	
	@Ignore
	IndelibleFSVolumeProxy volumeProxy;
	
	public MoveObjectMessage()
	{
		// for message pack
	}

	public MoveObjectMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeProxy volume,
			FilePath sourcePath, FilePath destinationPath,
			AsyncCompletion<MoveObjectInfo, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.volumeProxy = volume;
		this.volume = new IndelibleFSObjectHandleMsgPack(volume.getHandle());
		this.sourcePath = new FilePathMsgPack(sourcePath);
		this.destinationPath = new FilePathMsgPack(destinationPath);
	}

	@Override
	protected MoveObjectInfo getValueFromResult(MoveObjectInfoMsgPack result)
	{
		IndelibleDirectoryNodeProxy sourceDirectoryProxy = new IndelibleDirectoryNodeProxy(getClient(), getConnection(), 
				volumeProxy, result.getSourceInfo().getDirectoryNode());
		DeleteFileInfo returnSourceInfo = new DeleteFileInfo(sourceDirectoryProxy, result.getSourceInfo().isDeleteSucceeded());
		
		IndelibleDirectoryNodeProxy destDirectoryProxy = new IndelibleDirectoryNodeProxy(getClient(), getConnection(), 
				volumeProxy, result.getDestInfo().getDirectoryNode());
		IndelibleFileNodeProxy createProxy = new IndelibleFileNodeProxy(getClient(), getConnection(), 
				volumeProxy, result.getDestInfo().getCreateNode());
		
		CreateFileInfo returnDestInfo = new CreateFileInfo(destDirectoryProxy, createProxy);
		
		MoveObjectInfo returnInfo = new MoveObjectInfo(returnSourceInfo, returnDestInfo);
		return returnInfo;
	}

	@Override
	public Class<MoveObjectInfoMsgPack> getResultClass()
	{
		return MoveObjectInfoMsgPack.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.moveObject(clientInfo, getConnectionHandle(), volume.getObjectHandle(), sourcePath.getFilePath(), destinationPath.getFilePath());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kMoveObject;
	}

}
