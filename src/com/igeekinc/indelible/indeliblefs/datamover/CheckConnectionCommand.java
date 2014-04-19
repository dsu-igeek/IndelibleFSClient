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
 
package com.igeekinc.indelible.indeliblefs.datamover;

import java.io.IOException;
import java.io.InputStream;

public class CheckConnectionCommand extends DataMoverCommand
{
	public static final int kCommandSize = kCommandHeaderSize;
	
	public CheckConnectionCommand()
	{
		super(DataMoverSource.kCheckConnection);
		commandBuf = new byte[kCommandSize];
        writeCommandHeader();
	}
	
	@Override
	public int expectedOKBytes()
	{
		return 8;
	}

	@Override
	public void processOKResultInternal(byte[] okBytes, InputStream inputStream)
			throws IOException
	{
		// As long as we get a reply we're OK
	}

}
