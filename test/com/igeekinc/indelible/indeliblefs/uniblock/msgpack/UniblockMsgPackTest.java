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
package com.igeekinc.indelible.indeliblefs.uniblock.msgpack;

import java.lang.reflect.Constructor;

import junit.framework.TestCase;

import org.msgpack.MessagePack;

import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerCommand;
import com.igeekinc.indelible.indeliblefs.uniblock.firehose.CASServerFirehoseClient;

public class UniblockMsgPackTest extends TestCase
{
	public void testPackUnpack() throws Exception
	{
		MessagePack packer = new MessagePack();

		for (CASServerCommand checkCommand:CASServerCommand.values())
		{
			if (checkCommand != CASServerCommand.kIllegalCommand)
			{
				CASServerCommandMessage checkMessage = null;
				CASServerCommandMessage unpackMessage = null;
				@SuppressWarnings("rawtypes")
				Class<? extends CASServerCommandMessage>commandClass = CASServerFirehoseClient.getClassForCommandCode(checkCommand.getCommandNum());	// This exercises both getReturnClassForCommandCodeStatic and getClassForCommandCode
				Constructor<? extends CASServerCommandMessage>commandConstructor = commandClass.getConstructor(new Class<?>[0]);
				checkMessage = commandConstructor.newInstance();
				byte [] checkBytes = packer.write(checkMessage);
				unpackMessage = packer.read(checkBytes, commandClass);
				assertEquals(checkMessage, unpackMessage);
			}
		}
	}
	
	public void testGetMetaDataResourceMessagePackUnpack() throws Exception
	{
		MessagePack packer = new MessagePack();
		GetMetaDataResourceMessage<Void> testMessage = new GetMetaDataResourceMessage<Void>();
		testMessage.mdResourceName = "smokin'";
		byte [] checkBytes = packer.write(testMessage);
		GetMetaDataResourceMessage<Void> unpackMessage = (GetMetaDataResourceMessage<Void>)packer.read(checkBytes, GetMetaDataResourceMessage.class);
		assertEquals(testMessage, unpackMessage);
	}
}
