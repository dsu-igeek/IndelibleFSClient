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
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.proxies.RemoteCASServerProxy;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleSetCollectionProperty extends IndelibleFSUtilBase
{
    private CASCollectionID	setCollectionID;
	private String	clearPropertySetName;

	public IndelibleSetCollectionProperty() throws IOException,
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
                new LongOpt("cid", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
                new LongOpt("clear", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
                new LongOpt("set", LongOpt.REQUIRED_ARGUMENT, null, 's'),
                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
        };
        Getopt getOpt = new Getopt("IndelibleList", args, "i:c:s:v", longOptions);
        
        int opt;
        String collectionIDStr = null;
        clearPropertySetName = null;
        String setPropertyValueString = null;
        String setProperty = null;
        String setValue = null;
        while ((opt = getOpt.getopt()) != -1)
        {
            switch(opt)
            {
            case 'i': 
            	collectionIDStr = getOpt.getOptarg();
                break;
            case 'c':
            	clearPropertySetName = getOpt.getOptarg();
                break;
                /*
            case 's':
            	setPropertyValueString = getOpt.getOptarg();
            	if (setPropertyValueString.indexOf('=') < 0)
            	{
            		showUsage();
            		System.exit(1);
            	}
            	setProperty = setPropertyValueString.substring(0, setPropertyValueString.indexOf('='));
            	setValue = setPropertyValueString.substring(setPropertyValueString.indexOf('=') + 1);
            	break;
            	*/
            case 'v':
             	increaseVerbosity();
             	break;
            }
        }
        setCollectionID = (CASCollectionID) ObjectIDFactory.reconstituteFromString(collectionIDStr);
	}

    @Override
	public void runApp() throws RemoteException, PermissionDeniedException, IOException, VolumeNotFoundException
    {
    	
    	CASServerConnectionIF serverConn = fsServer.openCASServer();
    	CASCollectionConnection curCollectionConn;

    	try
		{
			curCollectionConn = serverConn.getCollectionConnection(setCollectionID);
			if (clearPropertySetName != null)
			{
				curCollectionConn.startTransaction();
				curCollectionConn.setMetaDataResource(clearPropertySetName, null);
				curCollectionConn.commit();
			}
		} catch (CollectionNotFoundException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
    	
        System.exit(0);
    }
    
    void listIndelibleFile(String listFileName, IndelibleFileNodeRemote listFile) throws RemoteException
    {
        System.out.println(listFile.totalLength()+" "+listFileName);
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
            IndelibleSetCollectionProperty icfs = new IndelibleSetCollectionProperty();
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
