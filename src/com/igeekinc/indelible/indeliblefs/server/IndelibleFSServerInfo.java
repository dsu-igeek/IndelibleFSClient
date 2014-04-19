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

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.igeekinc.indelible.oid.EntityID;

/**
 * Information about an Indelible FS server.  All properties are immutable
 * @author David L. Smith-Uchida
 */
public class IndelibleFSServerInfo implements Serializable
{
    private static final long serialVersionUID = 5933985661792149460L;
    public static final String kServerIDPropertyName = "serverID";
    public static final String kDisplayNamePropertyName = "displayName";
    public static final String kIPAddressPropertyName = "ipAddress";
    public static final String kSecurityServerIDPropertyName = "securityServerID";
    protected HashMap<String, Object>properties;
    
    public IndelibleFSServerInfo(EntityID serverID, String displayName, InetAddress ipAddress, EntityID securityServerID, HashMap<String, Object>otherProperties)
    {
        if (serverID == null || displayName == null || ipAddress == null || otherProperties == null)
            throw new IllegalArgumentException("Arguments cannot be null");
        properties = new HashMap<String,Object>(otherProperties);
        properties.put(kServerIDPropertyName, serverID);
        properties.put(kDisplayNamePropertyName, displayName);
        properties.put(kIPAddressPropertyName, ipAddress);
        properties.put(kSecurityServerIDPropertyName, securityServerID);
    }
    
    public Object getPropertyByName(String propertyName)
    {
        return properties.get(propertyName);
    }
    
    public String [] getPropertyNames()
    {
        String [] returnNames = new String[properties.size()];
        returnNames = properties.keySet().toArray(returnNames);
        return returnNames;
    }
    
    public Map.Entry<String, Object>[] getPropertyEntries()
    {
        Map.Entry<String, Object>[]returnMap = new Map.Entry[properties.size()];
        returnMap = properties.entrySet().toArray(returnMap);
        return returnMap;
    }

	public EntityID getServerID()
	{
		return (EntityID) properties.get(kServerIDPropertyName);
	}
	
	public EntityID getSecurityServerID()
	{
		return (EntityID) properties.get(kSecurityServerIDPropertyName);
	}
}
