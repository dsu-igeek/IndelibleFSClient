/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igeekinc.indelible.indeliblefs.datamover.msgpack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Message;

import com.igeekinc.util.logging.ErrorLogMessage;

@Message
public class InetSocketAddressMsgPack
{
	static Field hostNameField;
	static Method getHostStringMethod;
	static
	{
		try
		{
			Class<InetSocketAddress>inetSocketAddressClass = InetSocketAddress.class;
			try
			{
				hostNameField = inetSocketAddressClass.getDeclaredField("hostname");
				hostNameField.setAccessible(true);
			} catch (NoSuchFieldException e)
			{
				//	OK, well then we can probably go ahead and use getHostString
				getHostStringMethod = inetSocketAddressClass.getDeclaredMethod("getHostString", null);
				getHostStringMethod.setAccessible(true);
			}
		} catch (Throwable t)
		{
			t.printStackTrace();
			// Must not be there
			//throw new InternalError("InetAddress not compatible");
		}
		if (hostNameField == null && getHostStringMethod == null)
			throw new InternalError("Unsupported JDK, no hostname field and no getHostString method");
	}
	public String hostString;
	public byte [] ipAddress;
	public int port;
	public InetSocketAddressMsgPack()
	{
		// for message pack
	}

	public InetSocketAddressMsgPack(InetSocketAddress address)
	{
		try
		{
			if (getHostStringMethod != null)
			{
				// getHostString is a better method because it will not trigger an inverse lookup.  However,
				// it's only available in 1.7+ and we're supporting back to 1.5 so we wind up doing a silly 
				// reflection dance.  I miss #ifdef

				try
				{
					hostString = (String)getHostStringMethod.invoke(address);
				} catch (Throwable e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
					throw new InternalError("Couldn't invoke getHostString");
				}
			}
			else
			{
				hostString = (String) hostNameField.get(address);
			}
			InetAddress inetAddress = address.getAddress();
			if (inetAddress != null)
			{
				if (inetAddress instanceof Inet6Address)
				{
					ipAddress = ((Inet6Address)inetAddress).getAddress();
				}
				else
				{
					ipAddress = ((Inet4Address)inetAddress).getAddress();
				}
			}
			port = address.getPort();
		} catch (IllegalArgumentException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Reflection not working");
		} catch (IllegalAccessException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Reflection not working");
		}
	}
	
	public InetSocketAddress getInetSocketAddress()
	{
		InetAddress inetAddress;
		try
		{
			if (ipAddress != null)
			{
				if (ipAddress.length > 4)
				{
					if (hostString != null)
						inetAddress = Inet6Address.getByAddress(hostString, ipAddress);
					else
						inetAddress = Inet6Address.getByAddress(ipAddress);
				}
				else
				{
					if (hostString != null)
						inetAddress = InetAddress.getByAddress(hostString, ipAddress);
					else
						inetAddress = InetAddress.getByAddress(ipAddress);
				}
			}
			else
			{
				inetAddress = InetAddress.getByName(hostString);
			}
		} catch (UnknownHostException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("inet address incorrect");
		}
		return new InetSocketAddress(inetAddress, port);
	}
}
