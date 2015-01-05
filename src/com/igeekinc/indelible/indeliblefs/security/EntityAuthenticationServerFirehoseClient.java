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
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import com.igeekinc.firehose.FirehoseClient;
import com.igeekinc.firehose.FirehoseInitiator;
import com.igeekinc.firehose.SSLSetup;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.AuthenticateServerMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.AuthenticateServerReplyMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.GetEntityIDMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.GetEntityIDReplyMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.GetServerCertificateMessage;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.GetServerCertificateReplyMessage;
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
		kGetEntityID(3),
		kGetServerCertificate(4);
		
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
			case 4:
				return kGetServerCertificate;
			}
			throw new IllegalArgumentException();
		}
	}
	
	private SSLContext sslContext;
	private EntityID cachedEntityID = null;
	public EntityAuthenticationServerFirehoseClient(SocketAddress address) throws IOException
	{
		try
		{
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new X509TrustManager[]{new EntityAuthenticationBootTrustManager()}, new SecureRandom());
		} catch (Exception e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new IOException("Could not initialize SSL Context");
		} 
		FirehoseInitiator.initiateClient(address, this, new SSLSetup()
		{
			
			@Override
			public boolean useSSL()
			{
				return true;
			}
			
			@Override
			public SSLContext getSSLContextForSocket(SocketChannel socket) throws IOException
			{
				return EntityAuthenticationServerFirehoseClient.this.sslContext;
			}
		});
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
			sendMessage(addCommand, future, null);
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
			RemoteException, IOException, CertificateParsingException, AuthenticationFailureException, ServerNotRegisteredException
	{
		AuthenticateServerMessage authenticateCommand = new AuthenticateServerMessage(serverID, encodedCertReq);
		ComboFutureBase<AuthenticateServerReplyMessage>future = new ComboFutureBase<AuthenticateServerReplyMessage>();

		try
		{
			sendMessage(authenticateCommand, future, null);
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
			catch (AuthenticationFailureException e)
			{
				throw e;
			}
			catch (ServerNotRegisteredException e)
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
		GetServerCertificateMessage getEntityIDMessage = new GetServerCertificateMessage();
		ComboFutureBase<GetServerCertificateReplyMessage>future = new ComboFutureBase<GetServerCertificateReplyMessage>();

		try
		{
			sendMessage(getEntityIDMessage, future, null);
			GetServerCertificateReplyMessage replyMessage = future.get();
			return replyMessage.getServerCertificate();
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
		} catch (CertificateException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new RemoteException();
		} 
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
		if (cachedEntityID == null)
		{
			GetEntityIDMessage getEntityIDMessage = new GetEntityIDMessage();
			ComboFutureBase<GetEntityIDReplyMessage>future = new ComboFutureBase<GetEntityIDReplyMessage>();

			try
			{
				sendMessage(getEntityIDMessage, future, null);
				GetEntityIDReplyMessage replyMessage = future.get();
				if (replyMessage != null)
					cachedEntityID = replyMessage.getEntityID();
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
		return cachedEntityID;
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
		case kGetServerCertificate:
			return GetServerCertificateReplyMessage.class;
		default:
			break;
		}
		return null;
	}

	public String toString()
	{
		try
		{
			return getClass().getName()+" entityID = "+getEntityID()+" address = "+getServerAddress();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		return  getClass().getName() + "<Error getting info>";
	}
	
	private static final int kServerNotRegisteredException = kExtendedErrorStart + 1;
	private static final int kAuthenticationFailureException = kServerNotRegisteredException + 1;
	private static final int kCertificateEncodingException = kAuthenticationFailureException + 1;
	private static final int kInvalidKeyException = kCertificateEncodingException + 1;
	private static final int kIllegalStateException = kInvalidKeyException + 1;
	private static final int kNoSuchProviderException = kIllegalStateException + 1;
	private static final int kNoSuchAlgorithmException = kNoSuchProviderException + 1;
	private static final int kSignatureException = kNoSuchAlgorithmException + 1;
	private static final int kUnrecoverableKeyException = kSignatureException + 1;
	private static final int kKeyStoreException = kUnrecoverableKeyException + 1;
	private static final int kCertificateParsingException = kKeyStoreException + 1;
	private static final int kCertificateExpiredException = kCertificateParsingException + 1;
	private static final int kCertificateNotYetValidException = kCertificateExpiredException + 1;
	@Override
	public int getExtendedErrorCodeForThrowable(Throwable t)
	{
		return EntityAuthenticationServerFirehoseClient.getExtendedErrorCodeForThrowableStatic(t);
	}

	@Override
	public Throwable getExtendedThrowableForErrorCode(int errorCode)
	{
		return EntityAuthenticationServerFirehoseClient.getExtendedThrowableForErrorCodeStatic(errorCode);
	}
	
	public static int getExtendedErrorCodeForThrowableStatic(Throwable t)
	{
		Class<? extends Throwable> throwableClass = t.getClass();
		if (throwableClass.equals(ServerNotRegisteredException.class))
			return kServerNotRegisteredException;
		if (throwableClass.equals(AuthenticationFailureException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(CertificateEncodingException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(InvalidKeyException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(IllegalStateException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(NoSuchProviderException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(NoSuchAlgorithmException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(SignatureException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(UnrecoverableKeyException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(KeyStoreException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(CertificateParsingException.class))
			return kAuthenticationFailureException;
		if (throwableClass.equals(CertificateExpiredException.class))
			return kCertificateExpiredException;
		if (throwableClass.equals(CertificateNotYetValidException.class))
			return kCertificateNotYetValidException;
		return -1;
	}
	
	public static Throwable getExtendedThrowableForErrorCodeStatic(int errorCode)
	{
		switch(errorCode)
		{
		case kServerNotRegisteredException:
			return new ServerNotRegisteredException();
		case kAuthenticationFailureException:
			return new AuthenticationFailureException();
		case kCertificateEncodingException:
			return new CertificateEncodingException();
		case kInvalidKeyException:
			return new InvalidKeyException();
		case kIllegalStateException:
			return new IllegalStateException();
		case kNoSuchProviderException:
			return new NoSuchProviderException();
		case kNoSuchAlgorithmException:
			return new NoSuchAlgorithmException();
		case kSignatureException:
			return new SignatureException();
		case kUnrecoverableKeyException:
			return new UnrecoverableKeyException();
		case kKeyStoreException:
			return new KeyStoreException();
		case kCertificateParsingException:
			return new CertificateParsingException();
		case kCertificateExpiredException:
			return new CertificateExpiredException();
		case kCertificateNotYetValidException:
			return new CertificateNotYetValidException();
		default:
			return null;
		}
	}
}
