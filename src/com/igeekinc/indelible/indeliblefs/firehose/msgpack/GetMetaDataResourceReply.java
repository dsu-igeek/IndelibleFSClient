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

import com.igeekinc.util.logging.ErrorLogMessage;

@Message
/*
 * This is a temporary place holder - uses Java serialization to flatten the HashMap.  Needs to be
 * replaced with proper Message Pack infrastructure!	TODO
 */
public class GetMetaDataResourceReply
{
	public byte [] serializedResource;
	
	public GetMetaDataResourceReply()
	{
		// for message pack
	}

	public GetMetaDataResourceReply(Map<String, Object>resource) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(resource);
		oos.close();
		serializedResource = baos.toByteArray();
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object>getResource()
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(serializedResource);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Map<String, Object> returnResource;

			returnResource = (Map<String, Object>) ois.readObject();
			return returnResource;

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
