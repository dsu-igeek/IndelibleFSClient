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

import com.igeekinc.indelible.indeliblefs.IndelibleFSForkIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSForkRemoteInputStream;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASIdentifier;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.SHA1HashID;

public class IndelibleCompareAndVerifyFile extends IndelibleFSUtilBase
{
	private IndelibleFSObjectID	retrieveVolumeID;
	private String	sourcePathStr;
	private FilePath	sourcePath;
	private File	compareFile;
	
	
	public IndelibleCompareAndVerifyFile() throws IOException,
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
				new LongOpt("compare", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("fsid", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
				new LongOpt("source", LongOpt.REQUIRED_ARGUMENT, null, 's'),
				new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
		};
		Getopt getOpt = new Getopt("IndelibleCopyOutFile", args, "c:f:s:v", longOptions);

		int opt;
		String comparePathStr = null;
		String fsIDStr = null;
		sourcePathStr = null;
		while ((opt = getOpt.getopt()) != -1)
		{
			switch(opt)
			{
			case 'c': 
				comparePathStr = getOpt.getOptarg();
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

		if (sourcePathStr == null || fsIDStr == null || comparePathStr == null)
		{
			showUsage();
			System.exit(0);
		}

		sourcePath = FilePath.getFilePath(sourcePathStr);

		compareFile = null;
		if (comparePathStr != null)
			compareFile = new File(comparePathStr);

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

		// Our semantics on the name of the created file are the same as Unix cp.  If the create path specifies a directory, the
		// name will be the source file name (unless no source file is specified, in which case it's an error).  If the create path
		// specifies a non-existing file, then the create path name will be used (if it specifies an existing file then we copy over the existing file)

		if (!compareFile.exists())
		{
			System.err.println("Compare file "+compareFile+" does not exist");
			System.exit(1);
		}

		try
		{
			IndelibleFSForkIF readFork = sourceFile.getFork("data", false);
			if (readFork.length() != compareFile.length())
			{
				System.out.println("sourceFile "+sourceFile+" length = "+readFork.length()+", compareFile "+compareFile+" length = "+compareFile.length());
				System.exit(1);
			}
			CASIdentifier [] segmentIDs = readFork.getSegmentIDs();
			IndelibleFSForkRemoteInputStream readStream = new IndelibleFSForkRemoteInputStream(readFork);
			FileInputStream compareStream = new FileInputStream(compareFile);
			long readPosition = 0;
			
			for (int checkSegmentIDNum = 0; checkSegmentIDNum < segmentIDs.length; checkSegmentIDNum++)
			{
				CASIdentifier checkSegmentID = segmentIDs[checkSegmentIDNum];
				int checkSegmentLength = (int)checkSegmentID.getSegmentLength();
				byte [] readBuf = new byte[checkSegmentLength];
				byte [] compareBuf = new byte[checkSegmentLength];
				int bytesRead = readStream.read(readBuf);
				if (bytesRead != checkSegmentLength)
					throw new IOException("Short read at position "+readPosition+" expected "+checkSegmentLength+" got "+bytesRead);
				int verifyBytes = compareStream.read(compareBuf);
				if (bytesRead != verifyBytes)
				{
					System.err.println("bytesRead = "+bytesRead+", verifyBytes = "+verifyBytes+" at start position = "+readPosition);
					System.exit(1);
				}
				SHA1HashID recheckSHA1 = new SHA1HashID(readBuf);
				CASIdentifier recheckID = new CASIdentifier(recheckSHA1, readBuf.length);
				SHA1HashID verifySHA1 = new SHA1HashID(compareBuf);
				CASIdentifier verifyID = new CASIdentifier(verifySHA1, compareBuf.length);
				if (!recheckID.equals(checkSegmentID))
				{
					System.out.println("CASIdentifier for retrieved data ("+recheckID.toString()+") does not match expectedCASIdentifier ("+checkSegmentID.toString()+")");
				}
				if (!checkSegmentID.equals(verifyID))
				{
					System.out.println("CASIdentifier for compare data ("+verifyID.toString()+") does not match expectedCASIdentifier ("+checkSegmentID.toString()+")");
				}
				for (int checkByteNum = 0; checkByteNum < bytesRead; checkByteNum++)
				{
					if (readBuf[checkByteNum] != compareBuf[checkByteNum])
					{
						System.err.println("readBuf and verifyBuf differ at position "+(readPosition + checkByteNum));
						break;
					}
				}
				readPosition += bytesRead;
			}
			
			readStream.close();
			compareStream.close();
		} catch (ForkNotFoundException e)
		{
			System.err.println("No data fork on "+sourcePathStr);
		}
		System.exit(0);
	}


	private void showUsage()
	{
		System.err.println("Usage: IndelibleCompareAndVerifyFile --fsid <File System ID> --source <path to Indelible file> --verify <file to verify against>");
	}
	public static void main(String [] args)
	{
		int retCode = 1;
		try
		{
			IndelibleCompareAndVerifyFile icfs = new IndelibleCompareAndVerifyFile();
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
