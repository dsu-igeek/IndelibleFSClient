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
		authenticatedCert = new X509CertificateMsgPack(authentication.getCertificate());
	}
	
	public EntityAuthentication getEntityAuthentication() throws CertificateException
	{
		return new EntityAuthentication(authenticatedCert.getX509Certificate());
	}
}
