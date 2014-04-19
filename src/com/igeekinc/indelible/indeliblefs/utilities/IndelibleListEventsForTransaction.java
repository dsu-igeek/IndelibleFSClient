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
import java.util.Date;

import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.ObjectIDFactory;

public class IndelibleListEventsForTransaction extends IndelibleFSUtilBase
{

	private CASCollectionID	collectionID;
	private long	transactionEventID;

	public IndelibleListEventsForTransaction() throws IOException,
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
                new LongOpt("transaction", LongOpt.REQUIRED_ARGUMENT, null, 't'),
                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
        };
       // Getopt getOpt = new Getopt("MultiFSTestRunner", args, "p:ns:", longOptions);
        Getopt getOpt = new Getopt("MultiFSTestRunner", args, "c:s:t:v", longOptions);
        
        int opt;
        String collectionIDStr = null;
        String transactionIDStr = null;
        while ((opt = getOpt.getopt()) != -1)
        {
            switch(opt)
            {
            case 'c':
                collectionIDStr = getOpt.getOptarg();
                break;
            case 't':
            	transactionIDStr = getOpt.getOptarg();
            	break;
            case 'v':
             	increaseVerbosity();
             	break;
            }
        }
        
        if (transactionIDStr == null || collectionIDStr == null)
        {
            showUsage();
            System.exit(0);
        }
        collectionID = (CASCollectionID) ObjectIDFactory.reconstituteFromString(collectionIDStr);
        transactionEventID = Long.parseLong(transactionIDStr);
	}

	/**
	 * @param args
	 * @throws CollectionNotFoundException 
	 * @throws IOException 
	 */
	public void runApp() throws CollectionNotFoundException, IOException
	{
		 
	    	CASServerConnectionIF serverConn = fsServer.openCASServer();
	        CASCollectionConnection collectionConnection = serverConn.getCollectionConnection(collectionID);
	        IndelibleEventIterator transactionIterator = collectionConnection.getTransactionEventsAfterEventID(transactionEventID, 0);
	        if (transactionIterator.hasNext())
	        {
	        	IndelibleEvent indelibleEvent = transactionIterator.next();
	        	if (indelibleEvent.getEventID() == transactionEventID)
	        	{
	        		if (indelibleEvent instanceof TransactionCommittedEvent)
	        		{
	        			TransactionCommittedEvent transactionEvent = (TransactionCommittedEvent)indelibleEvent;
	        			IndelibleEventIterator iterator = collectionConnection.getEventsForTransaction(transactionEvent.getTransaction());
	        			while(iterator.hasNext())
	        			{
	        				IndelibleEvent curEvent = iterator.next();
	        				Date eventDate = new Date(curEvent.getTimestamp());
	        				System.out.println(curEvent.getEventID()+":"+eventDate+" ("+curEvent.toString()+")");
	        			}
	        			System.exit(0);
	        		}
	        	}
	        }
	        System.err.println("Could not find transaction event "+transactionEventID);
	        System.exit(1);
	}
	

    private void showUsage()
    {
        System.err.println("Usage: IndelibleListTrnsactionEvents --cid <Collection ID> --transaction <transaction event ID>");
    }

    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
        	IndelibleListEventsForTransaction ilte = new IndelibleListEventsForTransaction();
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
