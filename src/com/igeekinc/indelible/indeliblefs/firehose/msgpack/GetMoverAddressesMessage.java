/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igeekinc.indelible.indeliblefs.firehose.msgpack;

import java.net.InetSocketAddress;

import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandResult;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerCommand;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.EntityIDMsgPack;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public class GetMoverAddressesMessage<A> extends IndelibleFSCommandMessage<InetSocketAddress [], A, GetMoverAddressesReply>
{
	public EntityIDMsgPack securityServerID;
	public GetMoverAddressesMessage()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	public GetMoverAddressesMessage(EntityID securityServerID, AsyncCompletion<InetSocketAddress [], A> callerCompletionHandler, A attachment)
	{
		super(callerCompletionHandler, attachment);
		this.securityServerID = new EntityIDMsgPack(securityServerID);
	}

	@Override
	protected InetSocketAddress[] getValueFromResult(GetMoverAddressesReply result)
	{
		return result.getMoverAddresses();
	}

	public EntityID getSecurityServerID()
	{
		return securityServerID.getEntityID();
	}

	@Override
	public Class<GetMoverAddressesReply> getResultClass()
	{
		return GetMoverAddressesReply.class;
	}

	@Override
	public CommandResult execute(IndelibleFSFirehoseServerIF server,
			IndelibleFSClientInfoIF clientInfo) throws Exception
	{
		return server.getMoverAddresses(clientInfo, securityServerID.getEntityID());
	}

	@Override
	protected IndelibleFSServerCommand getInitServerCommand()
	{
		return IndelibleFSServerCommand.kGetMoverAddresses;
	}
	
	
}
