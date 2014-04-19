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

/**
 * Returns the results of deleting a file.  Includes the success of the deletion and the new directory node
 * (Indelible directories are immutable.  Deleting a file or directory will return a new directory)
 * @author David L. Smith-Uchida
 */
public class DeleteFileInfo
{
    private IndelibleDirectoryNodeIF directoryNode;
    private boolean deleteSucceeded;
    
    public DeleteFileInfo(IndelibleDirectoryNodeIF directoryNode, boolean deleteSucceeded)
    {
    	this.directoryNode = directoryNode;
    	this.deleteSucceeded = deleteSucceeded;
    }

	public IndelibleDirectoryNodeIF getDirectoryNode()
	{
		return directoryNode;
	}

	public boolean deleteSucceeded()
	{
		return deleteSucceeded;
	}
}
