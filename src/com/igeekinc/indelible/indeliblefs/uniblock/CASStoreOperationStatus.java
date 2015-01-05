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


public enum CASStoreOperationStatus
{
	kInvalidStatus(0),
	kSegmentCreated(1),
	kSegmentExists(2);
	
	static CASStoreOperationStatus [] operationStatusArray;
    static {
    	operationStatusArray = new CASStoreOperationStatus[CASStoreOperationStatus.values().length];
    	for (CASStoreOperationStatus addCommand:CASStoreOperationStatus.values())
    	{
    		if (addCommand.statusValue > operationStatusArray.length)
    			throw new InternalError("Command num > # of commands");
    		if (operationStatusArray[addCommand.statusValue] != null)
    			throw new InternalError(addCommand.statusValue+" is double-booked");
    		operationStatusArray[addCommand.statusValue] = addCommand;
    	}
    }
    
	int statusValue;
	private CASStoreOperationStatus(int statusValue)
	{
		this.statusValue = statusValue;
	}

	public int getStatusValue()
	{
		return statusValue;
	}
	
	public static CASStoreOperationStatus getStatusForNum(int num)
	{
		if (num >= operationStatusArray.length)
			throw new IllegalArgumentException(num+" > max command number ("+(operationStatusArray.length - 1)+")");
		if (num < 0)
			throw new IllegalArgumentException(num+" cannot be negative");
		if (num == 0)
			throw new IllegalArgumentException("0 is an invalid status");
		return operationStatusArray[num];
	}
}
