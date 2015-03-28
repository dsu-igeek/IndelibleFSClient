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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo;
import com.igeekinc.util.logging.ErrorLogMessage;

@Message
public class IndelibleSnapshotInfoMsgPack
{
	public IndelibleVersionMsgPack version;
	public byte [] metadata;
	
	public IndelibleSnapshotInfoMsgPack()
	{
		// for message pack
	}

	public IndelibleSnapshotInfoMsgPack(IndelibleSnapshotInfo snapshotInfo) throws IOException
	{
		this.version = new IndelibleVersionMsgPack(snapshotInfo.getVersion());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(snapshotInfo.getMetadata());
		oos.close();
		this.metadata = baos.toByteArray();
	}
	
	@SuppressWarnings("unchecked")
	private HashMap<String, Serializable>getMetadata()
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(metadata);
			ObjectInputStream ois = new ObjectInputStream(bais);
			HashMap<String, Serializable> returnMetadata;

			returnMetadata = (HashMap<String, Serializable>) ois.readObject();
			return returnMetadata;

		} catch (ClassNotFoundException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Unknown class when deserializing");
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("IOException when deserializing");
		}
	}
	
	public IndelibleSnapshotInfo getIndelibleSnapshotInfo()
	{
		return new IndelibleSnapshotInfo(version.getIndelibleVersion(), getMetadata());
	}
}
