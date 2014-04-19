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
 
package com.igeekinc.indelible.indeliblefs.security;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import com.igeekinc.util.logging.ErrorLogMessage;
/**
 * IndelibleTrustManager checks the certificate of an Indelible Entity against the trusted EntityAuthentication servers
 * @author David L. Smith-Uchida
 *
 */
public class IndelibleTrustManager implements X509TrustManager
{
    X509TrustManager core;
    Logger logger;
    public IndelibleTrustManager(X509TrustManager core)
    {
        this.core = core;
        logger = Logger.getLogger(getClass());
    }
    
    public void checkClientTrusted(
            X509Certificate[] paramArrayOfX509Certificate, String paramString)
            throws CertificateException
    {
    	try
    	{
    		core.checkClientTrusted(paramArrayOfX509Certificate, paramString);
    	} catch (CertificateException e)
    	{
    		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    		logger.error("checkClientTrusted cert list:");
    		for (X509Certificate curCert:paramArrayOfX509Certificate)
    		{
    			logger.error(curCert.toString());
    		}
    		logger.error("--end certificates");
    		throw e;
    	}
    }

    public void checkServerTrusted(
            X509Certificate[] paramArrayOfX509Certificate, String paramString)
            throws CertificateException
            {
    	try
    	{
    		core.checkServerTrusted(paramArrayOfX509Certificate, paramString);
    	} catch (CertificateException e)
    	{
    		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    		logger.error("checkServerTrusted cert list:");
    		for (X509Certificate curCert:paramArrayOfX509Certificate)
    		{
    			logger.error(curCert.toString());
    		}
    		logger.error("--end certificates");
    	}
            }

    public X509Certificate[] getAcceptedIssuers()
    {
        X509Certificate [] returnCerts = core.getAcceptedIssuers();
        return returnCerts;
    }
}
