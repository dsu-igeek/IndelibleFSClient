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
 
package com.igeekinc.indelible.indeliblefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.exceptions.ForkNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleDirectoryNodeRemote;
import com.igeekinc.indelible.indeliblefs.remote.IndelibleFSForkRemoteInputStream;
import com.igeekinc.util.ClientFileMetaData;
import com.igeekinc.util.ClientFileMetaDataProperties;
import com.igeekinc.util.FileLike;
import com.igeekinc.util.FileLikeFilenameFilter;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFileLike implements FileLike
{
	public static final String kClientFileMetaDataPropertyName = "com.igeekinc.util.ClientFileMetaData";
	private IndelibleFileNodeIF wrappedNode;
	private FilePath path;
	private HashMap<String, IndelibleNodeInfo> nodeInfoMap;
	private Future<IndelibleNodeInfo []>nodeInfoFuture;
	
	public IndelibleFileLike(FilePath path, IndelibleFileNodeIF wrappedNode) throws RemoteException, PermissionDeniedException, IOException
	{
		if (path == null)
			throw new IllegalArgumentException("path cannot be null");
		if (wrappedNode == null)
			throw new IllegalArgumentException("wrappedNode cannot be null");
		this.wrappedNode = wrappedNode;
		this.path = path;
		if (wrappedNode.isDirectory())
		{
			nodeInfoFuture = ((IndelibleDirectoryNodeIF)wrappedNode).getChildNodeInfoAsync(new String[]{kClientFileMetaDataPropertyName});
		}
	}
	
	@Override
	public FileLike getChild(String childName) throws IOException
	{
		return getChild(FilePath.getFilePath(childName));
	}

	@Override
	public FileLike getChild(FilePath childPath) throws IOException
	{
		if (wrappedNode.isDirectory())
		{
			String childName = childPath.getComponent(0);
			childPath = childPath.removeLeadingComponent();
			IndelibleFileNodeIF childNode;
			try
			{
				childNode = ((IndelibleDirectoryNodeIF)wrappedNode).getChildNode(childName);
			} catch (PermissionDeniedException e)
			{
				throw new IOException("Permission denied");
			} catch (ObjectNotFoundException e)
			{
				return null;
			}
			IndelibleFileLike child;
			try
			{
				child = new IndelibleFileLike(path.getChild(childName), childNode);
			} catch (PermissionDeniedException e)
			{
				throw new IOException("Permission denied");
			}
			if (child != null && childPath.getNumComponents() > 0)
				return child.getChild(childPath);
			else
				return child;
		}
		return null;
	}

	@Override
	public String[] list() throws IOException
	{
		if (wrappedNode.isDirectory())
		{
			try
			{
				return ((IndelibleDirectoryNodeRemote)wrappedNode).list();
			} catch (PermissionDeniedException e)
			{
				throw new IOException("Permission denied");
			}
		}
		return null;
	}

	@Override
	public String[] list(FileLikeFilenameFilter filter) throws IOException
	{
		if (wrappedNode.isDirectory())
		{
			String[] rawNames;
			try
			{
				rawNames = ((IndelibleDirectoryNodeRemote)wrappedNode).list();
			} catch (PermissionDeniedException e)
			{
				throw new IOException("Permission denied");
			}
			ArrayList<String>returnList = new ArrayList<String>(rawNames.length);
			for (String curName:rawNames)
			{
				if (filter.accept(this, curName))
					returnList.add(curName);
			}
			String [] returnNames = new String[returnList.size()];
			returnNames = returnList.toArray(returnNames);
			return returnNames;
		}
		return null;
	}

	@Override
	public String getAbsolutePath()
	{
		return path.toString();
	}

	@Override
	public String getName()
	{
		return path.getName();
	}

	@Override
	public long lastModified()
	{
		try
		{
			return wrappedNode.lastModified();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			return 0;
		}
	}

	@Override
	public boolean exists()
	{
		return true;
	}

	@Override
	public boolean isDirectory()
	{
		return wrappedNode.isDirectory();
	}

	@Override
	public boolean isFile()
	{
		return wrappedNode.isFile();
	}

	@Override
	public long length()
	{
		return totalLength();
	}

	@Override
	public long totalLength()
	{
		try
		{
			return wrappedNode.totalLength();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			return 0;
		}
	}

	@Override
	public boolean isMountPoint()
	{
		return false;
	}

	@Override
	public FilePath getFilePath()
	{
		return path;
	}

	@Override
	public FilePath getBackupPartialPath()
	{
		return path;
	}

	public ClientFileMetaData getMetaData() throws IOException
	{
		//destFile.setMetaDataResource(kClientFileMetaDataPropertyName, metaData.getProperties().getMap());
		Map<String, Object> mdMap;
		try
		{
			mdMap = wrappedNode.getMetaDataResource(kClientFileMetaDataPropertyName);
		} catch (PermissionDeniedException e)
		{
			throw new IOException("Permission denied retrieving meta data");
		}
		if (mdMap == null)
			return null;
		ClientFileMetaDataProperties mdProperties = ClientFileMetaDataProperties.getPropertiesForMap(mdMap);
		return mdProperties.getMetaData();
	}
	
	public ClientFileMetaData getMetaDataForChild(String childName) throws IOException
	{
		ClientFileMetaData returnMD = null;

		Map<String, Object> mdMap = null;
		synchronized(nodeInfoFuture)
		{
			if (nodeInfoMap == null)
			{
				IndelibleNodeInfo[] nodeInfo;
				try
				{
					nodeInfo = nodeInfoFuture.get();
				} catch (InterruptedException e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
					throw new IOException("Interrupted while retriving nodeInfo");
				} catch (ExecutionException e)
				{
					Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
					if (e.getCause() instanceof IOException)
						throw (IOException)e.getCause();
					throw new IOException("Got exception "+e.getCause().getMessage());
				}
				nodeInfoMap = new HashMap<String, IndelibleNodeInfo>();
				for (IndelibleNodeInfo curNodeInfo:nodeInfo)
				{
					nodeInfoMap.put(curNodeInfo.getName(), curNodeInfo);
				}
			}
		}
		IndelibleNodeInfo allMDForChild = nodeInfoMap.get(childName);
		if (allMDForChild != null)
			mdMap = allMDForChild.getMetaData(kClientFileMetaDataPropertyName);

		if (mdMap != null)
		{
			ClientFileMetaDataProperties mdProperties = ClientFileMetaDataProperties.getPropertiesForMap(mdMap);
			returnMD= mdProperties.getMetaData();
		}
		else
		{
			IndelibleFileLike child = (IndelibleFileLike) getChild(childName);
			if (child != null)
				returnMD = child.getMetaData();
		}
		return returnMD;
	}
	
	@Override
	public int getNumForks() throws IOException
	{
		return wrappedNode.listForkNames().length;
	}

	@Override
	public String[] getForkNames() throws IOException
	{
		return wrappedNode.listForkNames();
	}

	@Override
	public InputStream getForkInputStream(String streamName)
			throws com.igeekinc.util.exceptions.ForkNotFoundException, IOException
	{
		try
		{
			return new IndelibleFSForkRemoteInputStream(wrappedNode.getFork(streamName, false));
		} catch (ForkNotFoundException e)
		{
			throw new com.igeekinc.util.exceptions.ForkNotFoundException(streamName);
		} catch (PermissionDeniedException e)
		{
			throw new IOException("Permission denied");
		}
	}

	@Override
	public OutputStream getForkOutputStream(String streamName)
			throws com.igeekinc.util.exceptions.ForkNotFoundException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getForkInputStream(String streamName, boolean noCache)
			throws com.igeekinc.util.exceptions.ForkNotFoundException, IOException
	{
		try
		{
			return new IndelibleFSForkRemoteInputStream(wrappedNode.getFork(streamName, false));
		} catch (ForkNotFoundException e)
		{
			throw new com.igeekinc.util.exceptions.ForkNotFoundException(streamName);
		} catch (PermissionDeniedException e)
		{
			throw new IOException("Permission denied");
		}
	}

	@Override
	public OutputStream getForkOutputStream(String streamName, boolean noCache)
			throws com.igeekinc.util.exceptions.ForkNotFoundException
	{
		throw new UnsupportedOperationException();
	}
}
