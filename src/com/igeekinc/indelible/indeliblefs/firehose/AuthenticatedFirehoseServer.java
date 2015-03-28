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
package com.igeekinc.indelible.indeliblefs.firehose;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.SSLPeerUnverifiedException;

import com.igeekinc.firehose.FirehoseChannel;
import com.igeekinc.firehose.FirehoseServer;
import com.igeekinc.firehose.SSLFirehoseChannel;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;

public abstract class AuthenticatedFirehoseServer<C> extends FirehoseServer<C>
{

	public AuthenticatedFirehoseServer()
	{
		super();
	}

	@Override
	public void addChannel(FirehoseChannel newServerChannel) throws IOException
	{
		if (!(newServerChannel instanceof SSLFirehoseChannel))
			throw new IllegalArgumentException("Authenticated clients must connect via SSL");
		super.addChannel(newServerChannel);
	}
	
	public EntityAuthentication[] getAuthenticatedClients(SSLFirehoseChannel channel)
			throws SecurityException, SSLPeerUnverifiedException
	{
    	ArrayList<EntityAuthentication>authenticatedClients = new ArrayList<EntityAuthentication>();
        Certificate[] clientCertificates;
        clientCertificates = channel.getPeerCertificates();
        for (Certificate curCertificate:clientCertificates)
        {
            if (curCertificate instanceof X509Certificate)
            {
                EntityAuthentication curAuthentication = new EntityAuthentication((X509Certificate)curCertificate);
                if (EntityAuthenticationClient.getEntityAuthenticationClient().checkAuthentication(curAuthentication))
                    authenticatedClients.add(curAuthentication);
            }
        }
        EntityAuthentication [] returnAuthentications = new EntityAuthentication[authenticatedClients.size()];
        returnAuthentications = authenticatedClients.toArray(returnAuthentications);
        return returnAuthentications;
	}
}
