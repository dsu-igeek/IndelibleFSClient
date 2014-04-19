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
 
package com.igeekinc.indelible.indeliblefs.proxies;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.core.IndelibleFSTransaction;
import com.igeekinc.indelible.indeliblefs.core.IndelibleVersion;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.server.RemoteCASServerConnectionProxy;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionConnection;
import com.igeekinc.indelible.indeliblefs.uniblock.CASCollectionEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServer;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.uniblock.CASServerEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.TransactionCommittedEvent;
import com.igeekinc.indelible.indeliblefs.uniblock.exceptions.CollectionNotFoundException;
import com.igeekinc.indelible.indeliblefs.uniblock.remote.RemoteCASServer;
import com.igeekinc.indelible.oid.CASCollectionID;
import com.igeekinc.indelible.oid.EntityID;
import com.igeekinc.indelible.oid.ObjectID;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.ErrorLogMessage;

public class RemoteCASServerProxy implements CASServer
{
	private RemoteCASServer remoteServer;
	
	public RemoteCASServerProxy(RemoteCASServer remoteServer)
	{
		this.remoteServer = remoteServer;
	}

	@Override
	public CASCollectionConnection getCollection(
			CASServerConnectionIF connection, CASCollectionID id)
			throws CollectionNotFoundException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASCollectionConnection createNewCollection(
			CASServerConnectionIF connection)
	{
		throw new UnsupportedOperationException();	// Only accessible through the connection
	}

	
	@Override
	public CASCollectionConnection addCollection(
			CASServerConnectionIF casServerConnection,
			CASCollectionID addCollectionID)
	{
		throw new UnsupportedOperationException();	// Only accessible through the connection
	}

	@Override
	public EntityID getServerID()
	{
		try
		{
			return remoteServer.getServerID();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public void setSecurityServerID(EntityID securityServerID)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityID getSecurityServerID()
	{
		try
		{
			return remoteServer.getSecurityServerID();
		} catch (RemoteException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Caught remote exception");
		}
	}

	@Override
	public CASCollectionID[] listCollections(CASServerConnectionIF connection)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public DataDescriptor retrieveMetaData(CASServerConnectionIF connection,
			String name) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void storeMetaData(CASServerConnectionIF connection, String name,
			DataDescriptor metaData) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ObjectIDFactory getOIDFactory()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void logCASCollectionEvent(CASServerConnectionIF casServerConnection,
			ObjectID source, CASCollectionEvent event, IndelibleFSTransaction transaction)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleVersion startTransaction(CASServerConnectionIF connection)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleFSTransaction commit(CASServerConnectionIF connection)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void rollback(CASServerConnectionIF connection)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndelibleVersion startReplicatedTransaction(
			CASServerConnectionIF casServerConnection,
			TransactionCommittedEvent transactionEvent)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASCollectionEvent[] getCollectionEventsAfterEventID(
			CASServerConnectionIF serverConnection, CASCollectionID id,
			long lastEventID, int maxToReturn) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASCollectionEvent[] getCollectionEventsAfterTimestamp(
			CASServerConnectionIF connection, CASCollectionID collectionID,
			long timestamp, int numToReturn) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASCollectionEvent[] getCollectionEventsAfterEventIDForTransaction(
			CASServerConnectionIF connection, CASCollectionID collectionID,
			long startingEventID, int numToReturn,
			IndelibleFSTransaction transaction) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASServerEvent[] getServerEventsAfterEventID(
			CASServerConnectionIF serverConnection, long lastEventID,
			int maxToReturn) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASServerEvent[] getServerEventsAfterTimestamp(
			CASServerConnectionIF connection, long timestamp, int numToReturn)
			throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CASServerEvent[] getServerEventsAfterEventIDForTransaction(
			CASServerConnectionIF connection, CASCollectionID collectionID,
			long startingEventID, int numToReturn,
			IndelibleFSTransaction transaction) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void logServerEvent(CASServerConnectionIF casServerConnection,
			ObjectID source, CASServerEvent serverEvent,
			IndelibleFSTransaction transaction)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void logReplicatedServerEvent(
			CASServerConnectionIF casServerConnection, ObjectID source,
			CASServerEvent serverEvent, IndelibleFSTransaction transaction)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void logReplicatedCASCollectionEvent(
			CASServerConnectionIF casServerConnection, ObjectID source,
			CASCollectionEvent casEvent, IndelibleFSTransaction transaction)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void addConnectedServer(CASServerConnectionIF casServerConnection,
			EntityID serverID, EntityID securityServerID)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLastServerEventID(CASServerConnectionIF casServerConnection)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public long getVersionIDForVersion(CASServerConnectionIF connection,
			IndelibleVersion version)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void loadTime(CASServerConnectionIF connection,
			IndelibleVersion version)
	{
		throw new UnsupportedOperationException();
	}
}
