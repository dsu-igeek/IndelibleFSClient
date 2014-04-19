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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.util.Properties;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.igeekinc.indelible.PreferencesManager;
import com.igeekinc.indelible.indeliblefs.IndelibleFSClient;
import com.igeekinc.indelible.indeliblefs.IndelibleFSClientPreferences;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverReceiver;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSource;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.exceptions.VolumeNotFoundException;
import com.igeekinc.indelible.indeliblefs.proxies.IndelibleFSServerProxy;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServer;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServerFirehoseClient;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.GeneratorID;
import com.igeekinc.indelible.oid.GeneratorIDFactory;
import com.igeekinc.indelible.oid.IndelibleFSClientOIDs;
import com.igeekinc.indelible.oid.IndelibleFSObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.MonitoredProperties;

public abstract class IndelibleFSUtilBase
{
    protected IndelibleFSServerProxy fsServer;
    protected IndelibleServerConnectionIF connection;
    protected EntityAuthenticationServer securityServer;
    protected DataMoverSession moverSession;
    protected Logger logger;
    
    public IndelibleFSUtilBase()
    {
    	IndelibleFSClientOIDs.initMappings();	// IndelibleFSClient will set these up but we do things before IndelibleFSClient is initialized
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.FATAL);
        logger = Logger.getLogger(getClass());
    }

	public void setup() throws IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, UnrecoverableKeyException,
			InvalidKeyException, NoSuchProviderException, SignatureException,
			AuthenticationFailureException, InterruptedException,
			RemoteException, SSLPeerUnverifiedException,
			CertificateParsingException, CertificateEncodingException
	{
		MonitoredProperties clientProperties = setupProperties();
		try
		{
			setupLogging(clientProperties);
		} catch (Throwable t)
		{
			t.printStackTrace();
			System.exit(1);
		}
        File preferencesDir = PreferencesManager.getPreferencesDir();
        File securityClientKeystoreFile = new File(preferencesDir, IndelibleFSClient.kIndelibleEntityAuthenticationClientConfigFileName);
        EntityAuthenticationClient.initializeEntityAuthenticationClient(securityClientKeystoreFile, null, clientProperties);
        EntityAuthenticationClient.startSearchForServers();
        EntityAuthenticationServer [] securityServers = new EntityAuthenticationServer[0];
        while(securityServers.length == 0)
        {
            securityServers = EntityAuthenticationClient.listEntityAuthenticationServers();
            if (securityServers.length == 0)
                Thread.sleep(1000);
        }
        
        securityServer = securityServers[0];
        
        EntityAuthenticationClient.getEntityAuthenticationClient().trustServer(securityServer);
        IndelibleFSClient.start(null, clientProperties);
        IndelibleFSServerProxy[] servers = new IndelibleFSServerProxy[0];
        
        while(servers.length == 0)
        {
            servers = IndelibleFSClient.listServers();
            if (servers.length == 0)
                Thread.sleep(1000);
        }
        fsServer = servers[0];

        GeneratorIDFactory genIDFactory = new GeneratorIDFactory();
        GeneratorID testBaseID = genIDFactory.createGeneratorID();
        ObjectIDFactory oidFactory = new ObjectIDFactory(testBaseID);
        DataMoverSource.init(oidFactory);
        DataMoverReceiver.init(oidFactory);

        connection = fsServer.open();
        
        EntityAuthentication serverAuthentication = connection.getClientEntityAuthentication();
        moverSession = DataMoverSource.getDataMoverSource().createDataMoverSession(securityServer.getEntityID());
        moverSession.addAuthorizedClient(serverAuthentication);
	}

	/**
	 * Override this to setup a log file, etc.
	 */
	public void setupLogging(MonitoredProperties loggingProperties)
	{
		
	}

	public MonitoredProperties setupProperties() throws IOException
	{
		IndelibleFSClientPreferences.initPreferences(null);

        MonitoredProperties clientProperties = IndelibleFSClientPreferences.getProperties();
		return clientProperties;
	}
    
    /**
     * Create a new file system with the default 
     * @param volumeProperties
     * @return
     * @throws RemoteException
     * @throws IOException
     * @throws PermissionDeniedException
     */
    public IndelibleFSVolumeIF createNewFileSystem(Properties volumeProperties) throws RemoteException, IOException, PermissionDeniedException
    {
        connection.startTransaction();
        IndelibleFSVolumeIF newVolume = connection.createVolume(volumeProperties);
        connection.commit();
        return newVolume;
    }
    
    public IndelibleFSVolumeIF retrieveFileSystem(IndelibleFSObjectID retrieveVolumeID) throws VolumeNotFoundException, IOException
    {
    	IndelibleFSVolumeIF retrievedVolume = connection.retrieveVolume(retrieveVolumeID);
        return retrievedVolume;
    }
    
	public EntityID getEntityID()
	{
		return EntityAuthenticationClient.getEntityAuthenticationClient().getEntityID();
	}
	
	public void increaseVerbosity()
	{
		Level curLevel = Logger.getRootLogger().getLevel();
		int newLevelInt = curLevel.toInt() - 10000;
		if (newLevelInt >= 0)
		{
			newLevelInt = ((int)(newLevelInt/10000)) * 10000;
			Level newLevel = Level.toLevel(newLevelInt);
			Logger.getRootLogger().setLevel(newLevel);
		}
	}
	
	public final void run(String [] args)
	{
		try
		{
			processArgs(args);
			setup();
			runApp();
		}
		catch (Throwable t)
		{
			logger.fatal("Uncaught exception", t);
		}
	}
	
	public void processArgs(String [] args) throws Exception
	{
		 LongOpt [] longOptions = {
	                new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')
	        };
	        Getopt getOpt = new Getopt("IndelibleList", args, "v", longOptions);
	        
	        int opt;
	        while ((opt = getOpt.getopt()) != -1)
	        {
	            switch(opt)
	            {
	            case 'v':
	            	increaseVerbosity();
	            	break;
	            }
	        }
	}
	public abstract void runApp() throws Exception;
}
