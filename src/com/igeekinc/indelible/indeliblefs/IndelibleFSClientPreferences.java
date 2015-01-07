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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.igeekinc.indelible.PreferencesManager;
import com.igeekinc.util.CheckCorrectDispatchThread;
import com.igeekinc.util.MonitoredProperties;
import com.igeekinc.util.SystemInfo;

public class IndelibleFSClientPreferences extends PreferencesManager
{
	public static final String kPropertiesFileName = "indeliblefs.client.properties"; //$NON-NLS-1$
    public static final String kPreferencesDirName = "com.igeekinc.indelible";

    public static void initPreferences(CheckCorrectDispatchThread dispatcher) throws IOException
    {
    	new IndelibleFSClientPreferences(dispatcher);	// This will automatically hook itself to the singleton
    }
    
	protected void initPreferencesInternal(CheckCorrectDispatchThread dispatcher) throws IOException
	{
		File preferencesDir = getPreferencesDir();
		Properties defaults = new Properties();
		properties = new MonitoredProperties(defaults, dispatcher);
		setIfNotSet(kPreferencesDirPropertyName, preferencesDir.getAbsolutePath()); //$NON-NLS-1$
		File propertiesFile = getPreferencesFile(); //$NON-NLS-1$
		if (propertiesFile.exists())
		{
			FileInputStream propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);
			propertiesInputStream.close();
		}

	}
	
    public IndelibleFSClientPreferences(CheckCorrectDispatchThread dispatcher) throws IOException
    {
    	super(dispatcher);
    }
	
    public File getPreferencesFileInternal()
	{
		File preferencesDir = getPreferencesDir();
		return new File(preferencesDir, kPropertiesFileName);
	}

    public File getPreferencesDirInternal()
	{
		return new File(SystemInfo.getSystemInfo().getGlobalPreferencesDirectory(), kPreferencesDirName);
	}

	@Override
	protected File getUserPreferencesDirInternal()
	{
		return new File(SystemInfo.getSystemInfo().getUserPreferencesDirectory(), kPreferencesDirName);
	}
}
