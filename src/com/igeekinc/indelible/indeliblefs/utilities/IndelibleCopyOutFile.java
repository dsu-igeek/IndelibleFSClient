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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSForkRemoteInputStream;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.FilePath;

public class IndelibleCopyOutFile extends IndelibleFSUtilBase
{
    private IndelibleFSObjectID	retrieveVolumeID;
	private String	sourcePathStr;
	private FilePath	sourcePath;
	private File	outputFile;
	public IndelibleCopyOutFile() throws IOException,
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
    			new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
    			new LongOpt("fsid", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
    			new LongOpt("source", LongOpt.REQUIRED_ARGUMENT, null, 's'),
    			new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
    	};
    	Getopt getOpt = new Getopt("MultiFSTestRunner", args, "o:f:s:", longOptions);

    	int opt;
    	String outputPathStr = null;
    	String fsIDStr = null;
    	sourcePathStr = null;
    	while ((opt = getOpt.getopt()) != -1)
    	{
    		switch(opt)
    		{
    		case 'o': 
    			outputPathStr = getOpt.getOptarg();
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

    	if (sourcePathStr == null || fsIDStr == null)
    	{
    		showUsage();
    		System.exit(0);
    	}

    	sourcePath = FilePath.getFilePath(sourcePathStr);

    	outputFile = null;
    	if (outputPathStr != null)
    		outputFile = new File(outputPathStr);

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

    	IndelibleFileNodeIF sourceFile = null;
    	if (sourcePathStr != null)
    	{
    		try
    		{
    			sourceFile = volume.getObjectByPath(sourcePath);
    		}
    		catch (ObjectNotFoundException e)
    		{
    			System.err.println("Could not find source Indelible file "+sourcePath);
    			System.exit(1);
    		}

    		if (sourceFile.isDirectory())
    		{
    			System.err.println(sourceFile+" is a directory");
    			System.exit(1);
    		}
    	}

    	OutputStream writeStream;
    	// Our semantics on the name of the created file are the same as Unix cp.  If the create path specifies a directory, the
    	// name will be the source file name (unless no source file is specified, in which case it's an error).  If the create path
    	// specifies a non-existing file, then the create path name will be used (if it specifies an existing file then we copy over the existing file)

    	if (outputFile != null)
    	{
    		if (!outputFile.exists())
    		{
    			// The complete path does not exist so the parent must be a directory

    			String createName = outputFile.getName();
    			String outputPathStr = outputFile.getAbsolutePath();
    			outputFile = outputFile.getParentFile();

    			if (outputFile == null)
    			{
    				System.err.println("Cannot find parent dir for "+outputPathStr);
    				System.exit(1);
    			}
    			if (!outputFile.exists())
    			{
    				// Right, no parent so we bail
    				System.err.println(outputFile+" does not exist");
    				System.exit(1);
    			}
    			if (!outputFile.isDirectory())
    			{
    				System.err.println(outputFile+" is not a directory");
    				System.exit(1);
    			}
    			// We'll create the file, just fall through
    		}
    		else
    		{
    			if (outputFile.isDirectory())
    			{
    				// They specified a directory as the output path, so we'll use the source name for the file
    				outputFile = new File(outputFile, sourcePath.getName());
    			}
    		}
    		writeStream = new FileOutputStream(outputFile);
    	}
    	else
    	{
    		// No output file specified, dump to standard out
    		writeStream = System.out;
    	}

    	try
    	{
    		IndelibleFSForkIF readFork = sourceFile.getFork("data", false);
    		IndelibleFSForkRemoteInputStream readStream = new IndelibleFSForkRemoteInputStream(readFork);
    		int bytesRead = 0;
    		byte [] readBuf = new byte[1024*1024];
    		while ((bytesRead = readStream.read(readBuf)) > 0)
    		{
    			writeStream.write(readBuf, 0, bytesRead);
    		}

    		writeStream.close();
    	} catch (ForkNotFoundException e)
		{
    		System.err.println("No data fork on "+sourcePathStr);
		}
    	System.exit(0);
    }
    
    private void showUsage()
    {
        System.err.println("Usage: IndelibleCopyOutFile --fsid <File System ID> --source <path to Indelible file> [--output <file to output to>]");
    }
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
            IndelibleCopyOutFile icfs = new IndelibleCopyOutFile();
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
