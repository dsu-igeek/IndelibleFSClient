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
package com.igeekinc.indelible.indeliblefs.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;
import com.igeekinc.util.async.Pipeline;
import com.igeekinc.util.async.PipelineState;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSDataPipeline extends Pipeline<DataPipelinePhase, Void>
{
	private Throwable exception;
	private Logger logger = Logger.getLogger(getClass());
	private IndelibleFSForkIF writeFork;
	private long appendAt;
	
	public IndelibleFSDataPipeline(IndelibleFSForkIF writeFork) throws IOException
	{
		this.writeFork = writeFork;
		appendAt = writeFork.length();
	}
	
	public void appendData(DataDescriptor fromDescriptor, long offset, int length) 
			throws IOException
	{
		if (exception != null)
		{
			if (exception instanceof IOException)
				throw (IOException)exception;
			if (exception instanceof Error)
				throw (Error)exception;
			logger.error(new ErrorLogMessage("Got unexpected exception"), exception);
			throw new InternalError("Unexpected exception "+exception);
		}
		CASIDDataDescriptor curChunkDescriptor = new CASIDMemoryDataDescriptor(fromDescriptor, offset, length);
		System.out.println("offset = "+offset+" CASIdentifier = "+curChunkDescriptor.getCASIdentifier());
		DataPipelineState pipelineState = new DataPipelineState();
		pipelineState.setCurrentState(DataPipelinePhase.kAppending);
		enter(pipelineState);
		writeFork.writeDataDescriptorAsync(appendAt, curChunkDescriptor, this, pipelineState);
		appendAt += curChunkDescriptor.getLength();
	}
	
	public void appendComplete(PipelineState<DataPipelinePhase> attachment)
	{
		exit(attachment);
	}
	
	public void appendFailed(PipelineState<DataPipelinePhase> attachment)
	{
		exception = attachment.getException();
		exit(attachment);
	}
	
	@Override
	public void dispatchSuccess(PipelineState<DataPipelinePhase> attachment)
	{
		switch (attachment.getCurrentState())
		{
		case kAppending:
			appendComplete(attachment);
			break;
		default:
			break;
		
		}
	}

	@Override
	public void dispatchFailed(PipelineState<DataPipelinePhase> attachment)
	{
		switch (attachment.getCurrentState())
		{
		case kAppending:
			appendFailed(attachment);
			break;
		default:
			break;
		}
	}

	@Override
	public synchronized void waitForPipelineCompletion(long timeout, TimeUnit timeUnit) throws InterruptedException
	{
		super.waitForPipelineCompletion(timeout, timeUnit);
	}

}
