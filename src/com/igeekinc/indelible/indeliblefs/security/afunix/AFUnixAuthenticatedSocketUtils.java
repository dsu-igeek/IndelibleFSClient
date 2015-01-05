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
 
package com.igeekinc.indelible.indeliblefs.security.afunix;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationServer;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.logging.ErrorLogMessage;

public class AFUnixAuthenticatedSocketUtils
{
	private static final char	kResponseCommand	= 'R';
	private static final int	kChallengeSize	= 1024;
	private static final char	kChallengeCommand	= 'P';
	private static final char	kCertificateCommand	= 'C';

	public static void writeCertChain(OutputStream outputStream, EntityID securityServerID) throws IOException
	{
		EntityAuthenticationClient eac = EntityAuthenticationClient.getEntityAuthenticationClient();
		KeyManager [] keyManagers = eac.getKeyManagers(securityServerID);
		if (keyManagers == null || keyManagers.length < 1)
			throw new IOException("No server certificates configured");
		X509KeyManager keyManager = (X509KeyManager)keyManagers[0];
		String alias = keyManager.chooseServerAlias("RSA", null, null);
		X509Certificate [] certChain = keyManager.getCertificateChain(alias);
		if (certChain == null || certChain.length == 0)
			throw new IOException("Got bad certificate chain from EntityAuthenticationClient");
		outputStream.write('C');
		if (certChain.length > 255)
			throw new IOException("Too many certificates in chain (max 255)");
		outputStream.write(certChain.length);
		for (X509Certificate curCert:certChain)
		{
			try
			{
				byte [] certBytes = curCert.getEncoded();
				sendInt(outputStream, certBytes.length);
				outputStream.write(certBytes);
			} catch (CertificateEncodingException e)
			{
				Logger.getLogger(AFUnixAuthenticatedSocketUtils.class).error(new ErrorLogMessage("Caught exception"), e);
				throw new IOException("Can't encode certificate");
			}
		}
	}

	public static void sendInt(OutputStream outputStream, int intToSent)
			throws IOException
	{
		byte [] intBytes = new byte[4];
		BitTwiddle.intToJavaByteArray(intToSent, intBytes, 0);
		outputStream.write(intBytes);
	}
	
	public static int readInt(InputStream inputStream) throws IOException
	{
		byte [] intBytes = new byte[4];
		if (inputStream.read(intBytes) != intBytes.length)
			throw new IOException("Short read");
		return BitTwiddle.javaByteArrayToInt(intBytes, 0);
	}
	
	public static X509Certificate [] readCertChain(InputStream inputStream) throws IOException
	{
		int firstByte = inputStream.read();
		if (firstByte != kCertificateCommand)
			throw new IOException("Expected starting "+kCertificateCommand+" got "+Integer.toString(firstByte));
		int numCerts = inputStream.read();
		X509Certificate [] returnCerts = new X509Certificate[numCerts];
		
		CertificateFactory cf;
		try
		{
			cf = CertificateFactory.getInstance("X.509");
			for (int curCertNum = 0; curCertNum < numCerts; curCertNum++)
			{
				int certLength = readInt(inputStream);
				if (certLength > 16*1024)
					throw new IOException("Certifcate marked too long ("+certLength+") max = 16K");
				byte [] certBuffer = new byte[certLength];
				if (inputStream.read(certBuffer) != certLength)
					throw new IOException("Short read on certificate "+curCertNum);
				ByteArrayInputStream bais = new ByteArrayInputStream(certBuffer);
				returnCerts[curCertNum] = (X509Certificate)cf.generateCertificate(bais);
			}
		} catch (CertificateException e)
		{
			Logger.getLogger(AFUnixAuthenticatedSocketUtils.class).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Certificate exception");
		}
		return returnCerts;
	}
	
	public static void challengePeer(OutputStream outputStream, InputStream inputStream, X509Certificate peerCertificate) throws IOException, AuthenticationFailureException
	{
        try
		{
			SecureRandom challengeGenerator = SecureRandom.getInstance("SHA1PRNG");
			// challengeGenerator should self-seed
			byte [] challengeBytes = new byte[kChallengeSize];
			challengeGenerator.nextBytes(challengeBytes);
			BitTwiddle.longToJavaByteArray(System.currentTimeMillis(), challengeBytes, 0);
			
			outputStream.write(kChallengeCommand);
			sendInt(outputStream, challengeBytes.length);
			outputStream.write(challengeBytes);
			if (inputStream.read() != kResponseCommand)
				throw new IOException("Expected "+kResponseCommand);
			int responseSize = readInt(inputStream);
			byte [] checkSignatureBytes = new byte[responseSize];
			if (inputStream.read(checkSignatureBytes) != responseSize)
				throw new IOException("Short read on response signature bytes");
			Signature checkSignature = Signature.getInstance(EntityAuthenticationServer.kChallengeSignatureAlg, "BC");
			checkSignature.initVerify(peerCertificate);
			checkSignature.update(challengeBytes);
			if (!checkSignature.verify(checkSignatureBytes))
			{
			    throw new AuthenticationFailureException();
			}
			return;
		} catch (InvalidKeyException e)
		{
			Logger.getLogger(AFUnixAuthenticatedSocketUtils.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchAlgorithmException e)
		{
			Logger.getLogger(AFUnixAuthenticatedSocketUtils.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchProviderException e)
		{
			Logger.getLogger(AFUnixAuthenticatedSocketUtils.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (SignatureException e)
		{
			Logger.getLogger(AFUnixAuthenticatedSocketUtils.class).error(new ErrorLogMessage("Caught exception"), e);
		}
        throw new IOException("Security exception");
	}
	
	public static void handleChallenge(InputStream inputStream, OutputStream outputStream) throws IOException
	{
		if (inputStream.read() != kChallengeCommand)
			throw new IOException("Expected starting "+kChallengeCommand);
		int challengeSize = readInt(inputStream);
		if (challengeSize != kChallengeSize)
			throw new IOException("Invalid challenge size "+challengeSize);
		byte [] challengeBytes = new byte[kChallengeSize];
		if (inputStream.read(challengeBytes) != kChallengeSize)
			throw new IOException("Short read on challenge bytes");
		byte [] signedBytes = EntityAuthenticationClient.getEntityAuthenticationClient().signChallenge(challengeBytes);
		outputStream.write(kResponseCommand);
		sendInt(outputStream, signedBytes.length);
		outputStream.write(signedBytes);
	}
}
