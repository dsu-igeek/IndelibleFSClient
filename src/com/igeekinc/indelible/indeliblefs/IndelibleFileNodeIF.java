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
import java.util.HashMap;

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;

public interface IndelibleFileNodeIF extends IndelibleFSObjectIF
{

	/* (non-Javadoc)
	 * @see com.igeekinc.util.FileLike#lastModified()
	 */
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFileNodeRemote#lastModified()
	 */
	public abstract long lastModified() throws IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.util.FileLike#isDirectory()
	 */
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFileNodeRemote#isDirectory()
	 */
	public abstract boolean isDirectory();

	/* (non-Javadoc)
	 * @see com.igeekinc.util.FileLike#isFile()
	 */
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFileNodeRemote#isFile()
	 */
	public abstract boolean isFile();

	/* (non-Javadoc)
	 * @see com.igeekinc.util.FileLike#length()
	 */
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFileNodeRemote#length()
	 */
	public abstract long length() throws IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.util.FileLike#totalLength()
	 */
	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFileNodeRemote#totalLength()
	 */
	public abstract long totalLength() throws IOException;

	public abstract long lengthWithChildren() throws IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFileNodeRemote#getFork(java.lang.String, boolean)
	 */
	public abstract IndelibleFSForkIF getFork(String name, boolean createIfNecessary) throws IOException, ForkNotFoundException, PermissionDeniedException;

	public abstract void deleteFork(String forkName) throws IOException, ForkNotFoundException, PermissionDeniedException;
	
	public abstract String[] listForkNames() throws IOException;

	public abstract IndelibleFSVolumeIF getVolume() throws IOException;

	public abstract IndelibleFileNodeIF setMetaDataResource(
			String mdResourceName, HashMap<String, Object> resources)
			throws PermissionDeniedException, IOException;

	public abstract IndelibleVersionIterator listVersions() throws IOException;

	public abstract IndelibleFileNodeIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException;

}