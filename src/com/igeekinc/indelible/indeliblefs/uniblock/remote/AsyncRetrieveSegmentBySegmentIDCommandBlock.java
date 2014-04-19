/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.indelible.indeliblefs.uniblock.remote;

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.RemoteAsyncCommandBlock;

public class AsyncRetrieveSegmentBySegmentIDCommandBlock extends RemoteAsyncCommandBlock
{
	private ObjectID segmentID;
	private IndelibleVersion version;
	private RetrieveVersionFlags flags;
	
	public AsyncRetrieveSegmentBySegmentIDCommandBlock(ObjectID segmentID)
	{
		this.segmentID = segmentID;
	}
	
	public AsyncRetrieveSegmentBySegmentIDCommandBlock(ObjectID segmentID, IndelibleVersion version, RetrieveVersionFlags flags)
	{
		this.segmentID = segmentID;
		this.version = IndelibleVersion.kLatestVersion;
		this.flags = RetrieveVersionFlags.kNearest;
	}
	private transient CASCollectionConnection connection;
	
	/*
	 * This is only called on the server side
	 */
	public void setConnection(CASCollectionConnection connection)
	{
		this.connection = connection;
	}
	
	@Override
	public void executeAsync(AsyncCompletion completionHandler)
			throws Exception
	{
		connection.retrieveSegmentAsync(segmentID, version, flags, completionHandler, null);
	}

}
