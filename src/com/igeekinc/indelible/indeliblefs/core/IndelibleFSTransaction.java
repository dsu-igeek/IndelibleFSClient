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
 
package com.igeekinc.indelible.indeliblefs.core;

import java.io.Serializable;

import com.igeekinc.indelible.oid.EntityID;

public class IndelibleFSTransaction implements Serializable
{
    private static final long serialVersionUID = -6471806510454713949L;
    private long transactionID;    
    private EntityID transactionMaster;
    private IndelibleVersion version;	// The version assigned to the transaction
    public IndelibleFSTransaction(EntityID transactionMaster, long transactionID)
    {
        this.transactionMaster = transactionMaster;
        this.transactionID = transactionID;
    }

    public EntityID getTransactionMaster()
    {
    	return transactionMaster;
    }
    
    public long getTransactionID()
    {
        return transactionID;
    }

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (transactionID ^ (transactionID >>> 32));
		result = prime
				* result
				+ ((transactionMaster == null) ? 0 : transactionMaster
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndelibleFSTransaction other = (IndelibleFSTransaction) obj;
		if (transactionID != other.transactionID)
			return false;
		if (transactionMaster == null)
		{
			if (other.transactionMaster != null)
				return false;
		} else if (!transactionMaster.equals(other.transactionMaster))
			return false;
		return true;
	}

	public String toString()
	{
		return "TransactionMaster="+transactionMaster+" TransactionID="+transactionID;
	}

	public void setVersion(IndelibleVersion versionForTransaction)
	{
		this.version = versionForTransaction;
	}
	
	public IndelibleVersion getVersion()
	{
		return version;
	}
}
