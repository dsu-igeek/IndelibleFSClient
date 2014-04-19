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

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import com.igeekinc.indelible.oid.EntityID;

/**
 * EntityAuthenticationServer interface.  The entity authentication server is responsible for authenticating other Indelible FS servers.
 * It provides 
 * 
 * @author David L. Smith-Uchida
 */
public interface EntityAuthenticationServer extends Remote
{
    public static final String kIndelibleEntityAuthenticationServerRMIName = "IndelibleEntityAuthenticationServer";
    public static final String kSignatureType = "SHA1withDSA";
    public static final long kDefaultAuthorizationTime = 60 * 60 * 1000;
    public static final String kChallengeSignatureAlg = "SHA1withRSA";
    public static final String kCertificateSignatureAlg = "SHA1withRSA";
    public static final int kDefaultEntityAuthenticationServerStaticPort = 43192;
    public static final String kEntityAuthenticationServerRandomPortPropertyName="indelible.security.entityauthenticationserver.useRandomPort";
    public static final String kEntityAuthenticationServerPortNumberPropertyName="indelible.security.entityauthenticationserver.portNumber";
    /**
     * Registers a server with the Security Server.  In an auto configuring environment, it will be immediately registered.  
     * In an administered environment, it will need to be approved by an administrator.
     * 
     * After registering, the server's public key/certificate will be kept by the security server.
     * 
     * @param selfSignedServerCert
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws SignatureException
     * @throws KeyStoreException
     * @throws RemoteException
     */
    public abstract void registerServer(X509Certificate selfSignedServerCert)
            throws InvalidKeyException, CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException,
            SignatureException, KeyStoreException, RemoteException;

    /**
     * Returns a certificate signed by the SecurityServer, based on the encodedCertReq (PKCS10CertificationRequest format).
     * The returned certificate is signed by the Security Server and is valid for a limited time (default 24 hours).  
     * When the certificate expires, a new one must be obtained with authenticateServer
     * @param serverID
     * @param encodedCertReq - a PKCS10 encoded certification request
     * @return EntityAuthentication containing the signed certificate
     * @throws CertificateEncodingException
     * @throws InvalidKeyException
     * @throws IllegalStateException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws RemoteException
     * @throws IOException
     * @throws CertificateParsingException
     */
    public abstract EntityAuthentication authenticateServer(EntityID serverID,
            byte[] encodedCertReq) throws CertificateEncodingException,
            InvalidKeyException, IllegalStateException,
            NoSuchProviderException, NoSuchAlgorithmException,
            SignatureException, UnrecoverableKeyException, KeyStoreException, RemoteException, IOException, CertificateParsingException;

    /**
     * Returns the certificate for this server.  The certificate may be a root certificate (if this is the root server) or it may be a 
     * certificate that has a chain attached back to the root server
     * @return
     * @throws KeyStoreException
     * @throws RemoteException
     */
    public Certificate getServerCertificate() throws KeyStoreException, RemoteException;
    
    /**
     * Challenges the security server to sign a byte string.  This is used to validate that the server really is the owner of the certificate it is returning.
     * @param bytesToSign
     * @return
     * @throws RemoteException
     */
    public byte [] entityAuthenticationServerChallenge(byte [] bytesToSign) throws RemoteException;

    /**
     * Returns the EntityID for the server
     * @return
     * @throws RemoteException
     */
    public abstract EntityID getEntityID() throws RemoteException;
}