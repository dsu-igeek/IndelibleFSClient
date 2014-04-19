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
 * Returns the results of creating a new file.  Includes the new file node and the new directory node
 * (Indelible directories are immutable.  Creating a new file or directory will return a new directory and the created node
 * @author David L. Smith-Uchida
 */
public class CreateFileInfo
{
    private IndelibleDirectoryNodeIF directoryNode;
    private IndelibleFileNodeIF createNode;
    
    public CreateFileInfo(IndelibleDirectoryNodeIF directoryNode, IndelibleFileNodeIF createNode)
    {
        this.directoryNode = directoryNode;
        this.createNode = createNode;
    }

    /* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.CreateFileInfoID#getDirectoryNode()
	 */
	public IndelibleDirectoryNodeIF getDirectoryNode()
    {
        return directoryNode;
    }

    /* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.CreateFileInfoID#getCreateNode()
	 */
	public IndelibleFileNodeIF getCreatedNode()
    {
        return createNode;
    }
}
