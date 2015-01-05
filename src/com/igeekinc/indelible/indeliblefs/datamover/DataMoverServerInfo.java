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

import com.igeekinc.indelible.oid.EntityID;

public class DataMoverServerInfo
{
    private EntityID serverID;
    private DataMoverClient dataMoverClient;
    
    public DataMoverServerInfo(EntityID serverID)
    {
        this.serverID = serverID;
    }
    
    protected synchronized void setConnection(DataMoverClient dataMoverClient)
    {
    	this.dataMoverClient = dataMoverClient;
        notifyAll();
    }

    public synchronized boolean checkConnection(long timeout)
    {
    	if (dataMoverClient == null)
    	{
    		try
    		{
    			wait(timeout);
    		}
    		catch (InterruptedException e)
    		{

    		}
    	}
    	if (dataMoverClient == null)
    		return false;
    	return true;
    }
    
    public EntityID getServerID()
    {
        return serverID;
    }
    
    public DataMoverClient getDataMoverClient()
    {
    	return dataMoverClient;
    }

	public String dump()
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("DataMoverServerInfo serverID = ");
		returnBuffer.append(serverID.toString());
		returnBuffer.append("\n");
		if (dataMoverClient != null)
			returnBuffer.append(dataMoverClient.dump());
		else
			returnBuffer.append("No data mover client");
		returnBuffer.append("\n");
		return returnBuffer.toString();
	}
}
