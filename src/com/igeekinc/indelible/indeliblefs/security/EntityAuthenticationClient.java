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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import sun.security.util.ObjectIdentifier;
import sun.security.x509.AVA;
import sun.security.x509.RDN;
import sun.security.x509.X500Name;

import com.igeekinc.indelible.indeliblefs.IndelibleEntity;
import com.igeekinc.indelible.indeliblefs.security.afunix.AFUnixAuthenticatedSocket;
import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.GeneratorIDFactory;
import com.igeekinc.indelible.oid.IndelibleFSClientOIDs;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.MonitoredProperties;
import com.igeekinc.util.OSType;
import com.igeekinc.util.SystemInfo;
import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.logging.WarnLogMessage;

/**
 * EntityAuthenticationClient manages the connection to EntityAuthenticationServer(s).  It can handle connections to a local server (in the same
 * JVM) or remote servers.  The EntityAuthenticationClient maintains a list of available entity authentication servers and a list of trusted entity authentication servers.
 * Servers can be discovered via Bonjour or be specified as part of the configuration.
 * @author David L. Smith-Uchida
 */
public abstract class EntityAuthenticationClient extends IndelibleEntity
{
    public static final String	kEntityIDCNPrefix	= "CN=Authentication for ";
	public static final String kAutoInitPropertyName = "com.igeekinc.entityauthenticationclient.autoinit";
	public static final String kInitServerPropertyName = "com.igeekinc.entityauthenticationclient.initserver";
	private static final String kAuthenticatedCertAlias = "authenticatedCert";
    // The list of servers that are allowed to authenticate to us
    private HashMap<String, X509Certificate> trustedServerCertificates = new HashMap<String, X509Certificate>();
    private static EntityAuthenticationClient singleton;
    private boolean initialized;
    
    // This is the keystore that is read/written to disk.  It contains all of the certificates of servers
    // that we have been configured to trust and our own certificates(s) 
    private KeyStore persistentKeyStore;
    
    // This is the list of all entity authentication servers we've seen or been configured for
    private ArrayList<EntityAuthenticationServer> entityAuthenticationServers = new ArrayList<EntityAuthenticationServer>();
    
    // This is the list of entity authentication servers that we trust
    private ArrayList<EntityAuthenticationServer> trustedServers = new ArrayList<EntityAuthenticationServer>();
    private EventListenerList eventListeners = new EventListenerList();
    private EntityID clientIdentity;
    
    private static final String kDefaultKeyStorePassword="IN671$%ddsl";
    private static final String kPrivateKeyAliasPrefix = "sckeyprv-";
    private static final String kEntityAuthenticationServerAuthenticationCertAlias = "entityauthenticationservercert";
    private static final String kMyCertAlias="mycert";
    private PublicKey publicKey;
    private X509Certificate mySelfSignedCert;
    private File keyStoreFile;
    private MonitoredProperties entityAuthenticationClientProperties;
    private ObjectIDFactory oidFactory;
	public static final String kEntityAuthenticationServerIDKey = "securityServerID";
    private HashMap<EntityID, HashMap<EntityID, EntityAuthentication>> cachedAuthentications = new HashMap<EntityID, HashMap<EntityID, EntityAuthentication>>();
	static
	{
		IndelibleFSClientOIDs.initMappings();
	}
    /**
     * Get the Entity Authentication client for this virtual machine
     * @return The Entity Authentication  client
     */
    public static EntityAuthenticationClient getEntityAuthenticationClient()
    {
        if (singleton == null || !singleton.initialized)
            throw new InternalError("EntityAuthenticationClient not initialized!");
        return singleton;
    }
    
    /**
     * Checks to see if the entity authentication client has been initialized
     * @return true if the client has been initialized, false otherwise
     */
    public static boolean wasInitialized()
    {
        return singleton != null && singleton.initialized;
    }
    
    /**
     * Initialize the entity authentication client with no entity authentication server
     * @param keyStoreFile
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws InvalidKeyException
     * @throws IllegalStateException
     * @throws NoSuchProviderException
     * @throws SignatureException
     * @throws AuthenticationFailureException
     */
    public static void initializeEntityAuthenticationClient(File keyStoreFile, ObjectIDFactory oidFactory, MonitoredProperties entityAuthenticationClientProperties) 
    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException, InvalidKeyException, IllegalStateException, NoSuchProviderException, SignatureException, AuthenticationFailureException
    {
        allocateSingleton();
        singleton.init(keyStoreFile, kDefaultKeyStorePassword.toCharArray(), oidFactory, entityAuthenticationClientProperties);
    }
    
	@SuppressWarnings("unchecked")
	public static synchronized void allocateSingleton() throws InternalError 
	{
		if (singleton != null)
			return;
		String className = null;
		if (SystemInfo.getSystemInfo().getOSType() == OSType.kWindows) //$NON-NLS-1$
		{
			className = "com.igeekinc.indelible.indeliblefs.security.windows.EntityAuthenticationClientWindows"; //$NON-NLS-1$
		}
		
		if (SystemInfo.getSystemInfo().getOSType() == OSType.kMacOSX) //$NON-NLS-1$
		{
			className = "com.igeekinc.indelible.indeliblefs.security.macosx.EntityAuthenticationClientMacOSX"; //$NON-NLS-1$
		}
		
		if (SystemInfo.getSystemInfo().getOSType() ==  OSType.kLinux) //$NON-NLS-1$
		{
			className = "com.igeekinc.indelible.indeliblefs.security.linux.EntityAuthenticationClientLinux";	//$NON-NLS-1$
		}

		try
		{
			Class<? extends EntityAuthenticationClient> fsClientClass = (Class<? extends EntityAuthenticationClient>) Class.forName(className);

			Class<?> [] constructorArgClasses = {};
			Constructor<? extends EntityAuthenticationClient> fsClientConstructor = fsClientClass.getConstructor(constructorArgClasses);
			Object [] constructorArgs = {};
			singleton = fsClientConstructor.newInstance(constructorArgs);
		}
		catch (Throwable e)
		{
			Logger.getLogger(EntityAuthenticationClient.class).error("Caught exception creating EntityAuthenticationClient", e); //$NON-NLS-1$
			throw new InternalError("Caught exception creating IndelibleFSClient"); //$NON-NLS-1$
		}
	}
    
    
    
    protected EntityAuthenticationClient() 
    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException, InvalidKeyException, IllegalStateException, NoSuchProviderException, SignatureException, AuthenticationFailureException
    {
        super(null);
    }
    
