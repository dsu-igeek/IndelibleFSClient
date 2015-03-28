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
import com.igeekinc.indelible.indeliblefs.DeleteFileInfo;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleDirectoryNodeProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSVolumeProxy;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class DeleteObjectByPathMessage<A>
		extends
		IndelibleFSConnectionCommandMessage<DeleteFileInfo, A, DeleteFileInfoMsgPack>
{
	public IndelibleFSObjectHandleMsgPack volume;
	public FilePathMsgPack path;
	
	@Ignore 
	IndelibleFSVolumeProxy volumeProxy;
	public DeleteObjectByPathMessage()
	{
		// for message pack
	}

	public DeleteObjectByPathMessage(IndelibleFSFirehoseClient client, 
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeProxy volume,
			FilePath path,
			AsyncCompletion<DeleteFileInfo, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, callerCompletionHandler, attachment);
		this.volumeProxy = volume;
		this.volume = new IndelibleFSObjectHandleMsgPack(volume.getHandle());
		this.path = new FilePathMsgPack(path);
	}

	@Override
	protected DeleteFileInfo getValueFromResult(
			DeleteFileInfoMsgPack result)
	{
		IndelibleDirectoryNodeProxy directoryProxy = new IndelibleDirectoryNodeProxy(getClient(), getConnection(), 
				volumeProxy, result.getDirectoryNode());
		DeleteFileInfo returnInfo = new DeleteFileInfo(directoryProxy, result.isDeleteSucceeded());
		return returnInfo;
	}

	@Override
	public Class<DeleteFileInfoMsgPack> getResultClass()
	{
		return DeleteFileInfoMsgPack.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		FilePath deletePath = path.getFilePath();
		return server.deleteObjectByPath(clientInfo, getConnectionHandle(), (IndelibleFSVolumeHandle)volume.getObjectHandle(), deletePath);
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kDeleteObjectByPath;
	}

}
