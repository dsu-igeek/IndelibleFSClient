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
package com.igeekinc.indelible.indeliblefs.uniblock.firehose.proxies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class CASServerProxy implements CASServer<CASServerConnectionIF>
{
	private CASServerFirehoseClient client;
	private ArrayList<CASServerConnectionIF>connections = new ArrayList<CASServerConnectionIF>();
	private EntityID serverID = null;
	
	public CASServerProxy(CASServerFirehoseClient client)
	{
		this.client = client;
	}
	@Override
	public CASServerConnectionIF open() throws IOException, PermissionDeniedException
	{
		CASServerConnectionIF returnConnection = new CASServerConnectionProxy(this, client);
		connections.add(returnConnection);
		return returnConnection;
	}

	@Override
	public EntityID getServerID() throws IOException
	{
		if (serverID == null)
		{
			ComboFutureBase<EntityID>getServerFuture = new ComboFutureBase<EntityID>();
			client.getServerIDAsync(getServerFuture, null);
			try
			{
				serverID = getServerFuture.get();
			} catch (InterruptedException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new IOException("getServerID timed out");
			} catch (ExecutionException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				if (e.getCause() instanceof IOException)
					throw ((IOException)e.getCause());
				throw new IOException("Got an unexpected error");
			}
		}
		return serverID;
	}


	@Override
	public EntityID getSecurityServerID()
	{
		ComboFutureBase<EntityID>future = new ComboFutureBase<EntityID>();
		try
		{
			client.getSecurityServerIDAsync(future, null);
			return future.get();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Could not retrieve security server ID");
	}
}
