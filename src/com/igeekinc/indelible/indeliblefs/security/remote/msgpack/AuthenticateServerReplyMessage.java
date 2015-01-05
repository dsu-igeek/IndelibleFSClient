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
 
package com.igeekinc.indelible.indeliblefs.security.remote.msgpack;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;

@Message
public class AuthenticateServerReplyMessage
{
	public X509CertificateMsgPack authenticatedCert;
	
	public AuthenticateServerReplyMessage()
	{
		
	}
	
	public AuthenticateServerReplyMessage(EntityAuthentication authentication) throws CertificateEncodingException
	{
		X509Certificate certificate = null;
		if (authentication != null)
			certificate = authentication.getCertificate();
		authenticatedCert = new X509CertificateMsgPack(certificate);
	}
	
	public EntityAuthentication getEntityAuthentication() throws CertificateException
	{
		if (authenticatedCert != null)
		{
			X509Certificate x509Certificate = authenticatedCert.getX509Certificate();
			if (x509Certificate != null)
				return new EntityAuthentication(x509Certificate);
		}
		return null;
	}
}
