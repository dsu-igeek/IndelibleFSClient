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
 
package com.igeekinc.indelible.indeliblefs.server;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.uniblock.CASSegmentIDIterator;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class RemoteCASSegmentIDIteratorProxy implements CASSegmentIDIterator
{
	private RemoteCASSegmentIDIterator remoteIterator;

	public RemoteCASSegmentIDIteratorProxy(RemoteCASSegmentIDIterator remoteIterator)
	{
		this.remoteIterator = remoteIterator;
	}
	
	@Override
	public boolean hasNext()
	{
		try
		{
			return remoteIterator.hasNext();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got remote exception");
		}
	}

	@Override
	public ObjectID next()
	{
		try
		{
			return remoteIterator.next();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got remote exception");
		}
	}

	@Override
	public void remove()
	{
		try
		{
			remoteIterator.remove();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got remote exception");
		}
	}

	@Override
	public void close()
	{
		try
		{
			remoteIterator.close();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Got remote exception");
		}
	}

}
