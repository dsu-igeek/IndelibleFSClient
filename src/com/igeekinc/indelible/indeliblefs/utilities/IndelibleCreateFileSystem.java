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

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleCreateFileSystem extends IndelibleFSUtilBase
{
    public IndelibleCreateFileSystem() throws IOException,
            UnrecoverableKeyException, InvalidKeyException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            IllegalStateException, NoSuchProviderException, SignatureException,
            AuthenticationFailureException, InterruptedException
    {
        // TODO Auto-generated constructor stub
    }
    
    public void runApp() throws RemoteException, PermissionDeniedException, IOException
    {
        IndelibleFSVolumeIF newVolume = createNewFileSystem(null);
        System.out.println(newVolume.getVolumeID().toString());
    }
    
    public static void main(String [] args)
    {
        int retCode = 1;
        try
        {
            IndelibleCreateFileSystem icfs = new IndelibleCreateFileSystem();
            icfs.run(args);
            retCode = 0;
        } catch (Exception e)
        {
            Logger.getLogger(IndelibleCreateFileSystem.class).error(new ErrorLogMessage("Caught exception"), e);
        }
        finally
        {
            System.exit(retCode);
        }
    }
}
