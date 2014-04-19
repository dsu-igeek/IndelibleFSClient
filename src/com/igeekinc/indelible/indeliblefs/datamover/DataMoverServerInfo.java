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
 
package com.igeekinc.indelible.indeliblefs.datamover;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.security.AuthenticatedConnection;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class DataMoverServerInfo
{
    private EntityID serverID;
    private DataInputStream receiveSocketIS;
    private AuthenticatedConnection connection;
    private LinkedList<DataMoverCommand>outstandingCommands;
    private long commandNum;
    private Thread commandReplyProcessor;
    public DataMoverServerInfo(EntityID serverID)
    {
        this.serverID = serverID;
        outstandingCommands = new LinkedList<DataMoverCommand>();
    }
    
    protected synchronized void setConnection(AuthenticatedConnection connection)
    {
        this.connection = connection;
        this.receiveSocketIS = new DataInputStream(connection.getInputStream());
        commandNum = 0;
        commandReplyProcessor = new Thread(new Runnable(){
        	public void run() {
        		commandReplyLoop();
        	}
        }, "Command Reply");
        commandReplyProcessor.start();
        notifyAll();
    }

    public synchronized boolean checkConnection(long timeout)
    {
    	if (connection == null)
    	{
    		try
    		{
    			wait(timeout);
    		}
    		catch (InterruptedException e)
    		{

    		}
    	}
    	if (connection == null)
    		return false;
    	return true;
    }
    
    public EntityID getServerID()
    {
        return serverID;
    }

    public OutputStream getReceiveSocketOS()
    {
        return connection.getOutputStream();
    }

    public InputStream getReceiveSocketIS()
    {
        return receiveSocketIS;
    }

	public void sendSynchronousCommand(DataMoverCommand command) throws IOException
    {
		MoverFuture future = sendAsynchronousCommand(command);
		synchronized(command)
		{
			try
			{
				future.get(60, TimeUnit.SECONDS);
			} catch (InterruptedException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			} catch (ExecutionException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			} catch (TimeoutException e)
			{
				throw new IOException("Command timed out");
			}
		}
    }
	
	public MoverFuture sendAsynchronousCommand(DataMoverCommand command) throws IOException
	{
		synchronized(outstandingCommands)
		{
			command.setCommandNum(commandNum);
			commandNum ++;
			outstandingCommands.add(command);
		}
		if (connection.getOutputStream() == null)
			throw new IOException("Connection closed");
		try
		{
			connection.getOutputStream().write(command.getCommandBuf());
			connection.getOutputStream().flush();
		}
		catch (IOException e)
		{
			connection = null;
			throw e;
		}
		return command.getFuture();
	}
	
	public void commandReplyLoop()
	{
		while (true)
		{
			synchronized(receiveSocketIS)
			{
				int returnStatus;
				try
				{
					returnStatus = receiveSocketIS.read();
					if (returnStatus < 0)
					{
						Logger.getLogger(getClass()).error("Socket closed unexpectedly, exiting commandReplyLoop");
						break;
					}
					DataMoverCommand ourCommand;
					if (returnStatus == DataMoverSource.kCommandOK)
					{
						synchronized(outstandingCommands)
						{
							ourCommand = outstandingCommands.getFirst();    // Don't remove from the queue until we're done processing the response
							// and data.  We don't want another thread coming in, starting a command
							// and then trying to read from the socket.  Our lock on the socket input stream should be enough but let's 
							// be careful
						}
						int expectedBytes = ourCommand.expectedOKBytes();
						byte [] okBuf = new byte[expectedBytes];
						receiveSocketIS.readFully(okBuf);
						try
						{
							ourCommand.processOKResult(okBuf, receiveSocketIS);
						}
						finally
						{
							ourCommand.setFinished();
							synchronized(outstandingCommands)
							{
								outstandingCommands.remove();
							}
						}
					}
					else
					{
						if (returnStatus == DataMoverSource.kConnectionAborted)
						{
							break;
						}
					}
				} catch (IOException e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
					break;
				}
			}
		}
		DataMoverReceiver.getDataMoverReceiver().closeServer(this);
		synchronized(outstandingCommands)
		{
			try
			{
				connection.getOutputStream().close();
				receiveSocketIS.close();
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
			// TODO - standardize on one wait loop and get rid of this
			if (outstandingCommands.size() > 0)
			{
				DataMoverCommand wakeCommand = outstandingCommands.getFirst();
				synchronized(wakeCommand)
				{
					wakeCommand.notify();	// Break everyone else
				}
			}
		}
	}
}
