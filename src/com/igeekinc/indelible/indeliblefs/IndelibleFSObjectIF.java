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
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public interface IndelibleFSObjectIF
{

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSObjectRemote#getObjectID()
	 */
	public abstract IndelibleFSObjectID getObjectID();

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSObjectRemote#listMetaDataResources()
	 */
	public abstract String[] listMetaDataResources()
			throws PermissionDeniedException, IOException;

	/* (non-Javadoc)
	 * @see com.igeekinc.indelible.indeliblefs.core.IndelibleFSObjectRemote#getMetaDataResource(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public abstract HashMap<String, Object> getMetaDataResource(
			String mdResourceName) throws PermissionDeniedException,
			IOException;

	public abstract IndelibleFSObjectIF setMetaDataResource(
			String mdResourceName, HashMap<String, Object> resources)
			throws PermissionDeniedException, IOException;

	public abstract IndelibleVersionIterator listVersions() throws IOException;

	public abstract IndelibleFSObjectIF getVersion(IndelibleVersion version,
			RetrieveVersionFlags flags) throws IOException;

	public abstract IndelibleVersion getVersion();

}