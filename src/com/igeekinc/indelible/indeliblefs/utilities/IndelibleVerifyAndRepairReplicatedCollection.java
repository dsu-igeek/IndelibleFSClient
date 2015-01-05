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
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSServer;
import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEvent;
import com.igeekinc.indelible.indeliblefs.events.IndelibleEventIterator;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSClient;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionSegmentEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.DataVersionInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.SegmentInfo;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.SegmentNotFound;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleVerifyAndRepairReplicatedCollection extends IndelibleFSUtilBase
{
	private CASCollectionID	casCollectionID;
	private EntityID	masterServerID;
	private EntityID	checkServerID;
	private ArrayList<String>	serversToConnectTo;
	private boolean	repair;

	public IndelibleVerifyAndRepairReplicatedCollection() throws UnrecoverableKeyException, InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IllegalStateException, NoSuchProviderException, SignatureException, IOException, AuthenticationFailureException, InterruptedException
	{
		super();
	}
	
    @Override
	public void processArgs(String[] args) throws Exception
	{
    	Logger.getLogger(getClass()).setLevel(Level.ERROR);
        LongOpt [] longOptions = {
                new LongOpt("ccid", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
                new LongOpt("master", LongOpt.REQUIRED_ARGUMENT, null, 'm'),
                new LongOpt("check", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
                new LongOpt("repair", LongOpt.NO_ARGUMENT, null, 'r'),
                new LongOpt("server", LongOpt.REQUIRED_ARGUMENT, null, 's'),
                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
        };
       // Getopt getOpt = new Getopt("MultiFSTestRunner", args, "p:ns:", longOptions);
        Getopt getOpt = new Getopt("IndelibleVerifyAndRepairReplicatedCollection", args, "i:m:c:s:rv", longOptions);
        
        int opt;
        String casCollectionIDStr = null;
        String masterServerIDStr = null, checkServerIDStr = null;
        serversToConnectTo = new ArrayList<String>();
        repair = false;
        while ((opt = getOpt.getopt()) != -1)
        {
            switch(opt)
            {
            case 'i':
                casCollectionIDStr = getOpt.getOptarg();
                break;
            case 'm':
            	masterServerIDStr = getOpt.getOptarg();
            	break;
            case 'c':
            	checkServerIDStr = getOpt.getOptarg();
            	break;
            case 'r':
            	repair = true;
            	break;
            case 's':
            	serversToConnectTo.add(getOpt.getOptarg());
            	break;
            case 'v':
             	increaseVerbosity();
             	break;
            }
        }
        
        if (masterServerIDStr == null || checkServerIDStr == null || casCollectionIDStr == null)
        {
            showUsage();
            System.exit(1);
        }
        
        ObjectID objectID = ObjectIDFactory.reconstituteFromString(casCollectionIDStr);
        if (!(objectID instanceof CASCollectionID))
        {
        	System.err.println(casCollectionIDStr+" is not a CAS Collection ID");
        	System.exit(0);
        }
        casCollectionID = (CASCollectionID)objectID;
        masterServerID = (EntityID) ObjectIDFactory.reconstituteFromString(masterServerIDStr);
        checkServerID = (EntityID) ObjectIDFactory.reconstituteFromString(checkServerIDStr);
	}

    @Override
	public void runApp()
    {
    	
        for (String curServerToConnectTo:serversToConnectTo)
        {
        	String connectToServerName;
        	int connectToServerPort;
        	if (curServerToConnectTo.indexOf(':') >= 0)
            {
        		connectToServerName = curServerToConnectTo.substring(0, curServerToConnectTo.indexOf(':'));
        		connectToServerPort = Integer.parseInt(curServerToConnectTo.substring(curServerToConnectTo.indexOf(':') + 1));
            }
            else
            {
            	connectToServerName = curServerToConnectTo;
            	connectToServerPort = 50901;
            }
        	try
    		{
    			IndelibleFSClient.connectToServer(connectToServerName, connectToServerPort);
    			System.out.println("Adding server "+connectToServerName+":"+connectToServerPort);
    		} catch (Exception e)
    		{
    			System.err.println("Could not connect to "+connectToServerName+":"+connectToServerPort);
    			System.exit(1);
    		}
        }
        
        IndelibleFSServer masterServer = null, checkServer = null;
        IndelibleFSServer [] servers = IndelibleFSClient.listServers();
        for (IndelibleFSServer curServer:servers)
        {
        	try
			{
				System.out.println("Found server: "+curServer.getServerID() +"("+curServer.getServerAddress()+")");
				if (curServer.getServerID().equals(masterServerID))
				{
					masterServer = curServer;
					System.out.println("MASTER: "+masterServerID);
				}
				
				if (curServer.getServerID().equals(checkServerID))
				{
					checkServer = curServer;
					System.out.println("CHECK: "+checkServerID);
				}
			} catch (IOException e)
			{
				System.err.println("Error getting address, continuing");
			}
        }
        
        try
		{
			verify(casCollectionID, masterServer, checkServer, repair);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (PermissionDeniedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
    }
    
    private void verify(CASCollectionID casCollectionID, IndelibleFSServer masterServer, IndelibleFSServer checkServer, boolean repair) throws IOException, PermissionDeniedException
    {
    	CASServerConnectionIF masterCASServerConn = masterServer.openCASServer();
    	CASServerConnectionIF checkCASServerConn = checkServer.openCASServer();
    	CASCollectionConnection masterCASConn, checkCASConn;
    	try
		{
    		masterCASConn = masterCASServerConn.openCollectionConnection(casCollectionID);
		} catch (CollectionNotFoundException e)
		{
			System.err.println("Could not find "+casCollectionID+" in master server");
			return ;
		}
    	try
		{
    		checkCASConn = checkCASServerConn.openCollectionConnection(casCollectionID);
		} catch (CollectionNotFoundException e)
		{
			System.err.println("Could not find "+casCollectionID+" in check server");
			return ;
		}
    	
    	// Authorize the servers to talk to each other using our session
    	checkCASServerConn.addConnectedServer(masterCASServerConn.getServerID(), masterCASServerConn.getSecurityServerID());
    	checkCASServerConn.addClientSessionAuthentication(masterCASServerConn.getSessionAuthentication());
    	masterCASServerConn.addClientSessionAuthentication(checkCASServerConn.getSessionAuthentication());
    	IndelibleEventIterator masterTransactionEventIterator = masterCASConn.getTransactionEventsAfterEventID(0, 1);
    	
    	int totalEventsProcessed = 0, totalTransactionsProcessed = 0;
    	while (masterTransactionEventIterator.hasNext())
    	{
    		IndelibleEvent event = masterTransactionEventIterator.next();
    		if (event instanceof TransactionCommittedEvent)
    		{
    			TransactionCommittedEvent transactionEvent = (TransactionCommittedEvent)event;
    			IndelibleFSTransaction sourceTransaction = transactionEvent.getTransaction();
    			IndelibleEventIterator eventsIterator = masterCASConn.getEventsForTransaction(sourceTransaction);
    			IndelibleVersion transactionVersion = sourceTransaction.getVersion();
    			int numEventsChecked = 0;
    			while (eventsIterator.hasNext())
    			{
    				IndelibleEvent curEvent = eventsIterator.next();

    				if (curEvent instanceof CASCollectionEvent)
    				{
    					CASCollectionEvent curCASEvent = (CASCollectionEvent)curEvent;
    					switch(curCASEvent.getEventType())
    					{
    					case kMetadataModified:
    						CASIDDataDescriptor masterMetaData = masterCASConn.getMetaDataForReplication();
    						CASIDDataDescriptor checkMetaData = checkCASConn.getMetaDataForReplication();
    						if (!masterMetaData.getCASIdentifier().equals(checkMetaData.getCASIdentifier()))
    						{
    							logger.error(new ErrorLogMessage("Meta data does not compare for event {0}", curCASEvent.getEventID()));
    							// Currently metadata is not versioned, so changes may occur.  This needs to be fixed, but overwriting the MD here is probably
    							// a really bad idea so we don't repair that
    						}
    						break;
    					case kSegmentCreated:
    						ObjectID checkSegmentID = ((CASCollectionSegmentEvent)curCASEvent).getSegmentID();
    						SegmentInfo masterInfo = masterCASConn.retrieveSegmentInfo(checkSegmentID, transactionVersion, RetrieveVersionFlags.kExact);
    						if (masterInfo != null)
    						{
    							SegmentInfo checkInfo = checkCASConn.retrieveSegmentInfo(checkSegmentID, transactionVersion, RetrieveVersionFlags.kExact);
    							if (checkInfo != null)
    							{
    								if (!masterInfo.equals(checkInfo))
    								{
    									logger.error(new ErrorLogMessage("Data does not compare for segment {0}", checkSegmentID));
    								}
    								boolean verified = false;
    								try
    								{
    									verified = checkCASConn.verifySegment(checkSegmentID, transactionVersion, RetrieveVersionFlags.kExact);
    								}
    								catch (IOException e)
    								{
    									logger.error(new ErrorLogMessage("Data does not verify on check server for segment {0}", checkSegmentID), e);
    								}
    								if (!verified)
    								{
    									logger.error(new ErrorLogMessage("Data does not verify on check server for segment {0}", checkSegmentID));
    									if (repair)
    									{
    										try
											{
												DataVersionInfo masterData = masterCASConn.retrieveSegment(checkSegmentID, transactionVersion, RetrieveVersionFlags.kExact);
												checkCASConn.repairSegment(checkSegmentID, transactionVersion, masterData);
											} catch (SegmentNotFound e)
											{
												Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
											}
    									}
    								}
    							}
    							else
    							{
    								logger.error(new ErrorLogMessage("Could not retrieve check data for segment {0}", checkSegmentID));
    							}
    						}
    						else
    						{
    							logger.error(new ErrorLogMessage("Could not retrieve master data for segment {0}", checkSegmentID));
    						}
    						break;
    					case kSegmentReleased:
    						break;
						case kTransactionCommited:
							break;
						default:
							break;
    					}
    				}
    				numEventsChecked++;
    				if (numEventsChecked % 1000 == 0)
    				{
    					logger.error(new ErrorLogMessage("Event {0} for transaction {1} ({2})", new Serializable [] {numEventsChecked, transactionEvent.getEventID(), transactionEvent.getDate()}));
    				}
    			}
    			logger.error(new ErrorLogMessage("Processed {0} events for transaction {1} ({2})", new Serializable [] {numEventsChecked, transactionEvent.getEventID(), transactionEvent.getDate()}));
        		totalEventsProcessed += numEventsChecked;
    		}
    		totalTransactionsProcessed ++;
    	}
    	logger.error(new ErrorLogMessage("Processed {0} events and {1} transactions", new Serializable [] {totalEventsProcessed, totalTransactionsProcessed}));
    }
    
	void showUsage()
    {
    	System.err.println("usage: IndelibleVerifyAndRepairReplicatedCollection --server <add server> --master <master server ID> --check <check server ID> --ccid <collection id to check> [ --repair ]" );
    }
    
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
        	IndelibleVerifyAndRepairReplicatedCollection app = new IndelibleVerifyAndRepairReplicatedCollection();
            app.run(args);
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
