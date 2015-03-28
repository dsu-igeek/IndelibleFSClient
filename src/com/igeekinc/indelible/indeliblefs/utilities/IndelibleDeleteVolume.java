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

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleDeleteVolume extends IndelibleFSUtilBase
{
	private IndelibleFSObjectID	retrieveVolumeID;

	public IndelibleDeleteVolume() throws IOException,
            UnrecoverableKeyException, InvalidKeyException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            IllegalStateException, NoSuchProviderException, SignatureException,
            AuthenticationFailureException, InterruptedException
    {
        // TODO Auto-generated constructor stub
    }
    
    public void runApp()
    {
        try
        {
            connection.deleteVolume(retrieveVolumeID);
        } catch (VolumeNotFoundException e1)
        {
            System.err.println("Could not find volume ID "+retrieveVolumeID);
            System.exit(1);
        } catch (IOException e1)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
            System.exit(1);
        } catch (PermissionDeniedException e)
		{
			System.err.println("Permission denied");
			System.exit(1);
		}
        logger.info("Deleted volume "+retrieveVolumeID.toString());
        System.exit(0);
    }
    
    void listIndelibleFile(String listFileName, IndelibleFileNodeIF listFile) throws IOException
    {
        System.out.println(listFile.totalLength()+" "+listFileName);
    }
    
    private void showUsage()
    {
        System.err.println("Usage: IndelibleList --fsid <File System ID> --path <path>");
    }
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
            IndelibleDeleteVolume icfs = new IndelibleDeleteVolume();
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

	@Override
	public void processArgs(String[] args)
	{
		LongOpt [] longOptions = {
                new LongOpt("fsid", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
        };
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
                
        ObjectID reconstitutedOID = ObjectIDFactory.reconstituteFromString(fsIDStr);
        if (!(reconstitutedOID instanceof IndelibleFSObjectID))
        {
        	System.err.println(fsIDStr+ " is not an IndelibleFSObjectID");
        	System.exit(2);
        }
		retrieveVolumeID = (IndelibleFSObjectID) reconstitutedOID;
        if (retrieveVolumeID == null)
        {
            System.err.println("Invalid volume ID "+retrieveVolumeID);
            System.exit(1);
        }
	}
    
    
}
