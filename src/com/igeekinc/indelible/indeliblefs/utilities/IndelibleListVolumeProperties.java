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
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleListVolumeProperties extends IndelibleFSUtilBase
{
    private IndelibleFSObjectID	retrieveVolumeID;
	public IndelibleListVolumeProperties() throws IOException,
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
                new LongOpt("fsid", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
        };
       // Getopt getOpt = new Getopt("MultiFSTestRunner", args, "p:ns:", longOptions);
        Getopt getOpt = new Getopt("IndelibleList", args, "f:v", longOptions);
        
        int opt;
        String fsIDStr = null;
        while ((opt = getOpt.getopt()) != -1)
        {
            switch(opt)
            {
            case 'f':
                fsIDStr = getOpt.getOptarg();
                break;
            case 'v':
             	increaseVerbosity();
             	break;
            }
        }
        
        if (fsIDStr == null)
        {
            showUsage();
            System.exit(0);
        }
                
        retrieveVolumeID = (IndelibleFSObjectID) ObjectIDFactory.reconstituteFromString(fsIDStr);
        if (retrieveVolumeID == null)
        {
            System.err.println("Invalid volume ID "+retrieveVolumeID);
            System.exit(1);
        }
        
	}

	@Override
    public void runApp()
    {
        IndelibleFSVolumeIF volume = null;
        try
        {
            volume = connection.retrieveVolume(retrieveVolumeID);
        } catch (VolumeNotFoundException e1)
        {
            System.err.println("Could not find volume ID "+retrieveVolumeID);
            System.exit(1);
        } catch (IOException e1)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
            System.exit(1);
        }
        if (volume == null)
        {
            System.err.println("Could not find volume "+retrieveVolumeID);
            System.exit(1);
        }
        
        try
        {
            String [] mdResourceNames = volume.listMetaDataResources();
            for (String curMDResourceName:mdResourceNames)
            {
            	HashMap<String, Object>curMDProperties = volume.getMetaDataResource(curMDResourceName);
            	System.out.println(curMDResourceName+":");
            	for (String curPropertyName:curMDProperties.keySet())
            	{
            		Object curProperty = curMDProperties.get(curPropertyName);
            		System.out.println("\t"+curPropertyName+"="+curProperty.toString());
            	}
            }
        } catch (PermissionDeniedException e)
        {
            System.err.println("Permission denied");
            System.exit(1);
        } catch (RemoteException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            System.exit(1);
        } catch (IOException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            System.exit(1);
        }

        System.exit(0);
    }
    
    void listIndelibleFile(String listFileName, IndelibleFileNodeRemote listFile) throws RemoteException
    {
        System.out.println(listFile.totalLength()+" "+listFileName);
    }
    
    private void showUsage()
    {
        System.err.println("Usage: IndelibleListVolumeProperties --fsid <File System ID>");
    }
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
            IndelibleListVolumeProperties icfs = new IndelibleListVolumeProperties();
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
