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

/**
 * A CASCollection stores mutable and immutable data.  Both are identified by an IndelibleFSObjectID that is
 * assigned when the data is stored.  In the case of immutable data, the IndelibleFSObjectID and CASIdentifier
 * are permanently bound.  In the case of the mutable data, they are not.  A CASIdentifier uniquely identifies a
 * segment of data by its cryptographic checksum.  A CASIdentifier is a world wide unique ID as the belief is that
 * two pieces of data with the same CASIdentifier are, in fact, the same two pieces of data.  The ObjectID
 * is bound on a per-CASCollection basis.  A particular segment will have one or more ObjectID's assigned to it.
 * If the assignment is mutable, then the data that the ObjectID represents can be changed.<br/>
 * Additionally, the CASCollection supports a single segment (up to the maximum segment size) of metadata that is available
 * for the use of its clients.  The metadata is treated as raw data and is not interpreted.  It is there primarily to allow
 * upper layers to boot strap without using any external storage.
 */

public enum RetrieveVersionFlags
{
	kExact,
	kNearest
}