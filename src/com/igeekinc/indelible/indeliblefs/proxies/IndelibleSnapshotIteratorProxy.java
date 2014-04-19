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
 
package com.igeekinc.indelible.indeliblefs.proxies;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo;
import com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotIterator;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleSnapshotIteratorRemote;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleSnapshotIteratorProxy implements
		IndelibleSnapshotIterator
{
	private IndelibleSnapshotIteratorRemote remote;
	private IndelibleSnapshotInfo [] list;
	private int offset = 0;
	private boolean finished = false;
	
	public IndelibleSnapshotIteratorProxy(IndelibleSnapshotIteratorRemote remote)
	{
		this.remote = remote;
		try
		{
			list = remote.next(100);
		} catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	
	@Override
	public boolean hasNext()
	{
		loadNext();
		if (!finished)
		{
			if (offset < list.length)
				return true;
		}
		return false;
	}

	@Override
	public IndelibleSnapshotInfo next()
	{
		loadNext();
		if (!finished)
		{
			if (offset < list.length)
				return list[offset ++];
		}
		return null;
	}

	private void loadNext()
	{
		if (!finished)
		{
			if (list == null || offset >= list.length)
			{
				try
				{
					list = remote.next(100);
					offset = 0;
				} catch (RemoteException e)
				{
					// TODO Auto-generated catch block
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				}
				if (list == null || list.length == 0)
					finished = true;
			}
		}
	}
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}
