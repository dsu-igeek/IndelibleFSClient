/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igeekinc.indelible.indeliblefs.firehose;

import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public class IndelibleFSObjectHandle
{
	public enum ObjectHandleType
	{
		kVolume(0),
		kFile(1),
		kDirectory(2),
		kSymbolicLink(3);
		
		static ObjectHandleType [] commandArray;
	    static {
	    	commandArray = new ObjectHandleType[ObjectHandleType.values().length];
	    	for (ObjectHandleType addCommand:ObjectHandleType.values())
	    	{
	    		if (addCommand.typeNum > commandArray.length)
	    			throw new InternalError("Command num > # of commands");
	    		if (commandArray[addCommand.typeNum] != null)
	    			throw new InternalError(addCommand.typeNum+" is double-booked");
	    		commandArray[addCommand.typeNum] = addCommand;
	    	}
	    }
	    
		int typeNum;
		private ObjectHandleType(int commandNum)
		{
			this.typeNum = commandNum;
		}

		public int getTypeNum()
		{
			return typeNum;
		}
		
		public static ObjectHandleType getTypeForNum(int num)
		{
			if (num >= commandArray.length)
				throw new IllegalArgumentException(num+" > max command number ("+(commandArray.length - 1)+")");
			if (num < 0)
				throw new IllegalArgumentException(num+" cannot be negative");
			return commandArray[num];
		}
	}
	
	public static IndelibleFSObjectHandle createObjectHandle(long objectHandle, IndelibleFSObjectID objectID, ObjectHandleType handleType)
	{
		switch (handleType)
		{
		case kDirectory:
			return createDirectoryHandle(objectHandle, objectID);
		case kFile:
			return createFileHandle(objectHandle, objectID);
		case kSymbolicLink:
			return createSymbolicLinkHandle(objectHandle, objectID);
		case kVolume:
			return createVolumeHandle(objectHandle, objectID);
		default:
			throw new IllegalArgumentException("Undefined handle type");		
		}
	}
	
	public static IndelibleFSVolumeHandle createVolumeHandle(long objectHandle, IndelibleFSObjectID objectID)
	{
		return new IndelibleFSVolumeHandle(objectHandle, objectID);
	}
	
	public static IndelibleFSFileHandle createFileHandle(long objectHandle, IndelibleFSObjectID objectID)
	{
		return new IndelibleFSFileHandle(objectHandle, objectID);
	}
	
	public static IndelibleFSDirectoryHandle createDirectoryHandle(long objectHandle, IndelibleFSObjectID objectID)
	{
		return new IndelibleFSDirectoryHandle(objectHandle, objectID);
	}
	
	public static IndelibleFSSymbolicLinkHandle createSymbolicLinkHandle(long objectHandle, IndelibleFSObjectID objectID)
	{
		return new IndelibleFSSymbolicLinkHandle(objectHandle, objectID);
	}
	private ObjectHandleType handleType;
	private long objectHandle;
	private IndelibleFSObjectID objectID;
	
	public IndelibleFSObjectHandle()
	{
		// for message pack
	}
	
	protected IndelibleFSObjectHandle(long objectHandle, IndelibleFSObjectID objectID, ObjectHandleType handleType)
	{
		this.objectHandle = objectHandle;
		this.objectID = objectID;
		this.handleType = handleType;
	}

	public long getObjectHandle()
	{
		return objectHandle;
	}
	
	public IndelibleFSObjectID getObjectID()
	{
		return objectID;
	}

	public ObjectHandleType getHandleType()
	{
		return handleType;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (objectHandle ^ (objectHandle >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndelibleFSObjectHandle other = (IndelibleFSObjectHandle) obj;
		if (objectHandle != other.objectHandle)
			return false;
		return true;
	}
	
	public String toString()
	{
		return handleType+"|"+objectHandle+"|"+objectID;
	}
}
