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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.msgpack.annotation.Message;

import com.igeekinc.indelible.indeliblefs.datamover.NetworkDataDescriptor;
import com.igeekinc.indelible.indeliblefs.security.remote.msgpack.EntityIDMsgPack;
import com.igeekinc.indelible.indeliblefs.uniblock.msgpack.CASIdentifierMsgPack;
import com.igeekinc.util.EthernetID;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

@Message
public class NetworkDataDescriptorMsgPack
{
	public NetworkDataDescriptorIDMsgPack id;
	public long dataLength;
	public byte [] sourceID;
	public InetSocketAddressMsgPack [] hostPorts;
	public EntityIDMsgPack serverID, securityServerID;
	public DataMoverSessionIDMsgPack sessionID;
	public CASIdentifierMsgPack casIdentifier;
	public byte [] shareableDescriptorData; // TODO - Serialize this properly!!
	public String localSocketStr;
	public NetworkDataDescriptorMsgPack()
	{
		// for message pack
	}

	public NetworkDataDescriptorMsgPack(NetworkDataDescriptor ndd)
	{
		id = new NetworkDataDescriptorIDMsgPack(ndd.getID());
		dataLength = ndd.getDataLength();
		sourceID = ndd.getSourceID().getBytes();
		InetSocketAddress [] sourceHostPorts = ndd.getHostPorts();
		if (sourceHostPorts != null)
		{
			hostPorts = new InetSocketAddressMsgPack[sourceHostPorts.length];
			for (int portNum = 0; portNum < hostPorts.length; portNum++)
			{
				hostPorts[portNum] = new InetSocketAddressMsgPack(sourceHostPorts[portNum]);
			}
		}
		serverID = new EntityIDMsgPack(ndd.getServerID());
		securityServerID = new EntityIDMsgPack(ndd.getSecurityServerID());
		sessionID = new DataMoverSessionIDMsgPack(ndd.getSessionID());
		casIdentifier= new CASIdentifierMsgPack(ndd.getCASIdentifier());
		
		DataDescriptor shareableDescriptor = ndd.getShareableDescriptor();
		if (shareableDescriptor != null)
		{
			try
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(shareableDescriptor);
				oos.close();
				shareableDescriptorData = baos.toByteArray();
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new InternalError("Could not serialize shareable descriptor");
			}
		}
		else
		{
			shareableDescriptorData = new byte[0];
		}
		/*
		shareableDescriptorData = new byte[] {1,2,3};*/
		if (ndd.getLocalSocket() != null)
			localSocketStr = ndd.getLocalSocket().toString();
	}
	
	public NetworkDataDescriptor getNetworkDataDescriptor()
	{
		InetSocketAddress [] returnHostPorts;
		if (hostPorts != null)
		{
			returnHostPorts = new InetSocketAddress[hostPorts.length];
			for (int portNum = 0; portNum < hostPorts.length; portNum++)
			{
				returnHostPorts[portNum] = hostPorts[portNum].getInetSocketAddress();
			}
		}
		else
		{
			returnHostPorts = new InetSocketAddress[0];
		}
		File localSocketFile = null;
		if (localSocketStr != null)
			localSocketFile = new File(localSocketStr);
		DataDescriptor shareableDescriptor = null;
		if (shareableDescriptorData != null && shareableDescriptorData.length > 0)
		{
			
			try
			{
				ByteArrayInputStream bais = new ByteArrayInputStream(shareableDescriptorData);
				ObjectInputStream ois = new ObjectInputStream(bais);
				shareableDescriptor = (DataDescriptor)ois.readObject();
				ois.close();
			} catch (ClassNotFoundException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				throw new InternalError("Got class not found exception when deserializing shareable descriptor");
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
		NetworkDataDescriptor returnDescriptor = new NetworkDataDescriptor(serverID.getEntityID(), securityServerID.getEntityID(),
				sessionID.getDataMoverSessionID(), id.getDataDescriptorID(), casIdentifier.getCASIdentifier(), dataLength, 
				new EthernetID(sourceID),
				returnHostPorts, localSocketFile, shareableDescriptor, false);
		return returnDescriptor;
	}
	
	public String toString()
	{
		return "NetworkDataDescriptorMsgPack: "+getNetworkDataDescriptor().toString();
	}
}
