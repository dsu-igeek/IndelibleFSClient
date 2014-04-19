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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.igeekinc.indelible.indeliblefs.CreateFileInfo;
import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDDataDescriptor;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIDMemoryDataDescriptor;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.FilePath;

public class IndelibleCopyInFile extends IndelibleFSUtilBase
{
    private FilePath	createPath;
	private File	sourceFile;
	private IndelibleFSObjectID	retrieveVolumeID;
	public IndelibleCopyInFile() throws IOException,
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
                 new LongOpt("create", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
                 new LongOpt("fsid", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
                 new LongOpt("source", LongOpt.REQUIRED_ARGUMENT, null, 's'),
                 new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
         };
         Getopt getOpt = new Getopt("MultiFSTestRunner", args, "c:f:s:v", longOptions);
         
         int opt;
         String createPathStr = null;
         String fsIDStr = null;
         String sourcePathStr = null;
         while ((opt = getOpt.getopt()) != -1)
         {
             switch(opt)
             {
             case 'c': 
                 createPathStr = getOpt.getOptarg();
                 break;
             case 'f':
                 fsIDStr = getOpt.getOptarg();
                 break;
             case 's':
                 sourcePathStr = getOpt.getOptarg();
                 break;
             case 'v':
             	increaseVerbosity();
             	break;
             }
         }
         
         if (createPathStr == null || fsIDStr == null)
         {
             showUsage();
             System.exit(0);
         }
         
         sourceFile = null;
         if (sourcePathStr != null)
         {
             sourceFile = new File(sourcePathStr);
             if (!sourceFile.exists())
             {
                 System.err.println(sourceFile+" does not exist");
                 System.exit(1);
             }
             
             if (sourceFile.isDirectory())
             {
                 System.err.println(sourceFile+" is a directory");
                 System.exit(1);
             }
         }
         createPath = FilePath.getFilePath(createPathStr);
         
         retrieveVolumeID = (IndelibleFSObjectID) ObjectIDFactory.reconstituteFromString(fsIDStr);
         if (retrieveVolumeID == null)
         {
             System.err.println("Invalid volume ID "+retrieveVolumeID);
             System.exit(1);
         }
	}

	public void runApp() throws RemoteException, PermissionDeniedException, IOException
    {
       
        
        IndelibleFSVolumeIF volume = null;
        connection.startTransaction();
        try
        {
            volume = connection.retrieveVolume(retrieveVolumeID);
        } catch (VolumeNotFoundException e1)
        {
            System.err.println("Could not find volume "+retrieveVolumeID);
            connection.rollback();
            System.exit(1);
        }
        
        IndelibleFileNodeIF checkFile = null;
        
        // Our semantics on the name of the created file are the same as Unix cp.  If the create path specifies a directory, the
        // name will be the source file name (unless no source file is specified, in which case it's an error).  If the create path
        // specifies a non-existing file, then the create path name will be used (if it specifies an existing file then we copy over the existing file)
        String createName = null;
        if (sourceFile != null)
        {
            createName = sourceFile.getName();
        }
        
        try
        {
            checkFile = volume.getObjectByPath(createPath);
        } catch (ObjectNotFoundException e1)
        {
            // The complete path does not exist so the parent must be a directory
            try
            {
                checkFile = volume.getObjectByPath(createPath.getParent());
                createName = createPath.getName();
            } catch (ObjectNotFoundException e)
            {
                // Right, no parent so we bail
                System.err.println(createPath.getParent()+" does not exist");
                connection.rollback();
                System.exit(1);
            }
            if (!checkFile.isDirectory())
            {
                System.err.println(createPath.getParent()+" is not a directory");
                connection.rollback();
                System.exit(1);
            }
        }

        IndelibleFSObjectID createdID;
        if (checkFile.isDirectory())
        {
            IndelibleDirectoryNodeIF parentDir = (IndelibleDirectoryNodeIF)checkFile;
            if (sourceFile != null)
            {
                try
                {
                    CreateFileInfo createFileInfo = parentDir.createChildFile(createName, true);
                    IndelibleFileNodeIF writeFileNode = createFileInfo.getCreatedNode();
                    if (sourceFile.exists())
                    {
                        IndelibleFSForkIF writeFork = null;
                        try
                        {
                            writeFork = writeFileNode.getFork("data", true);
                        } catch (ForkNotFoundException e)
                        {
                            System.err.println("Internal error - got fork does not exist while creating fork");
                            System.exit(1);
                        }
                        if (writeFork.length() > 0)
                            writeFork.truncate(0);
                        FileInputStream sfFIS = new FileInputStream(sourceFile);
                        long offset = 0L;
                        int numPrint = 0;
                        while (offset < sourceFile.length())
                        {
                            int bytesToCopy = 1024*1024;
                            if (sourceFile.length() - offset < 1024*1024)
                                bytesToCopy = (int)(sourceFile.length() - offset);
                            CASIDDataDescriptor curDataDescriptor = new CASIDMemoryDataDescriptor(sfFIS, bytesToCopy);
                            
                            writeFork.appendDataDescriptor(curDataDescriptor);
                            offset+=bytesToCopy;
                            numPrint++;
                            if (numPrint % 80 == 0)
                            	System.err.println(" ("+offset+" - "+(offset/(1024*1024))+" MB)");
                            System.err.print("#");
                        }
                        System.err.println();
                        if (writeFileNode.totalLength() != sourceFile.length())
                        {
                            System.err.println("Copies do not have same length");
                            connection.rollback();
                            System.exit(1);
                        }
                    }
                } catch (FileExistsException e)
                {
                    
                }
            }
            else
            {
                
            }
        }
        else
        {
            
        }
        connection.commit();
        System.exit(0);
    }
    
    private void showUsage()
    {
        System.err.println("Usage: IndelibleCreateFile --fsid <File System ID> --create <file to create> [--source <source file>]");
    }
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
            IndelibleCopyInFile icfs = new IndelibleCopyInFile();
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
