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
package com.igeekinc.indelible.indeliblefs.firehose.msgpack;

import org.msgpack.annotation.Message;

import com.igeekinc.util.FilePath;
import com.igeekinc.util.linux.LinuxFilePath;
import com.igeekinc.util.macos.macosx.MacOSXFilePath;
import com.igeekinc.util.windows.WindowsFilePath;

@Message
public class FilePathMsgPack
{
	public static final int kMacOSXPath = 1;
	public static final int kLinuxPath = 2;
	public static final int kWindowsPath = 3;
	
	public boolean absolute;
	public int pathType;
	public String [] pathComponents;
	
	public FilePathMsgPack()
	{
		// for message pack
	}
	
	public FilePathMsgPack(FilePath path)
	{
		absolute = path.isAbsolute();
		pathComponents = path.getComponents();
		if (path instanceof MacOSXFilePath)
		{
			pathType = kMacOSXPath;
			return;
		}
		if (path instanceof LinuxFilePath)
		{
			pathType = kLinuxPath;
			return;
		}
		if (path instanceof WindowsFilePath)
		{
			pathType = kWindowsPath;
			return;
		}
		throw new IllegalArgumentException("Unrecognized file path type "+path.getClass());
	}
	
	public FilePath getFilePath()
	{
		switch (pathType)
		{
		case kMacOSXPath:
			return new MacOSXFilePath(pathComponents, absolute);
		case kLinuxPath:
			return new LinuxFilePath(pathComponents, absolute);
		case kWindowsPath:
			return new WindowsFilePath(pathComponents, absolute);
		default:
			throw new InternalError("Unknown file path type "+pathType);
		}
	}
}
