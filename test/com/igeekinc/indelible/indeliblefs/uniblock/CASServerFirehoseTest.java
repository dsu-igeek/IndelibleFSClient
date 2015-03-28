/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igeekinc.indelible.indeliblefs.uniblock;

import java.io.File;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.igeekinc.indelible.indeliblefs.IndelibleFSClientPreferences;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSClient;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;
import com.igeekinc.junitext.iGeekTestCase;
import com.igeekinc.util.MonitoredProperties;

public class CASServerFirehoseTest extends iGeekTestCase
{
	Logger logger;
	CASServerFirehoseClient client = null;
	CASServer testServer = null;
	
	public void setUp() throws Exception
	{
		if (testServer == null)
		{
			IndelibleFSClientPreferences.initPreferences(null);

			MonitoredProperties serverProperties = IndelibleFSClientPreferences.getProperties();
			PropertyConfigurator.configure(serverProperties);
			logger = Logger.getLogger(getClass());
			File preferencesDir = new File(serverProperties.getProperty(IndelibleFSClientPreferences.kPreferencesDirPropertyName));
			File securityClientKeystoreFile = new File(preferencesDir, IndelibleFSClient.kIndelibleEntityAuthenticationClientConfigFileName);
			EntityAuthenticationClient.initializeEntityAuthenticationClient(securityClientKeystoreFile, null, serverProperties);
			EntityAuthenticationClient.startSearchForServers();
			EntityAuthenticationServer [] securityServers = new EntityAuthenticationServer[0];
			while(securityServers.length == 0)
			{
				securityServers = EntityAuthenticationClient.listEntityAuthenticationServers();
				if (securityServers.length == 0)
					Thread.sleep(1000);
			}

			EntityAuthenticationServer securityServer = securityServers[0];

			EntityAuthenticationClient.getEntityAuthenticationClient().trustServer(securityServer);
			client = new CASServerFirehoseClient(new InetSocketAddress("localhost", 50904));
			testServer = client.getCASServer();
		}
	}
	
	public void testBasic()
	{
		// This should make setup run
		testServer.getSecurityServerID();
	}
}
