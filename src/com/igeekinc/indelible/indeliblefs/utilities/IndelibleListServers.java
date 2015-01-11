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
 
package com.igeekinc.indelible.indeliblefs.utilities;

import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSServer;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerAddedEvent;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerListListener;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSServerRemovedEvent;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleListServers extends IndelibleFSUtilBase
{
    public IndelibleListServers() throws IOException,
            UnrecoverableKeyException, InvalidKeyException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            IllegalStateException, NoSuchProviderException, SignatureException,
            AuthenticationFailureException, InterruptedException
    {
        // TODO Auto-generated constructor stub
    }
    
    public void runApp() throws RemoteException, PermissionDeniedException, IOException, VolumeNotFoundException
    {
    	IndelibleFSServer [] servers = IndelibleFSClient.listServers();
        HashMap<EntityID, ArrayList<IndelibleFSServer>>serversByAuthenticationServer = new HashMap<EntityID, ArrayList<IndelibleFSServer>>();
        for (IndelibleFSServer curServer:servers)
        {
        	EntityID securityServerID = curServer.getSecurityServerID();
        	ArrayList<IndelibleFSServer>serverList = serversByAuthenticationServer.get(securityServerID);
        	if (serverList == null)
        	{
        		serverList = new ArrayList<IndelibleFSServer>();
        		serversByAuthenticationServer.put(securityServerID, serverList);
        	}
        	serverList.add(curServer);
        }
        for (EntityID curSecurityServerID:serversByAuthenticationServer.keySet())
        {
        	System.out.println("====================");
        	System.out.println("Security server: "+curSecurityServerID.toString());
        	ArrayList<IndelibleFSServer>serverList = serversByAuthenticationServer.get(curSecurityServerID);
        	for (IndelibleFSServer curServer:serverList)
        	{
        		printServer(curServer);
        	}
        	System.out.println("====================");
        }
        
        IndelibleFSClient.addIndelibleFSServerListListener(new IndelibleFSServerListListener()
		{
			
			@Override
			public void indelibleFSServerRemoved(
					IndelibleFSServerRemovedEvent removedEvent)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void indelibleFSServerAdded(IndelibleFSServerAddedEvent addedEvent)
			{
				try
				{
		        	System.out.println("====================");
		        	try
					{
						System.out.println("Security server: "+addedEvent.getAddedServer().getSecurityServerID().toString());
					} catch (IOException e)
					{
						// TODO Auto-generated catch block
						Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
					}
					printServer(addedEvent.getAddedServer());
					System.out.println("====================");
				} catch (RemoteException e)
				{
					// TODO Auto-generated catch block
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				}
			}
		});
        try
        {
        	System.out.println("Waiting for servers to be discovered");
        	Thread.sleep(30000);
        }
        catch (InterruptedException e)
        {
        	
        }
        System.exit(0);
    }

	protected void printServer(IndelibleFSServer curServer)
			throws RemoteException
	{
		System.out.println("--------------------");
		try
		{
			System.out.println(curServer.getServerAddress()+":"+curServer.getServerPort()+" "+curServer.getServerID());
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		System.out.println("--------------------");
	}

    private void showUsage()
    {
        System.err.println("Usage: IndelibleVolumeList");
    }
    
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
            IndelibleListServers icfs = new IndelibleListServers();
            icfs.run(args);
            retCode = 0;
        } catch (Throwable t)
        {
        	t.printStackTrace();
        }
        finally
        {
            System.exit(retCode);
        }
    }
}
