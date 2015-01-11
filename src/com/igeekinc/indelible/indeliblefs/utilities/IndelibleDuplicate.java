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

import com.igeekinc.indelible.indeliblefs.CreateFileInfo;
import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException;
import com.igeekinc.indelible.indeliblefs.exceptions.NotFileException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.FilePath;

public class IndelibleDuplicate extends IndelibleFSUtilBase
{
    private FilePath	sourcePath;
	private FilePath	destPath;
	private IndelibleFSObjectID	retrieveVolumeID;
	public IndelibleDuplicate() throws IOException,
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
                new LongOpt("source", LongOpt.REQUIRED_ARGUMENT, null, 's'),
                new LongOpt("dest", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
        };
        Getopt getOpt = new Getopt("MultiFSTestRunner", args, "d:s:f:v", longOptions);
        
        int opt;
        String fsIDStr = null;
        String sourcePathStr = null, destPathStr = null;
        while ((opt = getOpt.getopt()) != -1)
        {
            switch(opt)
            {
            case 'f':
                fsIDStr = getOpt.getOptarg();
                break;
            case 's':
                sourcePathStr = getOpt.getOptarg();
                break;
            case 'd':
            	destPathStr = getOpt.getOptarg();
            	break;
            case 'v':
             	increaseVerbosity();
             	break;
            }
        }
        
        if (sourcePathStr == null || destPathStr == null || fsIDStr == null)
        {
            showUsage();
            System.exit(0);
        }
        
        sourcePath = FilePath.getFilePath(sourcePathStr);
        destPath = FilePath.getFilePath(destPathStr);
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
        
        IndelibleFileNodeIF sourceFile = null, checkFile = null;
        
        // Our semantics on the name of the created file are the same as Unix cp.  If the create path specifies a directory, the
        // name will be the source file name (unless no source file is specified, in which case it's an error).  If the create path
        // specifies a non-existing file, then the create path name will be used (if it specifies an existing file then we copy over the existing file)
        String createName = null;
        if (sourcePath != null)
        {
            createName = sourcePath.getName();
        }
        
        try
        {
        	sourceFile = volume.getObjectByPath(sourcePath);
        } catch (ObjectNotFoundException e1)
        {

                System.err.println(sourcePath+" does not exist");
                connection.rollback();
                System.exit(1);

        }

        IndelibleFSObjectID createdID;
        
        try
        {
        	checkFile = volume.getObjectByPath(destPath);
        	if (checkFile != null && !checkFile.isDirectory())
        	{
        		System.err.println(destPath+" exists");
        		System.exit(1);
        	}
        	createName = sourcePath.getName();
        }
        catch (ObjectNotFoundException e)
        {
        	try
        	{
        		checkFile = volume.getObjectByPath(destPath.getParent());
        		createName = destPath.getName();
        	}
        	catch (ObjectNotFoundException e1)
        	{
        		System.err.println(destPath.getParent()+" does not exist");
        		System.exit(1);
        	}
        }
        if (checkFile == null)
        {
    		System.err.println("Could not find object for "+destPath.getParent());
    		System.exit(1);
        }
        if (checkFile.isDirectory())
        {
            IndelibleDirectoryNodeIF parentDir = (IndelibleDirectoryNodeIF)checkFile;
            if (sourceFile != null)
            {

                    try {
						CreateFileInfo createFileInfo = parentDir.createChildFile(createName, sourceFile, true);
					} catch (FileExistsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ObjectNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NotFileException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
        System.err.println("Usage: IndelibleDuplicate --fsid <File System ID> --source <source file> --dest <dest file>");
    }
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
            IndelibleDuplicate icfs = new IndelibleDuplicate();
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
