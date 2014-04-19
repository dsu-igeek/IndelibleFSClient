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
 
package com.igeekinc.indelible.indeliblefs.uniblock;

import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.EntityID;

public class CollectionDestroyedEvent extends CASServerEvent
{
	private static final long	serialVersionUID	= -5547872950616496909L;
	private CASCollectionID collectionID;
	
	public CollectionDestroyedEvent(EntityID source, long eventID, long timestamp, CASCollectionID collectionID)
	{
		super(source, eventID, timestamp);
		this.collectionID = collectionID;
	}
	
	public CollectionDestroyedEvent(EntityID source, CASCollectionID collectionID)
	{
		super(source);
		this.collectionID = collectionID;
	}

	public CASCollectionID getCollectionID()
	{
		return collectionID;
	}
	
	@Override
	public CASServerEventType getEventType()
	{
		return CASServerEventType.kCollectionDestroyed;
	}
}
