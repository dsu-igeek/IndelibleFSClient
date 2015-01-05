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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Future;

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.CannotDeleteDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotFileException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.util.async.AsyncCompletion;

public interface IndelibleDirectoryNodeIF extends IndelibleFileNodeIF
{

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleDirectoryNodeRemote#createChildFile(java.lang.String)
	 */
	public abstract CreateFileInfo createChildFile(String name,
			boolean exclusive) throws IOException, PermissionDeniedException,
			FileExistsException;

	public abstract CreateFileInfo createChildFile(String name,
			Map<String, CASIDDataDescriptor> initialForkData,
			boolean exclusive) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException;

	public abstract Future<CreateFileInfo> createChildFileAsync(String name,
			Map<String, CASIDDataDescriptor> initialForkData,
			boolean exclusive) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException;
	
	public abstract <A>void  createChildFileAsync(String name,
			Map<String, CASIDDataDescriptor> initialForkData,
			boolean exclusive, AsyncCompletion<CreateFileInfo, ? super A>completionHandler, A attachment) throws IOException, PermissionDeniedException,
			FileExistsException, RemoteException;
	
	
	/**
	 * This creates a new file with the same data as the original file.  Changes to the new file
	 * will not affect the original file
	 * @param name
	 * @param sourceFile
	 * @param exclusive
	 * @return
	 * @throws PermissionDeniedException
	 * @throws FileExistsException
	 * @throws IOException
	 * @throws NotFileException
	 * @throws ObjectNotFoundException
	 */
	public abstract CreateFileInfo createChildFile(String name,
			IndelibleFileNodeIF sourceFile, boolean exclusive)
			throws PermissionDeniedException, FileExistsException, IOException,
			NotFileException, ObjectNotFoundException;

	public abstract CreateSymlinkInfo createChildSymlink(String name,
			String targetPath, boolean exclusive)
			throws PermissionDeniedException, FileExistsException, IOException, ObjectNotFoundException;

	/**
	 * Creates a new link to the specified file.
	 * @param name
	 * @param sourceFile
	 * @param exclusive
	 * @return
	 * @throws PermissionDeniedException
	 * @throws FileExistsException
	 * @throws IOException
	 * @throws NotFileException
	 * @throws ObjectNotFoundException
	 */
	public abstract CreateFileInfo createChildLink(String name,
			IndelibleFileNodeIF sourceFile)
					throws PermissionDeniedException, FileExistsException, IOException,
					NotFileException, ObjectNotFoundException;
	
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleDirectoryNodeRemote#createChildDirectory(java.lang.String)
	 */
	public abstract CreateDirectoryInfo createChildDirectory(String name)
			throws IOException, PermissionDeniedException, FileExistsException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleDirectoryNodeRemote#deleteChild(java.lang.String)
	 */
	public abstract DeleteFileInfo deleteChild(String name) throws IOException,
			PermissionDeniedException, CannotDeleteDirectoryException;

	public abstract DeleteFileInfo deleteChildDirectory(String name)
			throws IOException, PermissionDeniedException,
			NotDirectoryException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleDirectoryNodeRemote#list()
	 */
	public abstract String[] list() throws IOException, PermissionDeniedException;

	public abstract int getNumChildren() throws IOException, PermissionDeniedException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleDirectoryNodeRemote#getChildNode(java.lang.String)
	 */
	public abstract IndelibleFileNodeIF getChildNode(String name)
			throws IOException, PermissionDeniedException, ObjectNotFoundException;

	public abstract boolean isDirectory();

	public abstract IndelibleNodeInfo[] getChildNodeInfo(String[] mdToRetrieve)
			throws IOException, PermissionDeniedException, RemoteException;
	
	public abstract IndelibleDirectoryNodeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException;

	public abstract Future<IndelibleNodeInfo[]> getChildNodeInfoAsync(String[] mdToRetrieve) throws IOException, PermissionDeniedException, RemoteException;

	public abstract <A> void getChildNodeInfoAsync(String[] mdToRetrieve, AsyncCompletion<IndelibleNodeInfo[], ? super A> completionHandler, A attachment)
			throws IOException, PermissionDeniedException, RemoteException;

}