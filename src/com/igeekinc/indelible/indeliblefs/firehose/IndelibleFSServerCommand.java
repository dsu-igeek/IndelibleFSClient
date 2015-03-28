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
package com.igeekinc.indelible.indeliblefs.firehose;

/*
 * IndelibleFSServerCommand contains the numeric codes for all commands that can be sent to the server.
 * To add a new code, create the enumeration here (be sure not to conflict with an existing code, this list should
 * be kept in numeric order).  Then, create a new XXMessage class that return the command code enum from getInitServerCommand.
 * Then, update IndelibleFSFirehoseClient getClassForCommandCode to return the XXMessage class.  The IndelibleFSFirehoseClient
 * will need a new call that creates an XXMessage class and sends it and IndelibleFSFirehoseServer needs to implement
 * the server side of the call
 */
public enum IndelibleFSServerCommand
{
	kIllegalCommand(0),
	kOpenConnection(1),
    kCloseConnection(2),
    kStartTransaction(3),
    kInTransaction(4),
    kCommitTransaction(5),
    kCommitTransactionAndSnapshot(6),
    kRollbackTransaction(7),
    kCreateVolume(8),
    kRetrieveVolume(9),
    kListVolumes(10), 
    kAddClientSessionAuthentication(11),
    kGetSessionAuthentication(12), 
    kTestReverseConnection(13),
    kGetMoverAddresses(14), 
    kGetClientEntityAuthentication(15), 
    kGetMetaDataResource(16),
    kListMetaDataResources(17),
    kReleaseHandle(18),
    kGetRoot(19), 
    kGetObjectByPath(20),
    kGetObjectByID(21),
    kGetObjectByIDAndVersion(22),
    kCreateChildFile(23), 
    kListDirectory(24),
    kGetChildNode(25),
    kGetTotalLength(26),
    kGetFork(27),
    kAppendDataDescriptor(28),
    kWriteDataDescriptor(29),
    kGetDataDescriptor(30),
    kFlush(31),
    kExtend(32),
    kTruncate(33),
    kCreateChildFileWithInitialData(34),
    kCreateChildFileFromExistingFile(35),
    kCreateDirectory(36),
    kDeleteChildFile(37),
    kDeleteChildDirectory(38),
    kDeleteObjectByPath(39),
    kGetCurrentVersion(40), 
    kGetInfoForSnapshot(41),
    kAddSnapshot(42),
    kGetVersion(43),
    kGetLength(44), 
    kMoveObject(45),
    kListVersions(46),
    kNextVersionListItems(47),
    kCreateChildSymlink(48),
    kGetLastModified(49),
    kGetVolumeName(50),
    kGetChildNodeInfo(51),
    kSetMetaDataResource(52),
    kListSnapshots(53),
    kNextSnapshotListItems(54),
    kGetCASServerPort(55),
    kGetNumChildren(56),
    kGetServerID(57),
    kGetSegmentIDs(58),
    kGetForkName(59),
    kDeleteVolume(60);
	
	static IndelibleFSServerCommand [] commandArray;
    static {
    	commandArray = new IndelibleFSServerCommand[IndelibleFSServerCommand.values().length];
    	for (IndelibleFSServerCommand addCommand:IndelibleFSServerCommand.values())
    	{
    		if (addCommand.commandNum > commandArray.length)
    			throw new InternalError("Command num > # of commands");
    		if (commandArray[addCommand.commandNum] != null)
    			throw new InternalError(addCommand.commandNum+" is double-booked");
    		commandArray[addCommand.commandNum] = addCommand;
    	}
    }
    
	int commandNum;
	private IndelibleFSServerCommand(int commandNum)
	{
		this.commandNum = commandNum;
	}

	public int getCommandNum()
	{
		return commandNum;
	}
	
	public static IndelibleFSServerCommand getCommandForNum(int num)
	{
		if (num >= commandArray.length)
			throw new IllegalArgumentException(num+" > max command number ("+(commandArray.length - 1)+")");
		if (num < 0)
			throw new IllegalArgumentException(num+" cannot be negative");
		return commandArray[num];
	}
}