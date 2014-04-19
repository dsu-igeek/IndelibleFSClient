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

import com.igeekinc.indelible.indeliblefs.CreateDirectoryInfo;
import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleCreateDirectory extends IndelibleFSUtilBase
{
    private IndelibleFSObjectID	retrieveVolumeID;
	private FilePath	dirPath;
	private boolean	makeAll;
	public IndelibleCreateDirectory() throws IOException,
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
                new LongOpt("dir", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
                new LongOpt("fsid", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
                new LongOpt("makeall", LongOpt.NO_ARGUMENT, null, 'm'),
                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
        };
       // Getopt getOpt = new Getopt("MultiFSTestRunner", args, "p:ns:", longOptions);
        Getopt getOpt = new Getopt("MultiFSTestRunner", args, "d:f:mv", longOptions);
        
        int opt;
        String dirPathStr = null;
        String fsIDStr = null;
        makeAll = false;
        while ((opt = getOpt.getopt()) != -1)
        {
            switch(opt)
            {
            case 'd': 
                dirPathStr = getOpt.getOptarg();
                break;
            case 'f':
                fsIDStr = getOpt.getOptarg();
                break;
            case 'm':
                makeAll = true;
                break;
            case 'v':
             	increaseVerbosity();
             	break;
            }
        }
        
        if (dirPathStr == null || fsIDStr == null)
        {
            showUsage();
            System.exit(0);
        }
        
        dirPath = FilePath.getFilePath(dirPathStr);
        
        retrieveVolumeID = (IndelibleFSObjectID) ObjectIDFactory.reconstituteFromString(fsIDStr);
        if (retrieveVolumeID == null)
        {
            System.err.println("Invalid volume ID "+retrieveVolumeID);
            System.exit(1);
        }
	}

	public void runApp() throws RemoteException, PermissionDeniedException, IOException, VolumeNotFoundException, ObjectNotFoundException
    {
        
        
        IndelibleFSVolumeIF volume = connection.retrieveVolume(retrieveVolumeID);
        if (volume == null)
        {
            System.err.println("Could not find volume "+retrieveVolumeID);
            System.exit(1);
        }
        
        IndelibleDirectoryNodeIF parentFile = null;
        FilePath parentPath = dirPath.getParent();
        String dirName = dirPath.getName();
        if (makeAll)
        {
            
        }
        else
        {

        	IndelibleFileNodeIF cf = volume.getObjectByPath(parentPath);
            if (!cf.isDirectory())
            {
                System.err.println(parentPath+" is not a directory");
                System.exit(1);
            }
            
            parentFile = (IndelibleDirectoryNodeIF)cf;
        }
        if (parentFile == null)
        {
    		System.err.println("Could not find object for "+parentPath);
    		System.exit(1);
        }
        try
        {
            CreateDirectoryInfo cdir =  parentFile.createChildDirectory(dirName);
            System.out.println(cdir.getCreatedNode().getObjectID());
        } catch (FileExistsException e)
        {
            System.err.println(dirPath+" exists");
            System.exit(1);
        }
        System.exit(0);
    }
    
    private void showUsage()
    {
        System.err.println("Usage: IndelibleCreateDirectory --fsid <File System ID> --dir <directory path> [--makeall]");
    }
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
            IndelibleCreateDirectory icfs = new IndelibleCreateDirectory();
            icfs.run(args);
            retCode = 0;
        } catch (UnrecoverableKeyException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (InvalidKeyException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (KeyStoreException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (NoSuchAlgorithmException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (CertificateException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (IllegalStateException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (NoSuchProviderException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (SignatureException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (IOException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (AuthenticationFailureException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        } catch (InterruptedException e)
        {
            Logger.getLogger(IndelibleCreateDirectory.class).error(new ErrorLogMessage("Caught exception"), e);
        }
        finally
        {
            System.exit(retCode);
        }
    }
}
