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

public class EntityAuthenticationServerTrustedEvent extends EventObject
{
    private static final long serialVersionUID = -2913090476543034356L;
    EntityAuthenticationServer addedServer;
    
    public EntityAuthenticationServerTrustedEvent(EntityAuthenticationClient source, EntityAuthenticationServer addedServer)
    {
        super(source);
        this.addedServer = addedServer;
    }

    @Override
    public String toString()
    {
        try
        {
            return "Server "+addedServer.getEntityID().toString()+" trusted";
        } catch (RemoteException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            return "Server <unknown> trusted";
        }
    }

    public EntityAuthenticationServer getAddedServer()
    {
        return addedServer;
    }
}
