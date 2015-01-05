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

package com.igeekinc.indelible.indeliblefs.datamover;

import com.igeekinc.indelible.oid.DataMoverSessionID;

public class DataMoverSessionInfo
{
	private DataMoverSessionID sessionID;
	private DataMoverServerInfo serverInfo;
	public DataMoverSessionInfo(DataMoverSessionID sessionID)
	{
		this.sessionID = sessionID;

	}

	public DataMoverSessionID getSessionID()
	{
		return sessionID;
	}

	synchronized void setServerInfo(DataMoverServerInfo serverInfo)
	{
		this.serverInfo = serverInfo;
		notifyAll();
	}

	public synchronized boolean checkConnection(long timeout)
	{
		if (serverInfo == null)
		{
			try
			{
				wait(timeout);
			}
			catch (InterruptedException e)
			{

			}
		}
		if (serverInfo == null)
			return false;
		return true;
	}

	public DataMoverServerInfo getServerInfo()
	{
		return serverInfo;
	}

	public String dump()
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append(sessionID.toString());
		returnBuffer.append(":\n");
		returnBuffer.append(serverInfo.dump());
		return returnBuffer.toString();
	}
}

