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
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.oid.DataMoverSessionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.logging.ErrorLogMessage;

/**
 * A SessionAuthentication is a ticket that enables an Entity to use a Data Mover Session.
 * The SessionAuthentication is issued by the mover when an Entity is authorized for a session
 * The SessionAuthentication may be forwarded, this allows for third-party I/O
 * SessionAuthentications may be serialized as Java objects, or more compactly as byte buffers.
 * The format for a SessionAuthentication byte buffer is:
 * kMagicNumber - 8 bytes
 * Session ID - 32 bytes
 * Number of certificates in chain - 4 bytes
 * Certificate size - 4 bytes
 * Encoded certificate (variable)
 * Certificate size - 4 bytes
 * 
 * The buffer is in Java byte order
 * 
 * SessionAuthentications are validated by the EntityAuthenticationClient
 * @author David L. Smith-Uchida
 *
 */
public class SessionAuthentication implements Serializable
{
	private static final long	serialVersionUID	= -7199800632545016055L;
	public static final int	kMagicNumber	= 0x129baacd;
	public static final int kMagicNumberOffset = 0;
	public static final int kSessionIDOffset = kMagicNumberOffset + 4;
	public static final int kNumCertificatesOffset = kSessionIDOffset + DataMoverSessionID.kTotalBytes;
	public static final int kTotalSizeOffset = kNumCertificatesOffset + 4;
	public static final int kSessionAuthenticationHeaderSize = kTotalSizeOffset + 4;

	private DataMoverSessionID sessionID;
	private X509Certificate [] certificateChain;
	public SessionAuthentication(DataMoverSessionID sessionID, X509Certificate cert)
	{
		this.sessionID = sessionID;
		certificateChain = new X509Certificate[1];
		certificateChain[0] = cert;
		try
		{
			if (!sessionID.equals(EntityAuthenticationClient.getSessionIDFromCertificate(cert)))
				throw new IllegalArgumentException("sessionID "+sessionID+" does not match cert encoded session ID "+EntityAuthenticationClient.getSessionIDFromCertificate(cert));
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IllegalArgumentException("Malformed cert - cannot retrieve sessionID");
		}
	}

	public SessionAuthentication(SessionAuthentication baseAuthentication, X509Certificate forwarding)
	{
		this.sessionID = baseAuthentication.sessionID;
		certificateChain = new X509Certificate[baseAuthentication.certificateChain.length + 1];
		System.arraycopy(baseAuthentication.certificateChain, 0, certificateChain, 0, baseAuthentication.certificateChain.length);
		certificateChain[certificateChain.length - 1] = forwarding;
		for (X509Certificate cert:certificateChain)
		{
			try
			{
				if (!sessionID.equals(EntityAuthenticationClient.getSessionIDFromCertificate(cert)))
					throw new IllegalArgumentException("sessionID "+sessionID+" does not match cert encoded session ID "+EntityAuthenticationClient.getSessionIDFromCertificate(cert));
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new IllegalArgumentException("Malformed cert - cannot retrieve sessionID");
			}
		}
	}
	
	public SessionAuthentication(byte [] encoded)
	{
		if (BitTwiddle.javaByteArrayToInt(encoded, kMagicNumberOffset) != kMagicNumber)
			throw new IllegalArgumentException("Does not start with magic number");
		int numCertificates = BitTwiddle.javaByteArrayToInt(encoded, kNumCertificatesOffset);
		sessionID = (DataMoverSessionID)ObjectIDFactory.reconstituteFromBytes(encoded, kSessionIDOffset, DataMoverSessionID.kTotalBytes);
		certificateChain = new X509Certificate[numCertificates];
		int bufferOffset = kSessionAuthenticationHeaderSize;
		CertificateFactory cf;
		try
		{
			cf = CertificateFactory.getInstance("X.509");

			for (int curCertNum = 0; curCertNum < numCertificates; curCertNum++)
			{
				int certificateSize = BitTwiddle.javaByteArrayToInt(encoded, bufferOffset);
				bufferOffset += 4;
				byte [] certBuffer = new byte[certificateSize];
				System.arraycopy(encoded, bufferOffset, certBuffer, 0, certificateSize);
				bufferOffset += certificateSize;
				ByteArrayInputStream bais = new ByteArrayInputStream(certBuffer);
				certificateChain[curCertNum] = (X509Certificate)cf.generateCertificate(bais);
			}
		} catch (CertificateException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Certificate factory failure");
		}
	}
	
	public byte [] toBytes()
	{
		ArrayList<byte []>bufferList = new ArrayList<byte []>();
		byte [] header = new byte[kSessionAuthenticationHeaderSize];
		BitTwiddle.intToJavaByteArray(kMagicNumber, header, kMagicNumberOffset);
		sessionID.getBytes(header, kSessionIDOffset);
		BitTwiddle.intToJavaByteArray(certificateChain.length, header, kNumCertificatesOffset);
		
		bufferList.add(header);
		
		for (X509Certificate curCertificate:certificateChain)
		{
			try
			{
				byte [] certEncodedBytes = curCertificate.getEncoded();
				byte [] certLength = new byte[4];
				BitTwiddle.intToJavaByteArray(certEncodedBytes.length, certLength, 0);
				bufferList.add(certLength);
				bufferList.add(certEncodedBytes);
			} catch (CertificateEncodingException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new InternalError("Bad certificate");
			}
		}
		int returnBufferLength = 0;
		for (byte [] curBuffer:bufferList)
		{
			returnBufferLength += curBuffer.length;
		}
		BitTwiddle.intToJavaByteArray(returnBufferLength, header, kTotalSizeOffset);
		byte [] returnBuffer = new byte[returnBufferLength];
		
		int outOffset = 0;
		for (byte [] curBuffer:bufferList)
		{
			System.arraycopy(curBuffer, 0, returnBuffer, outOffset, curBuffer.length);
			outOffset += curBuffer.length;
		}
		return returnBuffer;
	}
	
	public X509Certificate [] getCertificateChain()
	{
		return certificateChain;
	}

	public DataMoverSessionID getSessionID()
	{
		return sessionID;
	}
	
	/**
	 * Returns the EntityID of the last certificate (i.e who this SessionAuthentication is valid for)
	 * @return
	 */
	public EntityID getAuthenticatedEntityID()
	{
		String dnName = certificateChain[certificateChain.length - 1].getSubjectDN().getName();
		if (dnName.startsWith("CN="))
		{
			String entityIDString=dnName.substring(3);
			return (EntityID)ObjectIDFactory.reconstituteFromString(entityIDString);
		}
		throw new InternalError("Subject DN ("+dnName+") is malformed");
	}
}
