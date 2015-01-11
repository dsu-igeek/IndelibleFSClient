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
import java.util.Properties;

import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;

public class IndelibleAddCASStore extends IndelibleFSUtilBase
{
    private Properties	addCASStoreProperties;

	public IndelibleAddCASStore() throws IOException,
            UnrecoverableKeyException, InvalidKeyException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            IllegalStateException, NoSuchProviderException, SignatureException,
            AuthenticationFailureException, InterruptedException
    {
        // TODO Auto-generated constructor stub
    }
    
    public void runApp() throws RemoteException, PermissionDeniedException, IOException, VolumeNotFoundException
    {

    	CASServerConnectionIF casServerConnection = fsServer.openCASServer();
    	//casServerConnection.createStoresForProperties(addCASStoreProperties);
        System.exit(0);
    }
    
    
    @Override
	public void processArgs(String[] args) throws IOException
    {
    	LongOpt [] longOptions = {
    			new LongOpt("propertiesFile", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
    			new LongOpt("property", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
    			new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
    	};
    	Getopt getOpt = new Getopt("IndelibleAddCASStore", args, "p:f:v", longOptions);

    	int opt;
    	addCASStoreProperties = new Properties();

    	while ((opt = getOpt.getopt()) != -1)
    	{
    		switch(opt)
    		{
    		case 'p': 
    			String curProperty = getOpt.getOptarg();
    			String curPropertyName = curProperty.substring(0, curProperty.indexOf('='));
    			String curPropertyValue = curProperty.substring(curProperty.indexOf('=') + 1);
    			addCASStoreProperties.put(curPropertyName, curPropertyValue);
    			break;
    		case 'f':
    			String propertiesFilePath = getOpt.getOptarg();
    			FileInputStream propertiesStream = new FileInputStream(new File(propertiesFilePath));
    			addCASStoreProperties.load(propertiesStream);
    			break;
            case 'v':
            	increaseVerbosity();
            	break;
    		}
    	}
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
            IndelibleAddCASStore icfs = new IndelibleAddCASStore();
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
