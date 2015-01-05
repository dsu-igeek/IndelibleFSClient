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
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Properties;

import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.SessionAuthentication;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.util.datadescriptor.DataDescriptor;

public interface IndelibleServerConnectionIF
{
	public abstract IndelibleFSVolumeIF createVolume(Properties volumeProperties)
			throws IOException, PermissionDeniedException;

	public abstract void deleteVolume(IndelibleFSObjectID deleteVolumeID)
		    throws VolumeNotFoundException, PermissionDeniedException, IOException;
	
	public abstract IndelibleFSVolumeIF retrieveVolume(
			IndelibleFSObjectID retrieveVolumeID)
			throws VolumeNotFoundException, IOException;

	public abstract IndelibleFSObjectID[] listVolumes() throws IOException;

	public abstract void startTransaction() throws IOException;

	public abstract boolean inTransaction() throws IOException;
	
	public abstract IndelibleVersion commit() throws IOException;

	public abstract IndelibleVersion commitAndSnapshot(
			HashMap<String, Serializable> snapshotMetadata) throws IOException,
			PermissionDeniedException;

	public abstract void rollback() throws IOException;

	public abstract void close() throws IOException;

	public EntityAuthentication getClientEntityAuthentication() throws IOException;
	
	public EntityAuthentication getServerEntityAuthentication() throws IOException, AuthenticationFailureException;
	
	public void addClientSessionAuthentication(SessionAuthentication sessionAuthentication) throws IOException;
	
	public SessionAuthentication getSessionAuthentication() throws IOException;
	
	public NetworkDataDescriptor registerDataDescriptor(DataDescriptor localDataDescriptor);
	
	public void removeDataDescriptor(DataDescriptor writeDescriptor);
	
	public DataMoverSession getMoverSession();

	public NetworkDataDescriptor registerNetworkDescriptor(byte[] bytes);

	public NetworkDataDescriptor registerNetworkDescriptor(byte[] bytes, int offset,
			int length);

	public NetworkDataDescriptor registerNetworkDescriptor(ByteBuffer byteBuffer);
	
	/*
	 * Sets a per-transaction value.  This value is only valid during the current transaction.  All per transaction
	 * data is discared at a commit or rollback.  If there is no transaction active the data will be silently discarded.
	 * 
	 */
	public void setPerTransactionData(String key, Object value);
	
	/*
	 * Retrieves a per-transaction value.  All values are discarded when a commit or rollback occurs.  Returns null if
	 * there is no value for the key
	 */
	public Object getPerTransactionData(String key);
}