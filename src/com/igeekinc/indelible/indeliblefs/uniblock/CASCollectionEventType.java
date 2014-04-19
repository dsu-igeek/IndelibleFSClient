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

public enum CASCollectionEventType
{

	kSegmentCreated('C'), kSegmentReleased('R'), kMetadataModified('D'), kTransactionCommited('T');
	public static final char kSegmentCreatedEventTypeChar = 'C';
	public static final char kSegmentReleasedEventTypeChar = 'R';
	public static final char kMetadataModifiedEventTypeChar = 'D';
	public static final char kTransactionCommittedEventTypeChar = 'T';
	private char eventType;
	private CASCollectionEventType(char eventType)
	{
		this.eventType = eventType;
	}
	
	public char getEventType()
	{
		return eventType;
	}
	
	public static CASCollectionEventType eventTypeForChar(char eventTypeChar)
	{
		switch(eventTypeChar)
		{
		case kSegmentCreatedEventTypeChar:
			return kSegmentCreated;
		case kSegmentReleasedEventTypeChar:
			return kSegmentReleased;
		case kMetadataModifiedEventTypeChar:
			return kMetadataModified;
		case kTransactionCommittedEventTypeChar:
			return kTransactionCommited;
		default:
			throw new IllegalArgumentException("Unrecognized event type "+eventTypeChar);
		}
	}
}