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

import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSVolumeRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;

public class IndelibleListVolumes extends IndelibleFSUtilBase
{
    public IndelibleListVolumes() throws IOException,
            UnrecoverableKeyException, InvalidKeyException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            IllegalStateException, NoSuchProviderException, SignatureException,
            AuthenticationFailureException, InterruptedException
    {
        // TODO Auto-generated constructor stub
    }
    
    public void runApp() throws RemoteException, PermissionDeniedException, IOException
    {
        IndelibleFSObjectID [] volumeIDs = connection.listVolumes();
        for (IndelibleFSObjectID curVolumeID:volumeIDs)
        {
			System.out.println("--------------------");
        	try
			{
				IndelibleFSVolumeIF curVolume = connection.retrieveVolume(curVolumeID);

				System.out.println("Volume ID:"+curVolumeID.toString());

				String [] mdNames = curVolume.listMetaDataResources();
				for (String curMetaDataName:mdNames)
				{
					HashMap<String, Object>mdMap = curVolume.getMetaDataResource(curMetaDataName);
					if (mdMap != null)
					{
						System.out.println("\t"+curMetaDataName);

						for (String curKey:mdMap.keySet())
						{
							System.out.println("\t\t"+curKey+"="+mdMap.get(curKey));
						}
					}
				}
				
			} catch (VolumeNotFoundException e)
			{
				System.out.println("Could not retrieve volume for "+curVolumeID.toString());
			}
        	System.out.println("--------------------");
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
            IndelibleListVolumes icfs = new IndelibleListVolumes();
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
