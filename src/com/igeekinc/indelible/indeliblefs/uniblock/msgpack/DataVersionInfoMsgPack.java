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
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.datamover.msgpack.NetworkDataDescriptorMsgPack;
import com.igeekinc.indelible.indeliblefs.firehose.msgpack.IndelibleVersionMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.DataVersionInfo;

@Message
public class DataVersionInfoMsgPack
{
	public NetworkDataDescriptorMsgPack dataDescriptor;
	public IndelibleVersionMsgPack version;
	
	public DataVersionInfoMsgPack()
	{
		// for message pack
	}

	public DataVersionInfoMsgPack(NetworkDataDescriptor dataDescriptor, IndelibleVersion version)
	{
		this.dataDescriptor = new NetworkDataDescriptorMsgPack(dataDescriptor);
		this.version = new IndelibleVersionMsgPack(version);
	}
	
	public DataVersionInfo getDataVersionInfo()
	{
		return new DataVersionInfo(dataDescriptor.getNetworkDataDescriptor(), version.getIndelibleVersion());
	}
}
