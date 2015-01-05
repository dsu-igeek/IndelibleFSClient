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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.ObjectIDFactory;

public class IndelibleListTransactionEvents extends IndelibleFSUtilBase
{

	private CASCollectionID	collectionID;
	private long	startingEventID;

	public IndelibleListTransactionEvents() throws IOException,
			UnrecoverableKeyException, InvalidKeyException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException,
			IllegalStateException, NoSuchProviderException, SignatureException,
			AuthenticationFailureException, InterruptedException
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void processArgs(String[] args) throws Exception
	{
		LongOpt [] longOptions = {
                new LongOpt("cid", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
                new LongOpt("startevent", LongOpt.REQUIRED_ARGUMENT, null, 's'),
                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
        };
       // Getopt getOpt = new Getopt("MultiFSTestRunner", args, "p:ns:", longOptions);
        Getopt getOpt = new Getopt("IndelibleListTransactionEvents", args, "c:s:v", longOptions);
        
        int opt;
        String collectionIDStr = null;
        String startingEventIDStr = null;
        while ((opt = getOpt.getopt()) != -1)
        {
            switch(opt)
            {
            case 'c':
                collectionIDStr = getOpt.getOptarg();
                break;
            case 's':
                startingEventIDStr = getOpt.getOptarg();
                break;
            case 'v':
             	increaseVerbosity();
             	break;
            }
        }
        
        if (startingEventIDStr == null || collectionIDStr == null)
        {
            showUsage();
            System.exit(0);
        }
        collectionID = (CASCollectionID) ObjectIDFactory.reconstituteFromString(collectionIDStr);
        startingEventID = Long.parseLong(startingEventIDStr);
	}

	/**
	 * @param args
	 * @throws CollectionNotFoundException 
	 * @throws IOException 
	 * @throws PermissionDeniedException 
	 */
	@Override
	public void runApp() throws CollectionNotFoundException, IOException, PermissionDeniedException
	{
		 
	    	CASServerConnectionIF serverConn = fsServer.openCASServer();
	        CASCollectionConnection collectionConnection = serverConn.openCollectionConnection(collectionID);
	        IndelibleEventIterator iterator = collectionConnection.eventsAfterID(startingEventID);
	        while(iterator.hasNext())
	        {
	        	IndelibleEvent curEvent = iterator.next();
	        	System.out.println(curEvent.toString());
	        }
	}
	

    private void showUsage()
    {
        System.err.println("Usage: IndelibleListTrnsactionEvents --cid <Collection ID> --startevent <starting event number 0 = all>");
    }

    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
        	IndelibleListTransactionEvents ilte = new IndelibleListTransactionEvents();
            ilte.run(args);
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

