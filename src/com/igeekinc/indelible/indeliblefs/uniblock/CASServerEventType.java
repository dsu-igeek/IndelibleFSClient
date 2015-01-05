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

public enum CASServerEventType
{
	kCollectionCreated('C'), kCollectionDestroyed('M');
	public static final char kCollectionCreatedEventTypeChar = 'C';
	public static final char kCollectionDestroyedEventTypeChar = 'M';
	private char eventType;
	private CASServerEventType(char eventType)
	{
		this.eventType = eventType;
	}
	
	public char getEventType()
	{
		return eventType;
	}
	
	public static CASServerEventType eventTypeForChar(char eventTypeChar)
	{
		switch(eventTypeChar)
		{
		case kCollectionCreatedEventTypeChar:
			return kCollectionCreated;
		case kCollectionDestroyedEventTypeChar:
			return kCollectionDestroyed;
		default:
			throw new IllegalArgumentException("Unrecognized event type "+eventTypeChar);
		}
	}
}