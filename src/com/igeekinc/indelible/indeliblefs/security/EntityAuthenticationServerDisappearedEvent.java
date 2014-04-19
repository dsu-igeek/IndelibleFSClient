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
 
package com.igeekinc.indelible.indeliblefs.security;

import java.rmi.RemoteException;
import java.util.EventObject;

import org.apache.log4j.Logger;

import com.igeekinc.util.logging.ErrorLogMessage;

public class EntityAuthenticationServerDisappearedEvent extends EventObject
{
    private static final long serialVersionUID = -5935619306428888124L;
    EntityAuthenticationServer removedServer;
    
    public EntityAuthenticationServerDisappearedEvent(EntityAuthenticationClient source, EntityAuthenticationServer removedServer)
    {
        super(source);
        this.removedServer = removedServer;
    }

    @Override
    public String toString()
    {
        try
        {
            return "Server "+removedServer.getEntityID().toString()+" removed";
        } catch (RemoteException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            return "Server <unknown> removed";
        }
    }

    public EntityAuthenticationServer getRemovedServer()
    {
        return removedServer;
    }
}
