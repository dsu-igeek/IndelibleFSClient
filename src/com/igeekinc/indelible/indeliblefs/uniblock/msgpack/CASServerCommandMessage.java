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
package com.igeekinc.indelible.indeliblefs.uniblock.msgpack;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Ignore;
import org.msgpack.annotation.Message;

import com.igeekinc.firehose.CommandMessage;
import com.igeekinc.firehose.CommandResult;
import com.igeekinc.firehose.CommandToProcess;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerClientInfoIF;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseServerIF;
import com.igeekinc.util.async.AsyncCompletion;

@Message
public abstract class CASServerCommandMessage<V /* Type of the value returned to the caller */, 
	A /* Type of the caller's attachment */, 
	R /* Type of the result from the server */> extends CommandMessage implements AsyncCompletion<R, Void>
{
	@Ignore
	private AsyncCompletion<V, A>callerCompletionHandler;	// Caller's completion handler
	@Ignore
	private A callerAttachment;								// Attachment to be returned to the caller
	
	public CASServerCommandMessage()
	{
		// for message pack
	}
	
	public CASServerCommandMessage(AsyncCompletion<V, A>callerCompletionHandler, A attachment)
	{
		this.callerCompletionHandler = callerCompletionHandler;
		this.callerAttachment = attachment;
	}

	public CASServerCommand getCommand()
	{
		return CASServerCommand.getCommandForNum(getCommandCode());
	}

	/**
	 * Converts the result from the server into the value to be returned to the caller
	 * @param result
	 * @return
	 */
	protected abstract V getValueFromResult(R result);
	@Override
	public void completed(R result, Void attachment)
	{
		callerCompletionHandler.completed(getValueFromResult(result), this.callerAttachment);
	}

	@Override
	public void failed(Throwable exc, Void attachment)
	{
		callerCompletionHandler.failed(exc, this.callerAttachment);
	}
	
	public abstract Class<R> getResultClass();
	
	/*
	 * executeAsync is called on the <b>server</b> side to actually execute the command.  Note that Firehose does not move
	 * code between instances so that the code executed on the server side is always the code the server was compiled with.
	 * 
	 * The default implementation calls execute synchronously.  Override executeAsync to provide an actual asynchronous
	 * implementation
	 */
	public void executeAsync(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo, 
			AsyncCompletion<CommandResult, CommandToProcess>completionHandler, CommandToProcess attachment)
	{
		try
		{
			CommandResult result = execute(server, clientInfo);
			completionHandler.completed(result, attachment);
		}
		catch (Throwable t)
		{
			Logger.getLogger(getClass()).error("Got an exception for command "+getCommandCode());
			completionHandler.failed(t, attachment);
		}
	}
	/**
	 * execute is called on the <b>server</b> side to actually execute the command.  Note that Firehose does not move
	 * code between instances so that the code executed on the server side is always the code the server was compiled with
	 * @param server
	 * @param clientInfo
	 * @return
	 * @throws Exception
	 */
	public abstract CommandResult execute(CASServerFirehoseServerIF server, CASServerClientInfoIF clientInfo) throws Exception;

	@Override
	protected int getInitCommandCode()
	{
		return getInitCASServerCommand().getCommandNum();
	}

	protected abstract CASServerCommand getInitCASServerCommand();
	
	
}
