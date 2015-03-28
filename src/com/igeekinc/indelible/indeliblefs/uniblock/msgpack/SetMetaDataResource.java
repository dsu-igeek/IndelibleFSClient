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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies.CASCollectionConnectionProxy;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;

@Message
public class SetMetaDataResource<A>
		extends
		CASCollectionConnectionCommand<Void, A, Void>
{
	public String mdResourceName;
	public byte [] serializedResource;
	public SetMetaDataResource()
	{
		// for message pack
	}

	public SetMetaDataResource(
			CASServerFirehoseClient client,
			CASCollectionConnectionProxy connection,
			String mdResourceName,
			Map<String, Object> resource,
			AsyncCompletion<Void, A> callerCompletionHandler,
			A attachment)
	{
		super(client, connection, callerCompletionHandler,
				attachment);
		this.mdResourceName = mdResourceName;
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(resource);
			oos.close();
			serializedResource = baos.toByteArray();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Could not serialize resource");
		}
	}

	@Override
	protected Void getValueFromResult(
			Void result)
	{
		return result;
	}

	@Override
	public Class<Void> getResultClass()
	{
		return Void.class;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Serializable>getResource()
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(serializedResource);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Map<String, Serializable> returnResource;

			returnResource = (Map<String, Serializable>) ois.readObject();
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
	
	@Override
	public CommandResult execute(CASServerFirehoseServerIF server,
			CASServerClientInfoIF clientInfo) throws Exception
	{
		return server.setMetaDataResource(clientInfo, getConnectionHandle(), getCollectionConnectionHandle(), mdResourceName, getResource());
	}

	@Override
	protected CASServerCommand getInitCASServerCommand()
	{
		return CASServerCommand.kSetMetaDataResource;
	}	
}
