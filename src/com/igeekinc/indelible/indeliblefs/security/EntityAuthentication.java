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

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.StringTokenizer;

import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;

/**
 * EntityAuthentication is a wrapper for an X509 certificate
 * @author David L. Smith-Uchida
 */
public class EntityAuthentication implements Serializable
{
    private static final long serialVersionUID = 5844360651454117159L;
    private X509Certificate certificate;
    
    public EntityAuthentication(X509Certificate certificate)
    {
        this.certificate = certificate;
    }

    /**
     * Retrieves the entity ID authenticated by this object
     * @return
     */
    public EntityID getEntityID()
    {
    	return getObjectIDFromCertificateSerialNumber(certificate);
    }
    
    public static EntityID getObjectIDFromCertificateSerialNumber(
            X509Certificate selfSignedServerCert)
    {
        String serverIDString = selfSignedServerCert.getSerialNumber().toString(16);
        while (serverIDString.length() < ObjectID.kObjectStrLength)
            serverIDString = "0"+serverIDString;
        return (EntityID)ObjectIDFactory.reconstituteFromString(serverIDString);
    }
    /**
     * Returns the server ID of the security server that authenticated the object
     * @return
     */
    public ObjectID getAuthorizingSecurityServerID()
    {
        String issuerString = certificate.getIssuerDN().getName();
        StringTokenizer tokenizer = new StringTokenizer(issuerString, ",");
        ObjectID returnID = null;
        while (tokenizer.hasMoreElements())
        {
            String curToken = tokenizer.nextToken();
            if (curToken.startsWith("UID="))
            {
                String oidString = curToken.substring(4);
                returnID = ObjectIDFactory.reconstituteFromString(oidString);
            }
        }
        if (returnID == null)
            throw new InternalError("Unable to retrieve OID from distinguished name "+issuerString);
        return returnID;
    }

    /**
     * Returns the time the entity was authenticated
     * @return
     */
    public Date getAuthorizationTime()
    {
        return certificate.getNotBefore();
    }

    /**
     * Returns the time when the certificate will expire
     * @return
     */
    public Date getAuthorizationExpirationTime()
    {
        return certificate.getNotAfter();
    }
    
    public X509Certificate getCertificate()
    {
        return certificate;
    }
    
    public String toString()
    {
    	return "Authorized ID = "+getEntityID()+" by "+getAuthorizingSecurityServerID()+" expires at "+getAuthorizationExpirationTime();
    }
}
