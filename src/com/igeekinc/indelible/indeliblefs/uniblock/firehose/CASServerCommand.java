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
package com.igeekinc.indelible.indeliblefs.uniblock.firehose;

public enum CASServerCommand
{
	kIllegalCommand(0),
	kOpenConnection(1),
	kClose(2),
	kOpenCollectionConnection(3),
	kCreateNewCollection(4),
	kListCollections(5),
	kGetMetaData(6),
	kSetMetaData(7),
	kStartTransaction(8),
	kCommit(9),
	kRollback(10),
	kGetMoverID(11),
	kAddCollection(12),
	kAddConnectedServer(13),
	kAddClientSessionAuthentication(14),
	kGetSessionAuthentication(15),
	kGetServerEventsAfterEventIDIterator(16),
	kGetLastEventID(17),
	kGetLastReplicatedEventID(18),
	kGetEventsAfterIDIterator(19),
	kGetEventsAfterTimeIterator(20),
	kGetMoverAddresses(21),
	kTestReverseConnection(22),
	kGetTestDescriptor(23),
	kSetupReverseMoverConnection(24),
	kGetCollectionLastEventID(25),
	kGetCollectionLastReplicatedEventID(26),
	kGetSecurityServerID(27), 
	kNextCASServerEventListItems(28),
	kGetCollectionEventsAfterIDIterator(29),
	kSetMetaDataResource(30),
	kGetCollectionEventsAfterTimeIterator(31),
	kRetrieveSegmentByCASIdentifier(32),
	kRetrieveSegmentByObjectID(33),
	kStoreSegment(34),
	kStoreVersionedSegment(35),
	kStoreReplicatedSegment(36),
	kVerifySegment(37),
	kReplicateMetaDataResource(38),
	kRepairSegment(39),
	kRetrieveSegmentByObjectIDAndVersion(40),
	kReleaseSegment(41),
	kBulkReleaseSegment(42),
	kRetrieveSegmentInfo(43),
	kStartCollectionReplicatedTransaction(44),
	kStartCollectionTransaction(45),
	kCommitCollectionTransaction(46),
	kRollbackCollectionTransaction(47),
	kListMetaDataNames(48), 
	kReleaseVersionedSegment(49),
	kRetrieveCASIdentifier(50), 
	kGetMetaDataForReplication(51), 
	kNextVersionListItems(52),
	kGetVersionsForSegmentIterator(53),
	kGetVersionsForSegmentInRangeIterator(54),
	kGetEventsForTransactionIterator(55),
	kGetTransactionEventsAfterEventIDIterator(56),
	kGetMetaDataResource(57),
	kNextCASCollectionEventListItems(58),
	kStartListeningForCollection(59),
	kPollForCollectionEvents(60),
	kGetServerID(61);
	
	static CASServerCommand [] commandArray;
    static {
    	commandArray = new CASServerCommand[CASServerCommand.values().length];
    	for (CASServerCommand addCommand:CASServerCommand.values())
    	{
    		if (addCommand.commandNum > commandArray.length)
    			throw new InternalError("Command num > # of commands");
    		if (commandArray[addCommand.commandNum] != null)
    			throw new InternalError(addCommand.commandNum+" is double-booked");
    		commandArray[addCommand.commandNum] = addCommand;
    	}
    }
    
	int commandNum;
	private CASServerCommand(int commandNum)
	{
		this.commandNum = commandNum;
	}

	public int getCommandNum()
	{
		return commandNum;
	}
	
	public static CASServerCommand getCommandForNum(int num)
	{
		if (num >= commandArray.length)
			throw new IllegalArgumentException(num+" > max command number ("+(commandArray.length - 1)+")");
		if (num < 0)
			throw new IllegalArgumentException(num+" cannot be negative");
		return commandArray[num];
	}
}
