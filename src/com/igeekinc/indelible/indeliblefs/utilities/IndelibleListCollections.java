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
import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.oid.CASCollectionID;

public class IndelibleListCollections extends IndelibleFSUtilBase
{
    public IndelibleListCollections() throws IOException,
            UnrecoverableKeyException, InvalidKeyException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            IllegalStateException, NoSuchProviderException, SignatureException,
            AuthenticationFailureException, InterruptedException
    {
        // TODO Auto-generated constructor stub
    }
    
    public void runApp() throws RemoteException, PermissionDeniedException, IOException, VolumeNotFoundException
    {
    	CASServerConnectionIF serverConn = fsServer.openCASServer();
    	CASCollectionID [] collections = serverConn.listCollections();
    	for (CASCollectionID curCollectionID:collections)
    	{

    		CASCollectionConnection curCollectionConn;
    		try
    		{
    			curCollectionConn = serverConn.openCollectionConnection(curCollectionID);
    			System.out.println("--------------------");
    			System.out.println(curCollectionID.toString());
    			String [] metaDataNames = curCollectionConn.listMetaDataNames();
    			for (String curMetaDataName:metaDataNames)
    			{
    				
    				Map<String, Serializable>mdMap = curCollectionConn.getMetaDataResource(curMetaDataName);
    				if (mdMap != null)
    				{
    					System.out.println("\t"+curMetaDataName);

    					for (String curKey:mdMap.keySet())
    					{
    						System.out.println("\t\t"+curKey+"="+mdMap.get(curKey));
    					}
    				}
    			}
    			System.out.println("--------------------");
    		}
    		catch (Exception e)
    		{
    			e.printStackTrace();
    		}
    	}
        System.exit(0);
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
            IndelibleListCollections icfs = new IndelibleListCollections();
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
