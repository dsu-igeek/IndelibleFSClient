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
import java.util.Map;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.IndelibleNodeInfo;
import com.igeekinc.util.logging.ErrorLogMessage;

@Message
public class IndelibleNodeInfoMsgPack
{
	public IndelibleFSObjectIDMsgPack nodeID;
	public String name;
	public byte [] serializedMetaData;
	
	public IndelibleNodeInfoMsgPack()
	{
		// for message pack
	}
	
	public IndelibleNodeInfoMsgPack(IndelibleNodeInfo nodeInfo)
	{
		this.nodeID = new IndelibleFSObjectIDMsgPack(nodeInfo.getNodeID());
		this.name = nodeInfo.getName();
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(nodeInfo.getAllMetaData());
			oos.close();
			serializedMetaData = baos.toByteArray();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got unexpected IOException serializing metadata");
		}
	}
	
	public IndelibleNodeInfo getNodeInfo()
	{

		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(serializedMetaData);
			ObjectInputStream ois = new ObjectInputStream(bais);

			@SuppressWarnings("unchecked")
			Map<String, Map<String, Object>> metaData = (Map<String, Map<String, Object>>) ois.readObject();
			return new IndelibleNodeInfo(nodeID.getIndelibleFSObjectID(), name, metaData);

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
}
