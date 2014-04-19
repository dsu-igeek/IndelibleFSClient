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
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketChannelImpl;

import com.igeekinc.firehose.CommandMessage;
import com.igeekinc.firehose.FirehoseClient;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.AuthenticateServerMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.AuthenticateServerReplyMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.GetEntityIDMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.GetEntityIDReplyMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.RegisterServerMessage;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class EntityAuthenticationServerFirehoseClient extends FirehoseClient implements
		EntityAuthenticationServer 
{
	public enum EntityAuthenticationCommand
	{
		kRegisterServerCommand(1),
		kAuthenticateServerCommand(2),
		kGetEntityID(3);
		
		int commandNum;
		private EntityAuthenticationCommand(int commandNum)
		{
			this.commandNum = commandNum;
		}
		
		public int getCommandNum()
		{
			return commandNum;
		}
		
		public static EntityAuthenticationCommand getCommandForNum(int num)
		{
			switch(num)
			{
			case 1:
				return kRegisterServerCommand;
			case 2:
				return kAuthenticateServerCommand;
			case 3:
				return kGetEntityID;
			}
			throw new IllegalArgumentException();
		}
	}
	public EntityAuthenticationServerFirehoseClient(SocketAddress address) throws IOException
	{
		socket = new Socket();
		if (address instanceof AFUNIXSocketAddress)
			socketChannel = AFUNIXSocketChannelImpl.open((AFUNIXSocketAddress)address);
		else
			socketChannel = SocketChannel.open(address);
		socket = socketChannel.socket();
		SSLContext sslContext;
		try
		{
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new X509TrustManager[]{new EntityAuthenticationBootTrustManager()}, new SecureRandom());
		} catch (Exception e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Could not initialize SSL Context");
		} 
		SSLEngine sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(true);
		createResponseLoop(sslEngine);
	}
	
	@Override
	public void registerServer(X509Certificate selfSignedServerCert)
			throws InvalidKeyException, CertificateException,
			NoSuchAlgorithmException, NoSuchProviderException,
			SignatureException, KeyStoreException, RemoteException
	{
		RegisterServerMessage addCommand = new RegisterServerMessage(selfSignedServerCert);
		ComboFutureBase<Integer>future = new ComboFutureBase<Integer>();
		
		try
		{
			sendMessage(addCommand, future);
			future.get();
		} 
		catch (ExecutionException ee)
		{
			try
			{
				throw ee.getCause();
			}
			catch (InvalidKeyException e)
			{
				throw e;
			}
			catch(CertificateException e)
			{
				throw e;
			}
			catch(NoSuchAlgorithmException e)
			{
				throw e;
			}
			catch (NoSuchProviderException e)
			{
				throw e;
			}
			catch (SignatureException e)
			{
				throw e;
			}
			catch(KeyStoreException e)
			{
				throw e;
			}
			catch (Throwable e)
			{
				throw new InternalError("Could not execute");
			}
		}
		catch(InterruptedException e)
		{
			throw new RemoteException();
		}
		catch (IOException e)
		{
			throw new RemoteException();
		}
	}

	@Override
	public EntityAuthentication authenticateServer(EntityID serverID,
			byte[] encodedCertReq) throws CertificateEncodingException,
			InvalidKeyException, IllegalStateException,
			NoSuchProviderException, NoSuchAlgorithmException,
			SignatureException, UnrecoverableKeyException, KeyStoreException,
			RemoteException, IOException, CertificateParsingException
	{
		AuthenticateServerMessage authenticateCommand = new AuthenticateServerMessage(serverID, encodedCertReq);
		ComboFutureBase<AuthenticateServerReplyMessage>future = new ComboFutureBase<AuthenticateServerReplyMessage>();

		try
		{
			sendMessage(authenticateCommand, future);
			AuthenticateServerReplyMessage replyMessage = future.get();
			return replyMessage.getEntityAuthentication();
		} 
		catch (ExecutionException ee)
		{
			try
			{
				throw ee.getCause();
			}
			catch(CertificateEncodingException e)
			{
				throw e;
			}
			catch (InvalidKeyException e)
			{
				throw e;
			}
			catch (IllegalStateException e)
			{
				throw e;
			}
			catch (NoSuchProviderException e)
			{
				throw e;
			}
			catch(NoSuchAlgorithmException e)
			{
				throw e;
			}
			catch (SignatureException e)
			{
				throw e;
			}
			catch(KeyStoreException e)
			{
				throw e;
			}
			catch(UnrecoverableKeyException e)
			{
				throw e;
			}
			catch(CertificateParsingException e)
			{
				throw e;
			}
			catch (Throwable e)
			{
				throw new InternalError("Could not execute");
			}
		}
		catch(InterruptedException e)
		{
			throw new RemoteException();
		}
		catch (IOException e)
		{
			throw new RemoteException();
		} catch (CertificateException e)
		{
			throw new InternalError("Got a certificate exception");
		}
	}

	@Override
	public Certificate getServerCertificate() throws KeyStoreException,
			RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] entityAuthenticationServerChallenge(byte[] bytesToSign)
			throws RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EntityID getEntityID() throws RemoteException
	{
		GetEntityIDMessage getEntityIDMessage = new GetEntityIDMessage();
		ComboFutureBase<GetEntityIDReplyMessage>future = new ComboFutureBase<GetEntityIDReplyMessage>();

		try
		{
			sendMessage(getEntityIDMessage, future);
			GetEntityIDReplyMessage replyMessage = future.get();
			return replyMessage.getEntityID();
		} 
		catch (ExecutionException ee)
		{
			try
			{
				throw ee.getCause();
			}
			catch (RemoteException e)
			{
				throw e;
			}
			catch (Throwable e)
			{
				throw new InternalError("Could not execute");
			}
		}
		catch(InterruptedException e)
		{
			throw new RemoteException();
		}
		catch (IOException e)
		{
			throw new RemoteException();
		} 
	}

	@Override
	protected Class<? extends CommandMessage> getClassForCommandCode(
			int commandCode)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Class<? extends Object> getReturnClassForCommandCode(
			int commandCode)
	{
		switch(EntityAuthenticationServerFirehoseClient.EntityAuthenticationCommand.getCommandForNum(commandCode))
		{
		case kRegisterServerCommand:
			return Void.class;
		case kAuthenticateServerCommand:
			return AuthenticateServerReplyMessage.class;
		case kGetEntityID:
			return GetEntityIDReplyMessage.class;
		default:
			break;
		}
		return null;
	}

}
