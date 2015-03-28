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
package com.igeekinc.indelible.indeliblefs.firehose.proxies;

import java.io.IOException;

import com.igeekinc.indelible.indeliblefs.IndelibleSymlinkNodeIF;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSObjectHandle;

public class IndelibleSymlinkNodeProxy extends IndelibleFileNodeProxy implements
		IndelibleSymlinkNodeIF
{

	public IndelibleSymlinkNodeProxy(IndelibleFSFirehoseClient client,
			IndelibleFSServerConnectionProxy connection,
			IndelibleFSVolumeProxy volume,
			IndelibleFSObjectHandle remote)
	{
		super(client, connection, volume, remote);
	}

	@Override
	public String getTargetPath() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

}
