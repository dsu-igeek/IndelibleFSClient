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
 
package com.igeekinc.indelible.indeliblefs.remote;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;

import com.igeekinc.indelible.indeliblefs.IndelibleNodeInfo;
import com.igeekinc.indelible.indeliblefs.exceptions.CannotDeleteDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotFileException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.FilePath;

public interface IndelibleDirectoryNodeRemote extends IndelibleFileNodeRemote
{
    public abstract CreateFileInfoRemote createChildFile(String name, boolean exclusive)
            throws IOException, PermissionDeniedException, FileExistsException, RemoteException;

    public abstract CreateFileInfoRemote createChildFile(String name, HashMap<String, CASIDDataDescriptor>initialForkData, boolean exclusive)
        throws IOException, PermissionDeniedException, RemoteException, FileExistsException;
    
    /**
     * Creates a new file with the same data as the source file, using copy-on-write semantics
     * @param name
     * @param sourceFile
     * @return
     * @throws IOException
     * @throws PermissionDeniedException
     * @throws FileExistsException
     * @throws RemoteException
     * @throws ObjectNotFoundException 
     * @throws NotFileException 
     */
    public abstract CreateFileInfoRemote createChildFile(String name, FilePath sourceFilePath, boolean exclusive)
            throws IOException, PermissionDeniedException, FileExistsException, RemoteException, ObjectNotFoundException, NotFileException;
    
    public abstract CreateFileInfoRemote createChildFile(String name, IndelibleFSObjectID sourceFileID, boolean exclusive)
            throws IOException, PermissionDeniedException, FileExistsException, RemoteException, ObjectNotFoundException, NotFileException;
    
    public abstract CreateDirectoryInfoRemote createChildDirectory(String name)
            throws IOException, PermissionDeniedException, RemoteException, FileExistsException;

    public abstract DeleteFileInfoRemote deleteChild(String name) 
        throws IOException, PermissionDeniedException, CannotDeleteDirectoryException, RemoteException;
    
    public abstract DeleteFileInfoRemote deleteChildDirectory(String name)
        throws IOException, PermissionDeniedException, NotDirectoryException;
    
    public abstract String[] list() 
        throws IOException, PermissionDeniedException, RemoteException;

    public abstract IndelibleFileNodeRemote getChildNode(String name)
            throws IOException, PermissionDeniedException, RemoteException, ObjectNotFoundException;
    
    public abstract int getNumChildren() throws RemoteException;

	public abstract CreateSymlinkInfoRemote createChildSymlink(String name, String targetPath, boolean exclusive)
			throws IOException, PermissionDeniedException, FileExistsException, RemoteException, ObjectNotFoundException;
	
	public abstract CreateFileInfoRemote createChildLink(String name, IndelibleFSObjectID sourceFileID)
            throws IOException, PermissionDeniedException, FileExistsException, RemoteException, ObjectNotFoundException, NotFileException;
	
	public abstract IndelibleNodeInfo [] getChildNodeInfo(String [] mdToRetrieve) throws IOException, PermissionDeniedException, RemoteException;
}