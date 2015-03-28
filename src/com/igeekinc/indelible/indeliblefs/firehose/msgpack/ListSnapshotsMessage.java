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

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotIterator;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSVolumeHandle;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleFSServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.firehose.proxies.IndelibleSnapshotIteratorProxy;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class ListSnapshotsMessage<A> extends IndelibleFSVolumeCommandMessage<IndelibleSnapshotIterator, A, ListSnapshotsReply>
{

	public ListSnapshotsMessage()
	{
		// for message pack
	}

	public ListSnapshotsMessage(
			IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeHandle volumeHandle,
			AsyncCompletion<IndelibleSnapshotIterator, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, volumeHandle, callerCompletionHandler,
				attachment);
	}

	@Override
	protected IndelibleSnapshotIterator getValueFromResult(
			ListSnapshotsReply result)
	{
		IndelibleSnapshotIterator returnIterator = new IndelibleSnapshotIteratorProxy(getClient(), getConnection(), 
				result.getHandle(), result.getVersions(), result.hasMore());
		return returnIterator;
	}

	@Override
	public Class<ListSnapshotsReply> getResultClass()
	{
		return ListSnapshotsReply.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.listSnapshots(clientInfo, getConnectionHandle(), getVolumeHandle());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kListSnapshots;
	}
}
