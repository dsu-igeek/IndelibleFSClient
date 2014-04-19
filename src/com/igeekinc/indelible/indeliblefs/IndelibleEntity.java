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
 
package com.igeekinc.indelible.indeliblefs;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectIDFactory;

/**
 * An IndelibleEntity is an object that is identified by an EntityID and may be authenticated
 * by the SecurityServer.  In general these are objects that can communicate with other objects.
 * Clients and servers are IndelibleEntities and authenticate to each other.
 * @author David L. Smith-Uchida
 */
public class IndelibleEntity
{
    protected EntityID id;
    protected Logger logger;
    
    public static void initMapping()
    {
    	ObjectIDFactory.addMapping(IndelibleEntity.class, EntityID.class);
    }
    
    public IndelibleEntity(EntityID id)
    {
        this.id = id;
        logger = Logger.getLogger(getClass());
    }
    
    protected void setEntityID(EntityID id)
    {
        this.id = id;
    }
    
    public EntityID getEntityID()
    {
        return id;
    }
}