    private void initKeyStoreFile(File keyStoreFile, char [] keyStorePassPhrase) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException, IOException
    {
    	EntityAuthenticationServer primaryEntityAuthenticationServer = null;
    	String entityAuthenticationInitServerName = entityAuthenticationClientProperties.getProperty(kInitServerPropertyName);
		if (entityAuthenticationInitServerName != null)
    	{
    		primaryEntityAuthenticationServer = EntityAuthenticationClient.connectToServer(entityAuthenticationInitServerName);
    		if (primaryEntityAuthenticationServer == null)
    		{
    			Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Could not connect to specified Entity Authentication Server {0}",
    					new Serializable[]{entityAuthenticationInitServerName}));
    		}
    	}
    	else
    	{
    		if (entityAuthenticationClientProperties.getProperty(kAutoInitPropertyName, "N").toUpperCase().equals("Y"))
    		{
    			EntityAuthenticationServer[] entityAuthenticationServers = EntityAuthenticationClient.listEntityAuthenticationServers();
    			while (entityAuthenticationServers == null || entityAuthenticationServers.length == 0)
    			{
    				try
					{
						Thread.sleep(500);
					} catch (InterruptedException e)
					{
						Logger.getLogger(EntityAuthenticationClient.class).error("Caught interrupted exception in initKeyStoreFile", e); //$NON-NLS-1$
					}
    				entityAuthenticationServers = EntityAuthenticationClient.listEntityAuthenticationServers();
    			}
    			primaryEntityAuthenticationServer = entityAuthenticationServers[0];
    		}
    		else
    		{
    			throw new IllegalArgumentException("com.igeekinc.entityauthenticationclient.initserver property is empty and com.igeekinc.entityauthenticationclient.autoinit is not enable");
    		}
    	}
		Certificate serverCertificate = primaryEntityAuthenticationServer.getServerCertificate();
		EntityAuthenticationClient.initIdentity(keyStoreFile, (EntityID)oidFactory.getNewOID(IndelibleEntity.class), serverCertificate);
    }
    
    public static EntityAuthenticationServer connectToServer(String serverInfo)
	{
    	String hostName="";
    	int port;
    	if (serverInfo.indexOf(':') > 0)
    	{
    		hostName = serverInfo.substring(0, serverInfo.indexOf(':'));
    		String portString = serverInfo.substring(serverInfo.indexOf(':') + 1);
    		port = Integer.parseInt(portString);
    	}
    	else
    	{
    		hostName = serverInfo;
    		port = EntityAuthenticationServer.kDefaultEntityAuthenticationServerStaticPort;
    	}
    	EntityAuthenticationServer connectedServer = serverFound(hostName, port);
		return connectedServer;
	}

    class ConnectToInitServerRunnable implements Runnable
    {
    	private MonitoredProperties initProperties;
    	public ConnectToInitServerRunnable(MonitoredProperties initProperties)
    	{
    		this.initProperties = initProperties;
    	}
    	
    	@Override
    	public void run()
    	{
    		connectToInitServer(initProperties);
    	}
    }
    
	private void init(File keyStoreFile, char [] keyStorePassPhrase, ObjectIDFactory oidFactory, MonitoredProperties entityAuthenticationClientProperties) 
    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException, InvalidKeyException, IllegalStateException, NoSuchProviderException, SignatureException, AuthenticationFailureException
    {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); // Bouncy, bouncy!
        
        this.keyStoreFile = keyStoreFile;
        this.entityAuthenticationClientProperties = entityAuthenticationClientProperties;
        if (oidFactory == null)
        {
        	oidFactory = new ObjectIDFactory(new GeneratorIDFactory().createGeneratorID());
        }
        this.oidFactory = oidFactory;
        if (!keyStoreFile.exists())
        {
        	initKeyStoreFile(keyStoreFile, keyStorePassPhrase);
        }

        persistentKeyStore = KeyStore.getInstance("JKS");
        try
		{
			persistentKeyStore.load(new FileInputStream(keyStoreFile), keyStorePassPhrase);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).fatal(new ErrorLogMessage("Could not read keystore file {0}, EntityAuthenticationClient initialization failed", 
					new Serializable[]{keyStoreFile}), e);
			throw e;
		}
        
        Enumeration<String> aliases = persistentKeyStore.aliases();
        while (aliases.hasMoreElements())
        {
            String checkAlias = aliases.nextElement();
            if (checkAlias.startsWith(kEntityAuthenticationServerAuthenticationCertAlias))
                trustedServerCertificates.put(checkAlias, (X509Certificate) persistentKeyStore.getCertificate(checkAlias));
        }
        if (this.trustedServerCertificates.size() == 0)
            throw new IOException("Could not retrieve authentication server certificate from keystore file "+keyStoreFile);
        id = null;
        Enumeration<String>ksAliases = persistentKeyStore.aliases();
        while(ksAliases.hasMoreElements())
        {
            String curAlias = ksAliases.nextElement();
            if (curAlias.startsWith(kPrivateKeyAliasPrefix))
            {
                String serverIDString = curAlias.substring(kPrivateKeyAliasPrefix.length());
                id = (EntityID) ObjectIDFactory.reconstituteFromString(serverIDString);
                
                break;
            }
        }
        if (id == null)
            throw new IOException("Could not find public/private keys in keystore file "+keyStoreFile);
                  
        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal              dnName = new X500Principal("CN=Indelible FS Client self-signed cert");

        certGen.setSerialNumber(id.toBigInteger());
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 10 * 60 * 1000));	// Allow for some clock skew
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 3600 * 1000));
        certGen.setSubjectDN(dnName);                       // note: same as issuer
        Certificate [] ssCerts = persistentKeyStore.getCertificateChain(kPrivateKeyAliasPrefix+id.toString());  // Should just be our own self-signed cert
        publicKey = ssCerts[0].getPublicKey();
        certGen.setPublicKey(publicKey);
        certGen.setSignatureAlgorithm(EntityAuthenticationServer.kCertificateSignatureAlg);

        mySelfSignedCert = certGen.generate((PrivateKey)persistentKeyStore.getKey(kPrivateKeyAliasPrefix+id.toString(), kDefaultKeyStorePassword.toCharArray()), "BC");
        
        clientIdentity = id;
        
        writeKeystore(keyStoreFile, persistentKeyStore);
        initialized = true;
        //connectToInitServer(entityAuthenticationClientProperties);
        Thread connectThread = new Thread(new ConnectToInitServerRunnable(entityAuthenticationClientProperties),
        		"Connect to EntityAuthenticationClient init server");
        connectThread.setDaemon(true);
        connectThread.start();
    }

	private void connectToInitServer(
			MonitoredProperties entityAuthenticationClientProperties)
	{
		try
		{
			if (entityAuthenticationClientProperties.getProperty(kInitServerPropertyName) != null)
			{
				EntityAuthenticationServer initServer = connectToServer(entityAuthenticationClientProperties.getProperty(kInitServerPropertyName));
				if (initServer != null)
					trustServer(initServer);
			}
		}
		catch (Throwable t)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught unexpected error connecting to EntityAuthenticationServer", t));
		}
	}
    
    public byte [] signChallenge(byte [] bytesToSign)
    {
        Signature signingSignature;
        try
        {
            signingSignature = Signature.getInstance(EntityAuthenticationServer.kChallengeSignatureAlg, "BC");
            signingSignature.initSign((PrivateKey) persistentKeyStore.getKey(kPrivateKeyAliasPrefix+id.toString(), kDefaultKeyStorePassword.toCharArray()));
            signingSignature.update(bytesToSign);
            byte [] signedBytes = signingSignature.sign();
            return signedBytes;
        } catch (GeneralSecurityException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        }
        throw new InternalError("Could not generate signing signature");
    }
    public X509Certificate[] getAuthenticationServerCertificates()
    {
        X509Certificate[] returnList = new X509Certificate[trustedServerCertificates.size()];
        returnList = trustedServerCertificates.values().toArray(returnList);
        return returnList;
    }
    
    /**
     * Validates the certificate inside an EntityAuthentication
     * @param authentication
     * @return
     */
    public boolean checkAuthentication(EntityAuthentication authentication)
    {
        boolean authenticated = false, authenticationCertificateValid = false;;
        try
        {
            authentication.getCertificate().checkValidity();
            authenticationCertificateValid = true;
        } catch (CertificateExpiredException e1)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
        } catch (CertificateNotYetValidException e1)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
        }
        if (authenticationCertificateValid)
        {
            for (X509Certificate checkCertificate:trustedServerCertificates.values())
            {
                try
                {
                    authentication.getCertificate().verify(checkCertificate.getPublicKey(), "BC");
                    authenticated = true;
                    break;
                }
                catch (SignatureException e)
                {
                    Logger.getLogger(getClass()).debug(new ErrorLogMessage("Caught exception"), e);
                }
                catch (CertificateExpiredException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                } catch (CertificateNotYetValidException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                } catch (InvalidKeyException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                } catch (CertificateException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                } catch (NoSuchAlgorithmException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                } catch (NoSuchProviderException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                } 
            }
        }
        return authenticated;
    }

    /**
     * Returns a set of key managers to work with the specified entityAuthenticationServer
     * @param entityAuthenticationServerID - the server to authenticate with
     * @return
     */
    public KeyManager [] getKeyManagers(EntityID entityAuthenticationServerID)
    {
        try
        {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            KeyStore authenticatedKeyStore = KeyStore.getInstance("JKS");
            authenticatedKeyStore.load(null, kDefaultKeyStorePassword.toCharArray());

            PrivateKey privateKey = (PrivateKey) persistentKeyStore.getKey(kPrivateKeyAliasPrefix+id.toString(), kDefaultKeyStorePassword.toCharArray());
            EntityAuthentication myAuthentication = authenticateEntity(id, entityAuthenticationServerID, new KeyPair(publicKey, privateKey));
            if (myAuthentication == null)
                throw new InternalError("Could not get authentication for client");
            authenticatedKeyStore.setCertificateEntry(kAuthenticatedCertAlias+entityAuthenticationServerID.toString(), myAuthentication.getCertificate());
            boolean trustedServerFound = false;
            for (String curKey:trustedServerCertificates.keySet())
            {
                X509Certificate curAuthenticationServer = trustedServerCertificates.get(curKey);
                authenticatedKeyStore.setCertificateEntry(curKey, curAuthenticationServer);

                String keyAlias = kPrivateKeyAliasPrefix+id.toString();

                Principal checkAuthenticationIssuerDN = myAuthentication.getCertificate().getIssuerDN();
                Principal curAuthenticationServerIssuerDN = curAuthenticationServer.getIssuerDN();
                if (principalsMatch(checkAuthenticationIssuerDN, curAuthenticationServerIssuerDN))
                {
                    authenticatedKeyStore.setKeyEntry(keyAlias, persistentKeyStore.getKey(keyAlias, kDefaultKeyStorePassword.toCharArray()), 
                            kDefaultKeyStorePassword.toCharArray(), new Certificate []{myAuthentication.getCertificate(), curAuthenticationServer});
                    trustedServerFound = true;
                    break;
                }

            }
            if (trustedServerFound)
            {
                keyManagerFactory.init(authenticatedKeyStore, kDefaultKeyStorePassword.toCharArray());
                return keyManagerFactory.getKeyManagers();
            }
        } catch (Throwable t)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), t);
        }
        throw new InternalError("Could not initialize/authenticate for key manager");
    }
    
    public boolean principalsMatch(Principal p1, Principal p2)
    {
        String p1UID = getUID(p1);
        String p2UID = getUID(p2);

        return p1UID.equals(p2UID);
    }
    
    public static String getUID(Principal p)
    {
        if (p instanceof X500Name)
        {
            List<RDN>rdns = ((X500Name)p).rdns();
            for (RDN curRDN:rdns)
            {
                List <AVA>avas = curRDN.avas();
                for (AVA curAva:avas)
                {
                    ObjectIdentifier curOID = curAva.getObjectIdentifier();
                    String valueString = curAva.getValueString();
                    try
                    {
                        if (curOID.equals((Object)new ObjectIdentifier("0.9.2342.19200300.100.1.1")))
                        {
                            return (valueString);
                        }
                    } catch (IOException e)
                    {
                        Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
                    }
                }
            }
        }
        if (p instanceof X509Principal)
        {
            @SuppressWarnings("rawtypes")
			Vector oids = ((X509Principal)p).getOIDs();
            @SuppressWarnings("rawtypes")
			Vector values = ((X509Principal)p).getValues();
            for (int curPairNum = 0; curPairNum < oids.size(); curPairNum++)
            {
                DERObjectIdentifier curOID = (DERObjectIdentifier) oids.get(curPairNum);
                if (curOID.equals((Object)new DERObjectIdentifier("0.9.2342.19200300.100.1.1")))
                    return (String) values.get(curPairNum);

            }
            Logger.getLogger(EntityAuthenticationClient.class).warn("oids = "+oids);
        }
        return "";
    }
    public TrustManager [] getTrustManagers(EntityID entityAuthenticationServerID)
    {
        
        TrustManagerFactory trustFactory = null;
        try
        {
            trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustFactory.init(persistentKeyStore);
        } catch (NoSuchAlgorithmException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            throw new InternalError();
        } catch (KeyStoreException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
            throw new InternalError();
        }

        TrustManager [] cores = trustFactory.getTrustManagers();
        TrustManager [] returnManagers = new TrustManager[cores.length];
        for (int curCoreNum = 0; curCoreNum < cores.length; curCoreNum++)
        {
            returnManagers[curCoreNum] = new IndelibleTrustManager((X509TrustManager)cores[curCoreNum]);
        }
        return returnManagers;
    }
    
    /**
     * Contacts the Entity Authentication Server to create an authentication for the specified Entity ID
     * @param entityID
     * @param entityAuthenticationServerID
     * @param entityKeys
     * @return
     * @throws CertificateEncodingException
     * @throws InvalidKeyException
     * @throws IllegalStateException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateParsingException
     */
    public EntityAuthentication authenticateEntity(EntityID entityID, EntityID entityAuthenticationServerID, KeyPair entityKeys) 
    throws CertificateEncodingException, InvalidKeyException, IllegalStateException, NoSuchProviderException, 
    NoSuchAlgorithmException, SignatureException, UnrecoverableKeyException, KeyStoreException, IOException, CertificateParsingException
    {
    	EntityAuthentication returnAuthentication = null;
    	synchronized(cachedAuthentications)
    	{
    		HashMap<EntityID, EntityAuthentication>authentications = cachedAuthentications.get(entityID);
    		if (authentications != null)
    		{
    			EntityAuthentication checkAuthentication = authentications.get(entityAuthenticationServerID);
    			if (checkAuthentication != null)
    			{
    				if (checkAuthentication.getAuthorizationExpirationTime().before(new Date()))
    				{
    					returnAuthentication = checkAuthentication;
    				}
    				else
    				{
    					// Authentication is expired, remove from the table
    					authentications.remove(entityAuthenticationServerID);
    				}
    			}
    		}
    	}
    	if (returnAuthentication == null)
    	{
    		X500Principal entityName = new X500Principal(kEntityIDCNPrefix+entityID.toString());

    		PKCS10CertificationRequest certReq = new PKCS10CertificationRequest(EntityAuthenticationServer.kCertificateSignatureAlg,
    				entityName,
    				entityKeys.getPublic(),
    				null,
    				entityKeys.getPrivate());
    		byte [] encodedCertReq = certReq.getEncoded();
    		EntityAuthenticationServer [] authenticateServers = new EntityAuthenticationServer[entityAuthenticationServers.size()];
    		authenticateServers = entityAuthenticationServers.toArray(authenticateServers);

    		for (int curServerNum = 0; curServerNum < authenticateServers.length; curServerNum++)
    		{
    			if (authenticateServers[curServerNum].getEntityID().equals(entityAuthenticationServerID))
    			{
    				returnAuthentication = authenticateServers[curServerNum].authenticateServer(entityID, encodedCertReq);
    				break;
    			};
    		}
    		if (returnAuthentication != null)
    		{
    			synchronized(cachedAuthentications)
    			{
    				HashMap<EntityID, EntityAuthentication>authentications = cachedAuthentications.get(entityID);
    				if (authentications == null)
    				{
    					authentications = new HashMap<EntityID, EntityAuthentication>();
    					cachedAuthentications.put(entityID, authentications);
    				}
    				EntityAuthentication checkAuthentication = authentications.get(entityAuthenticationServerID);
    				if (checkAuthentication != null && checkAuthentication.getAuthorizationExpirationTime().before(new Date()))
    				{
    					// Hmmm - someone beat us to it.  Use that authentication
    					returnAuthentication = checkAuthentication;
    				}
    				else
    				{
    					authentications.put(entityAuthenticationServerID, returnAuthentication);
    				}
    			}
    		}
    	}
    	return returnAuthentication;
    }
    
    /*
    public Certificate getServerCertificate() throws KeyStoreException, RemoteException
    {
        return securityServer.getServerCertificate();
    }
    */
    
    public static void initIdentity(File keyStoreFile, EntityID entityAuthenticationClientID, Certificate entityAuthenticationServerCertificate)
    throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, NoSuchProviderException
    {
        initIdentity(keyStoreFile, entityAuthenticationClientID, entityAuthenticationServerCertificate, kDefaultKeyStorePassword.toCharArray());
    }
    
    public static void initIdentity(File keyStoreFile, EntityID entityAuthenticationClientID, Certificate entityAuthenticationServerCertificate, char [] keyStorePassPhrase) 
    throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, NoSuchProviderException
    {
        if (keyStoreFile.exists())
            throw new IOException("Keystore file '"+keyStoreFile.getAbsolutePath()+"' already exists - refusing to overwrite");
        KeyStore initKeyStore = KeyStore.getInstance("JKS");
        initKeyStore.load(null);
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        kpGen.initialize(1024, new SecureRandom());
        KeyPair keyPair = kpGen.generateKeyPair();
        
        String privateKeyAlias = kPrivateKeyAliasPrefix+entityAuthenticationClientID.toString();
        
        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal              dnName = new X500Principal("CN=Indelible FS Client self-signed cert");
        Date startDate = new Date(System.currentTimeMillis() - 60 * 1000);              // time from which certificate is valid
        Date expiryDate = new Date(startDate.getTime() + (10L * 365L * 24L * 60L * 60L * 1000L));             // time after which certificate is not valid
        
        certGen.setSerialNumber(entityAuthenticationClientID.toBigInteger());
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(startDate);
        certGen.setNotAfter(expiryDate);
        certGen.setSubjectDN(dnName);                       // note: same as issuer
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm(EntityAuthenticationServer.kCertificateSignatureAlg);

        X509Certificate mySelfSignedCert;
        
        try
        {
            mySelfSignedCert = certGen.generate(keyPair.getPrivate(), "BC");
        } catch (InvalidKeyException e)
        {
            Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
            throw new InternalError("Invalid key while initializing");
        } catch (IllegalStateException e)
        {
            Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
            throw new InternalError("Illegal state while initializing");
        } catch (SignatureException e)
        {
            Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
            throw new InternalError("Signature exception while initializing");
        }

        initKeyStore.setKeyEntry(privateKeyAlias, keyPair.getPrivate(), kDefaultKeyStorePassword.toCharArray(), new Certificate[]{mySelfSignedCert});
        initKeyStore.setCertificateEntry(kEntityAuthenticationServerAuthenticationCertAlias, entityAuthenticationServerCertificate);
        initKeyStore.setCertificateEntry(kMyCertAlias, mySelfSignedCert);
        writeKeystore(keyStoreFile, initKeyStore);
    }

    protected static void writeKeystore(File keyStoreFile, KeyStore initKeyStore)
            throws FileNotFoundException, KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException
    {
        FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStoreFile);
        initKeyStore.store(keyStoreOutputStream, kDefaultKeyStorePassword.toCharArray());
        keyStoreOutputStream.close();
    }

    public EntityID getServerIdentity()
    {
        return clientIdentity;
    }

    public void checkSocket(Socket checkSocket, EntityID expectedID) throws AuthenticationFailureException
    {
    	if (checkSocket instanceof SSLSocket)
    	{
    		checkSSLSocket((SSLSocket)checkSocket, expectedID);
    		return;
    	}
    	if (checkSocket instanceof AFUnixAuthenticatedSocket)
    	{
    		checkAFUnixAuthenticatedSocket((AFUnixAuthenticatedSocket)checkSocket, expectedID);
    		return;
    	}
    	throw new IllegalArgumentException("Unsupported socket type "+checkSocket.getClass());
    }
    public void checkSSLSocket(SSLSocket checkSocket, EntityID expectedID)
    throws AuthenticationFailureException
    {
        try
        {
            Certificate [] serverCertificates = checkSocket.getSession().getPeerCertificates();
            X509Certificate serverCertificate = (X509Certificate) serverCertificates[0];
            EntityID certificateID = EntityAuthentication.getObjectIDFromCertificateSerialNumber(serverCertificate);
            if (certificateID.equals(expectedID))
            {
                serverCertificate.checkValidity();
                boolean serverValidated = false;
                try
                {
                    for (X509Certificate checkCertificate:trustedServerCertificates.values())
                    {
                        if (dnMatches(checkCertificate.getSubjectDN(), serverCertificate.getIssuerDN()))
                        {
                            serverCertificate.verify(checkCertificate.getPublicKey());
                            serverValidated = true;
                        }
                    }
                    if (!serverValidated)   // If we're not validated by the time we get here, none of our certificates matched
                        throw new AuthenticationFailureException();
                } catch (NoSuchAlgorithmException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new InternalError("Could not get signature algorithm "+EntityAuthenticationServer.kCertificateSignatureAlg);
                } catch (InvalidKeyException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new InternalError("Got InvalidKeyException from authenticationServerCertificate");
                } catch (CertificateEncodingException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new AuthenticationFailureException();
                } catch (SignatureException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new AuthenticationFailureException();
                } catch (CertificateException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new AuthenticationFailureException();
                } catch (NoSuchProviderException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new InternalError("Got NoSuchProviderException from authenticationServerCertificate");
                }
                
            }
            return;
        } catch (SSLPeerUnverifiedException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (CertificateExpiredException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (CertificateNotYetValidException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        }
        throw new AuthenticationFailureException();
    }
    
    public void checkAFUnixAuthenticatedSocket(AFUnixAuthenticatedSocket checkSocket, EntityID expectedID) throws AuthenticationFailureException
    {
    	try
    	{
    		Certificate [] serverCertificates = checkSocket.getPeerCertificates();
    		X509Certificate serverCertificate = (X509Certificate) serverCertificates[0];
    		EntityID certificateID = EntityAuthentication.getObjectIDFromCertificateSerialNumber(serverCertificate);
    		if (certificateID.equals(expectedID))
    		{
    			serverCertificate.checkValidity();
    			boolean serverValidated = false;
    			try
    			{
    				for (X509Certificate checkCertificate:trustedServerCertificates.values())
    				{
    					if (dnMatches(checkCertificate.getSubjectDN(), serverCertificate.getIssuerDN()))
    					{
    						serverCertificate.verify(checkCertificate.getPublicKey());
    						serverValidated = true;
    					}
    				}
    				if (!serverValidated)   // If we're not validated by the time we get here, none of our certificates matched
    				throw new AuthenticationFailureException();
    			} catch (NoSuchAlgorithmException e)
    			{
    				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    				throw new InternalError("Could not get signature algorithm "+EntityAuthenticationServer.kCertificateSignatureAlg);
    			} catch (InvalidKeyException e)
    			{
    				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    				throw new InternalError("Got InvalidKeyException from authenticationServerCertificate");
    			} catch (CertificateEncodingException e)
    			{
    				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    				throw new AuthenticationFailureException();
    			} catch (SignatureException e)
    			{
    				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    				throw new AuthenticationFailureException();
    			} catch (CertificateException e)
    			{
    				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    				throw new AuthenticationFailureException();
    			} catch (NoSuchProviderException e)
    			{
    				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    				throw new InternalError("Got NoSuchProviderException from authenticationServerCertificate");
    			}

    		}
    		return;
    	} catch (CertificateExpiredException e)
    	{
    		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    	} catch (CertificateNotYetValidException e)
    	{
    		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    	}
    	throw new AuthenticationFailureException();
    }
    
    public EntityID getClientAuthenticatedIDForSocket(SSLSocket checkSocket)
    throws AuthenticationFailureException
    {
    	return getClientEntityAuthenticationForSocket(checkSocket).getEntityID();
    }
    
    public EntityAuthentication getClientEntityAuthenticationForSocket(SSLSocket checkSocket)
    throws AuthenticationFailureException
    {
        try
        {
            Certificate [] clientCertificates = checkSocket.getSession().getPeerCertificates();
            X509Certificate clientCertificate = (X509Certificate) clientCertificates[0];
            clientCertificate.checkValidity();
                try
                {
                    boolean serverValidated = false;
                    for (X509Certificate checkCertificate:trustedServerCertificates.values())
                    {
                        if (dnMatches(checkCertificate.getSubjectDN(), clientCertificate.getIssuerDN()))
                        {
                            clientCertificate.verify(checkCertificate.getPublicKey());
                            serverValidated = true;
                        }
                    }
                    if (!serverValidated)   // If we're not validated by the time we get here, none of our certificates matched
                        throw new AuthenticationFailureException();
                } catch (NoSuchAlgorithmException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new InternalError("Could not get signature algorithm "+EntityAuthenticationServer.kCertificateSignatureAlg);
                } catch (InvalidKeyException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new InternalError("Got InvalidKeyException from authenticationServerCertificate");
                } catch (CertificateEncodingException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new AuthenticationFailureException();
                } catch (SignatureException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new AuthenticationFailureException();
                } catch (CertificateException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new AuthenticationFailureException();
                } catch (NoSuchProviderException e)
                {
                    Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
                    throw new InternalError("Got NoSuchProviderException from authenticationServerCertificate");
                }
                
            
            return new EntityAuthentication(clientCertificate);
        } catch (SSLPeerUnverifiedException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (CertificateExpiredException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (CertificateNotYetValidException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        }
        throw new AuthenticationFailureException();
    }
    
    public EntityAuthentication getServerEntityAuthenticationForSocket(SSLSocket checkSocket)
    throws AuthenticationFailureException
    {
    	try
    	{
    		Certificate [] serverCertificates = checkSocket.getSession().getLocalCertificates();
    		if (logger.isDebugEnabled())
    		{
    			logger.debug("Number of serverCertficates = "+serverCertificates.length);
    			for (int curCertNum = 0; curCertNum < serverCertificates.length; curCertNum++)
    				logger.debug(curCertNum+":"+serverCertificates[curCertNum].toString());
    		}
    		X509Certificate serverCertificate = (X509Certificate) serverCertificates[0];
    		serverCertificate.checkValidity();
    		try
    		{
    			boolean serverValidated = false;
    			for (X509Certificate checkCertificate:trustedServerCertificates.values())
    			{
    				if (logger.isDebugEnabled())
    					logger.debug("Checking against trustedServerCertificate:"+checkCertificate.toString());
    				if (dnMatches(checkCertificate.getSubjectDN(), serverCertificate.getIssuerDN()))
    				{
    					logger.debug("DN matches");
    					serverCertificate.verify(checkCertificate.getPublicKey());
    					serverValidated = true;
    					logger.debug("Passed check!");
    				}
    			}
    			if (!serverValidated)   // If we're not validated by the time we get here, none of our certificates matched
    			{
    				logger.error("No trusted certificate found for check certificate "+serverCertificate.toString());
    				logger.error("Number of serverCertficates = "+serverCertificates.length);
    				for (int curCertNum = 0; curCertNum < serverCertificates.length; curCertNum++)
    					logger.error(curCertNum+":"+serverCertificates[curCertNum].toString());
    				logger.error("Number of trusted server certificates = "+trustedServerCertificates.size());
    				for (X509Certificate checkCertificate:trustedServerCertificates.values())
    				{
    					logger.error(checkCertificate.toString());
    				}
    				logger.error("Checking for DN "+serverCertificate.getIssuerDN().toString());
    				for (X509Certificate checkCertificate:trustedServerCertificates.values())
    				{
    					logger.error("subjectDN = "+checkCertificate.getSubjectDN());
    				}
    				throw new AuthenticationFailureException();
    			}
    		} catch (NoSuchAlgorithmException e)
    		{
    			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			throw new InternalError("Could not get signature algorithm "+EntityAuthenticationServer.kCertificateSignatureAlg);
    		} catch (InvalidKeyException e)
    		{
    			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			throw new InternalError("Got InvalidKeyException from authenticationServerCertificate");
    		} catch (CertificateEncodingException e)
    		{
    			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			throw new AuthenticationFailureException();
    		} catch (SignatureException e)
    		{
    			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			throw new AuthenticationFailureException();
    		} catch (CertificateException e)
    		{
    			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			throw new AuthenticationFailureException();
    		} catch (NoSuchProviderException e)
    		{
    			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    			throw new InternalError("Got NoSuchProviderException from authenticationServerCertificate");
    		}
    		return new EntityAuthentication(serverCertificate);
    	} catch (CertificateExpiredException e)
    	{
    		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    	} catch (CertificateNotYetValidException e)
    	{
    		Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
    	}
    	throw new AuthenticationFailureException();
    }

    protected abstract void initializeBonjour() throws Exception;

    protected static EntityAuthenticationServer serverFound(java.lang.String hostName, int port)
    {
    	EntityAuthenticationServer foundEntityAuthenticationServer = null;
        long startTime = System.currentTimeMillis();
        boolean notBound = true;
        while (notBound && System.currentTimeMillis() - startTime < 30000)
        {
            try
            {
            	/*SocketAddress entityAuthenticationServerAddress = new InetSocketAddress(hostName, port);
            	foundEntityAuthenticationServer = new EntityAuthenticationServerNewRMIClient(entityAuthenticationServerAddress);*/
            	Registry locateRegistry = LocateRegistry.getRegistry(hostName, port);
                String [] availableServices = locateRegistry.list();
                Logger.getLogger(EntityAuthenticationClient.class).debug("Attempting to bind entity authentication client for "+hostName+":"+port);
                Logger.getLogger(EntityAuthenticationClient.class).debug("Services available:");
                for (String curService:availableServices)
                {
                	Logger.getLogger(EntityAuthenticationClient.class).debug(curService);
                }
                foundEntityAuthenticationServer = (EntityAuthenticationServer)locateRegistry.lookup(EntityAuthenticationServer.kIndelibleEntityAuthenticationServerRMIName);
                allocateSingleton();
                singleton.addServer(foundEntityAuthenticationServer);
                notBound = false;
            } catch (AccessException e)
            {
                Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
            } catch (RemoteException e)
            {
                Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
            } catch (IOException e)
            {
                Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
            } catch (NotBoundException e)
			{
            	 Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
			}
            if (notBound)
            {
                try
                {
                    Thread.sleep(10000);
                } catch (InterruptedException e)
                {
                    Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
                }
            }
        }
        long elapsed = System.currentTimeMillis() - startTime;
        if (notBound)
            Logger.getLogger(EntityAuthenticationClient.class).error("Could not connect to entity authentication server advertised at "+hostName+":"+port+" waited for "+elapsed+" ms");
        else
            Logger.getLogger(EntityAuthenticationClient.class).info("Found service");
        return foundEntityAuthenticationServer;
    }

    /*
     * Adds an authentication server.  May return a different server object if the remote server has already been added
     */
    protected EntityAuthenticationServer addServer(EntityAuthenticationServer foundEntityAuthenticationServer)
            throws RemoteException
    {
    	if (foundEntityAuthenticationServer != null)
    	{
    		boolean sendAddedEvent = false;
    		synchronized(entityAuthenticationServers)
    		{
    			if (!entityAuthenticationServers.contains(foundEntityAuthenticationServer))
    			{
    				Logger.getLogger(EntityAuthenticationClient.class).warn(new WarnLogMessage("Found entity authentication server "+foundEntityAuthenticationServer.getEntityID()));
    				entityAuthenticationServers.add(foundEntityAuthenticationServer);
    				entityAuthenticationServers.notifyAll();
    				sendAddedEvent = true;
    			}
    			else
    			{
    				foundEntityAuthenticationServer = entityAuthenticationServers.get(entityAuthenticationServers.indexOf(foundEntityAuthenticationServer));
    			}
    		}
    		if (sendAddedEvent && singleton != null)
    			singleton.fireEntityAuthenticationServerAppearedEvent(foundEntityAuthenticationServer);
    	}
        return foundEntityAuthenticationServer;
    }

    public static EntityAuthenticationServer[] listEntityAuthenticationServers()
    {
    	allocateSingleton();
    	return singleton.listEntityAuthenticationServersInternal();
    }
    public EntityAuthenticationServer[] listEntityAuthenticationServersInternal()
    {
        EntityAuthenticationServer [] returnList = new EntityAuthenticationServer[entityAuthenticationServers.size()];
        returnList = entityAuthenticationServers.toArray(returnList);
        return returnList;
    }

    public EntityAuthenticationServer [] listTrustedServers()
    {
        EntityAuthenticationServer [] returnList = new EntityAuthenticationServer[trustedServers.size()];
        synchronized(trustedServers)
        {
        	returnList = trustedServers.toArray(returnList);
        }
        return returnList;
    }
    /**
     * Adds a server to the trusted list.  This should take an authentication as well - FIXME!
     * If the server is not already in the list of security servers it will be added
     * @param serverToTrust
     */
    public void trustServer(EntityID serverToTrustID)
    {
        try
        {
            for (EntityAuthenticationServer checkServer:entityAuthenticationServers)
            {
                if (checkServer.getEntityID().equals(serverToTrustID))
                    trustServer(checkServer);
            }
        } catch (RemoteException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        }
    }
    
    /**
     * Adds a server to the trusted list.  This should take an authentication as well - FIXME!
     * If the server is not already in the list of security servers it will be added
     * @param serverToTrust
     */
    public void trustServer(EntityAuthenticationServer serverToTrust)
    {
        try
        {
            addServer(serverToTrust);
            serverToTrust.registerServer(mySelfSignedCert);
            synchronized(trustedServers)
            {
                EntityID securityServerID = serverToTrust.getEntityID();
                String securityServerIDStr = securityServerID.toString();
                if (!trustedServerCertificates.containsKey(securityServerIDStr))
                {
                    trustedServers.add(serverToTrust);
                    X509Certificate securityServerCertificate = (X509Certificate) serverToTrust.getServerCertificate();

                    persistentKeyStore.setCertificateEntry(kEntityAuthenticationServerAuthenticationCertAlias+securityServerIDStr, securityServerCertificate);
                    writeKeystore(keyStoreFile, persistentKeyStore);
                    trustedServerCertificates.put(securityServerIDStr, securityServerCertificate);
                }
            }
            fireEntityAuthenticationServerTrustedEvent(serverToTrust);
        } catch (RemoteException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        }   // Make sure it's in the list
        catch (InvalidKeyException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (CertificateException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (NoSuchAlgorithmException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (NoSuchProviderException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (SignatureException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (KeyStoreException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (FileNotFoundException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        } catch (IOException e)
        {
            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
        }
    }
    
    public void untrustServer(EntityAuthenticationServer serverToUntrust)
    {
        synchronized(trustedServers)
        {
            trustedServers.remove(serverToUntrust);
        }
        fireSecurityServerUntrustedEvent(serverToUntrust);
    }
    
    public static void startSearchForServers()
    {
        try
        {
        	allocateSingleton();
            singleton.initializeBonjour();
        } catch (Exception e)
        {
            Logger.getLogger(EntityAuthenticationClient.class).error(new ErrorLogMessage("Caught exception"), e);
        }
    }
    
    public void addEntityAuthenticationClientListener(EntityAuthenticationClientListener listener)
    {
        synchronized(eventListeners)
        {
            eventListeners.add(EntityAuthenticationClientListener.class, listener);
        }
    }
    
    public void removeSecurityClientListener(EntityAuthenticationClientListener removeListener)
    {
        synchronized(eventListeners)
        {
            eventListeners.remove(EntityAuthenticationClientListener.class, removeListener);
        }
    }
    
    protected void fireEntityAuthenticationServerAppearedEvent(EntityAuthenticationServer appearedServer)
    {
        EntityAuthenticationServerAppearedEvent appearedEvent = null;
        Object [] listeners;
        synchronized (eventListeners)
        {
            listeners = eventListeners.getListenerList();
        }
        for (int index = 0; index < listeners.length; index+= 2)
        {
            if (listeners[index]==EntityAuthenticationClientListener.class)
            {
                if (appearedEvent == null)
                {
                    appearedEvent = new EntityAuthenticationServerAppearedEvent(this, appearedServer);
                }
                ((EntityAuthenticationClientListener)listeners[index + 1]).entityAuthenticationServerAppeared(appearedEvent);
            }
        }
    }
    
    protected void fireEntityAuthenticationServerDisappearedEvent(EntityAuthenticationServer disappearedServer)
    {
        EntityAuthenticationServerDisappearedEvent disappearedEvent = null;
        Object [] listeners;
        synchronized (eventListeners)
        {
            listeners = eventListeners.getListenerList();
        }
        for (int index = 0; index < listeners.length; index+= 2)
        {
            if (listeners[index]==EntityAuthenticationClientListener.class)
            {
                if (disappearedEvent == null)
                {
                    disappearedEvent = new EntityAuthenticationServerDisappearedEvent(this, disappearedServer);
                }
                ((EntityAuthenticationClientListener)listeners[index + 1]).entityAuthenticationServerDisappeared(disappearedEvent);
            }
        }
    }
    
    protected void fireEntityAuthenticationServerTrustedEvent(EntityAuthenticationServer trustedServer)
    {
        EntityAuthenticationServerTrustedEvent trustedEvent = null;
        Object [] listeners;
        synchronized (eventListeners)
        {
            listeners = eventListeners.getListenerList();
        }
        for (int index = 0; index < listeners.length; index+= 2)
        {
            if (listeners[index]==EntityAuthenticationClientListener.class)
            {
                if (trustedEvent == null)
                {
                    trustedEvent = new EntityAuthenticationServerTrustedEvent(this, trustedServer);
                }
                ((EntityAuthenticationClientListener)listeners[index + 1]).entityAuthenticationServerTrusted(trustedEvent);
            }
        }
    }
    
    protected void fireSecurityServerUntrustedEvent(EntityAuthenticationServer untrustedServer)
    {
        EntityAuthenticationServerUntrustedEvent untrustedEvent = null;
        Object [] listeners;
        synchronized (eventListeners)
        {
            listeners = eventListeners.getListenerList();
        }
        for (int index = 0; index < listeners.length; index+= 2)
        {
            if (listeners[index]==EntityAuthenticationClientListener.class)
            {
                if (untrustedEvent == null)
                {
                    untrustedEvent = new EntityAuthenticationServerUntrustedEvent(this, untrustedServer);
                }
                ((EntityAuthenticationClientListener)listeners[index + 1]).entityAuthenticationServerUntrusted(untrustedEvent);
            }
        }
    }

    public boolean isTrusted(EntityID entityAuthenticationServerID) throws RemoteException
    {
    	EntityAuthenticationServer [] tsArray = new EntityAuthenticationServer[trustedServers.size()];
    	synchronized(trustedServers)
    	{
    		tsArray = trustedServers.toArray(tsArray);	// Avoid concurrent modification errors
    	}
        for (EntityAuthenticationServer checkServer:tsArray)
        {
            if (checkServer.getEntityID().equals(entityAuthenticationServerID))
                return true;
        }
        return false;
    }
    
    public SessionAuthentication authorizeEntityForSession(EntityAuthentication entity, DataMoverSessionID sessionID) 
    		throws SSLPeerUnverifiedException, CertificateParsingException, CertificateEncodingException, InvalidKeyException, 
    		UnrecoverableKeyException, IllegalStateException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException, 
    		KeyStoreException
    {
        X509Certificate cert = generateCertificateToEntity(entity, sessionID);
        return new SessionAuthentication(sessionID, cert);
        
    }
    
    public SessionAuthentication forwardAuthentication(EntityAuthentication forwardTo, SessionAuthentication baseAuthentication) throws SSLPeerUnverifiedException, CertificateParsingException, CertificateEncodingException, InvalidKeyException, UnrecoverableKeyException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException, KeyStoreException
    {
    	X509Certificate [] baseCertificates = baseAuthentication.getCertificateChain();
    	X509Certificate lastCert = baseCertificates[baseCertificates.length - 1];
    	X500Name lastCertPrincipal = (X500Name) lastCert.getSubjectDN();
    	String principalName = lastCertPrincipal.getName();
    	if (principalName.startsWith("CN="))
    	{
    		EntityID targetOID = getEntityIDFromPrincipalName(principalName);
    		if (!targetOID.equals(clientIdentity))
    			throw new IllegalArgumentException("Authentication is issued to "+targetOID.toString()+", not our iD ("+clientIdentity.toString()+")");
    		// We'll just assume it authenticates OK
    		X509Certificate forwardCert = generateCertificateToEntity(forwardTo, baseAuthentication.getSessionID());
    		SessionAuthentication returnAuthentication = new SessionAuthentication(baseAuthentication, forwardCert);
    		return returnAuthentication;
    	}
    	else
    	{
    		throw new IllegalArgumentException("Malformed session authentication");
    	}
    }

    public EntityID getEntityIDFromCertificate(X509Certificate certificate)
    {
    	return getEntityIDFromPrincipalX500Name((X500Name)certificate.getSubjectDN());
    }
    
    public EntityID getEntityIDFromPrincipalX500Name(X500Name principalName)
    {
    	return getEntityIDFromPrincipalName(principalName.toString());
    }
    
	public EntityID getEntityIDFromPrincipalName(String principalName)
	{
		if (!principalName.startsWith("CN="))
			throw new IllegalArgumentException("Principal name must start with CN=, principalName = "+principalName);
		String targetOIDStr;
		if (principalName.startsWith(kEntityIDCNPrefix))
			targetOIDStr = principalName.substring(kEntityIDCNPrefix.length()).trim();
		else
			targetOIDStr = principalName.substring(3).trim();
		EntityID targetOID = (EntityID) ObjectIDFactory.reconstituteFromString(targetOIDStr);
		return targetOID;
	}
    
	private X509Certificate generateCertificateToEntity(
			EntityAuthentication entity, DataMoverSessionID sessionID)
			throws SSLPeerUnverifiedException, CertificateParsingException,
			CertificateEncodingException, NoSuchProviderException,
			NoSuchAlgorithmException, SignatureException, InvalidKeyException,
			KeyStoreException, UnrecoverableKeyException
	{
		X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal              dnName = new X500Principal("CN="+entity.getEntityID().toString());

        certGen.setSerialNumber(sessionID.toBigInteger());
        X509Certificate rootCertificate = null;
        for (X509Certificate checkCertificate:trustedServerCertificates.values())
        {
            try
            {
                entity.getCertificate().verify(checkCertificate.getPublicKey(), "BC");
                rootCertificate = checkCertificate;
                break;
            }
            catch (GeneralSecurityException e)
            {
                Logger.getLogger(getClass()).debug(new ErrorLogMessage("Skipping certificate {0}", (Serializable)checkCertificate.getSubjectDN().getName()));
            }
        }
        if (rootCertificate == null)
        	 throw new SSLPeerUnverifiedException("No certificates authenticated");
        certGen.setIssuerDN(rootCertificate.getSubjectX500Principal());
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 60L * 60L * 1000L));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + (365L * 24L * 60L * 1000L)));
        certGen.setSubjectDN(dnName);                       // note: same as issuer
        certGen.setPublicKey(entity.getCertificate().getPublicKey());
        certGen.setSignatureAlgorithm(EntityAuthenticationServer.kCertificateSignatureAlg);

        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(rootCertificate));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(entity.getCertificate().getPublicKey()));
        byte [] sessionIDBytes = new byte[DataMoverSessionID.kTotalBytes];
        sessionID.getBytes(sessionIDBytes, 0);
        certGen.addExtension(X509Extensions.SubjectAlternativeName, false, sessionIDBytes);
        byte [] issuerIDBytes = new byte[EntityID.kTotalBytes];
        clientIdentity.getBytes(issuerIDBytes, 0);
        certGen.addExtension(X509Extensions.IssuerAlternativeName, false, issuerIDBytes);
        
        X509Certificate cert = certGen.generate((PrivateKey)persistentKeyStore.getKey(kPrivateKeyAliasPrefix+id.toString(), kDefaultKeyStorePassword.toCharArray()), "BC");
        return cert;
	}

	/**
	 * Validates a session authentication.  The session authentication certificate chain must be rooted in this
	 * EntityAuthenticationClient's public key
	 * 
	 * @param expectedSessionID
	 * @param checkAuthentication
	 * @throws CertificateException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws SignatureException
	 * @throws IOException
	 */
	public void checkSessionAuthentication(DataMoverSessionID expectedSessionID, SessionAuthentication checkAuthentication) 
			throws CertificateException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, IOException
	{
		// We validate the chain of certificates.  We assume that we are the root for this chain, so we start with the checkKey
		// being our own key.  As each cert is validated, we let the receiver be the check key
		X509Certificate [] certificates = checkAuthentication.getCertificateChain();
		PublicKey checkKey = publicKey;
		
		for (X509Certificate checkCert:certificates)
		{
			checkCert.checkValidity();
			DataMoverSessionID checkSessionID = getSessionIDFromCertificate(checkCert);
			if (!expectedSessionID.equals(checkSessionID))
				throw new CertificateException("Session ID does not match");
			checkCert.verify(checkKey, "BC");
			checkKey = checkCert.getPublicKey();
		}
	}

	public EntityID checkCertificateChain(X509Certificate [] certificateChain, EntityID securityServerID) throws CertificateException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException
	{
		// Last certificate in the chain must be one of our root servers
		X509Certificate startCert = certificateChain[certificateChain.length - 1];
		X509Certificate trustedCert =  trustedServerCertificates.get(securityServerID.toString());
		if (trustedCert == null)
			throw new CertificateException("Could not find certificate for "+securityServerID.toString());
		if (!trustedCert.equals(startCert))
		{
			throw new CertificateException("Certifcate chain starts from "+startCert.getIssuerDN()+" not from expected server "+securityServerID.toString());
		}

		PublicKey checkKey = trustedCert.getPublicKey();
		for (int checkCertNum = certificateChain.length - 2; checkCertNum >= 0; checkCertNum--)
		{
			X509Certificate checkCert = certificateChain[checkCertNum];
			checkCert.verify(checkKey, "BC");
			checkKey = checkCert.getPublicKey();
		}
		return getEntityIDFromCertificate(certificateChain[0]);
	}
	public static DataMoverSessionID getSessionIDFromCertificate(
			X509Certificate checkCert) throws IOException
	{
		byte [] checkSessionIDBytesEncoded = checkCert.getExtensionValue(X509Extensions.SubjectAlternativeName.toString());
		ASN1InputStream decoder = new ASN1InputStream(new ByteArrayInputStream(checkSessionIDBytesEncoded));
		DERObject checkObject = decoder.readObject();
		DEROctetString checkOctetString = (DEROctetString)checkObject;
		byte [] checkSessionIDBytes = checkOctetString.getOctets();
		DataMoverSessionID checkSessionID = (DataMoverSessionID) ObjectIDFactory.reconstituteFromBytes(checkSessionIDBytes);
		return checkSessionID;
	}
	
	public boolean dnMatches(Principal p1, Principal p2) 
	{
	    try
		{
			List<Rdn> rdn1 = new LdapName(p1.getName()).getRdns();
			List<Rdn> rdn2 = new LdapName(p2.getName()).getRdns();

			if(rdn1.size() != rdn2.size())
			    return false;

			return rdn1.containsAll(rdn2);
		} catch (InvalidNameException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	    return false;
	}
}
