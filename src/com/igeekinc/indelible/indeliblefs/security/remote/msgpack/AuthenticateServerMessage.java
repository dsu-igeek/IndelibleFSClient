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
 
package com.igeekinc.indelible.indeliblefs.security.remote.msgpack;

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandMessage;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerFirehoseClient;
import com.igeekinc.indelible.oid.EntityID;

@Message
public class AuthenticateServerMessage extends CommandMessage
{
	public EntityIDMsgPack entityID;
	public byte [] encodedCertReq;
	
	public AuthenticateServerMessage()
	{
		
	}
	
	public AuthenticateServerMessage(EntityID serverID, byte [] encodedCertReq)
	{
		this.entityID = new EntityIDMsgPack(serverID);
		this.encodedCertReq = encodedCertReq;
	}
	
	public EntityID getEntityID()
	{
		return (EntityID)entityID.getObjectID();
	}

	public byte[] getEncodedCertReq()
	{
		return encodedCertReq;
	}

	@Override
	protected int getInitCommandCode()
	{
		return EntityAuthenticationServerFirehoseClient.EntityAuthenticationCommand.kAuthenticateServerCommand.getCommandNum();
	}
}
