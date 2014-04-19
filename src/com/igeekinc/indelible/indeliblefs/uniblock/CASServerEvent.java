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

import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.oid.EntityID;

public abstract class CASServerEvent extends IndelibleEvent
{
	private static final long	serialVersionUID	= 7502342434305539244L;

	public CASServerEvent(EntityID source, long eventID, long timestamp)
	{
		super(source, eventID, timestamp);
	}

	public CASServerEvent(EntityID source)
	{
		super(source);
	}
	
	public abstract CASServerEventType getEventType();
}
