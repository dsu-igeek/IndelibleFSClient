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
 
package com.igeekinc.indelible.indeliblefs.core;

import java.io.Serializable;
import java.util.HashMap;

/**
 * An IndelibleSnapshotInfo describes a snapshot.  A snapshot is defined by an IndelibleVersion.  The snapshot may also
 * have metadata associated with it.  The metadata may not be modified after the snapshot is created.
 *
 */
public class IndelibleSnapshotInfo implements Serializable
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -8721291834749478105L;
	private IndelibleVersion version;
	private HashMap<String, Serializable>metadata;
	
	public IndelibleSnapshotInfo(IndelibleVersion version, HashMap<String, Serializable>metadata)
	{
		if (version == null)
			throw new IllegalArgumentException("Version cannot be null");
		this.version = version;
		if (metadata != null)
			this.metadata = hashmapClone(metadata);
		else
			this.metadata = new HashMap<String, Serializable>();
	}

	@SuppressWarnings("unchecked")
	private HashMap<String, Serializable> hashmapClone(
			HashMap<String, Serializable> metadata)
	{
		return (HashMap<String, Serializable>) metadata.clone();
	}
	
	public IndelibleVersion getVersion()
	{
		return version;
	}

	public Object getMetadataProperty(String propertyName)
	{
		synchronized(metadata)
		{
			return metadata.get(propertyName);
		}
	}
	
	public HashMap<String, Serializable> getMetadata()
	{
		return metadata;
	}
}
