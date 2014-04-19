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
 
package com.igeekinc.indelible.indeliblefs.uniblock;

import java.io.Serializable;
import java.math.BigInteger;

import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.SHA1HashID;

public class CASIdentifier implements Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8765930693502825631L;
	private SHA1HashID hashID;
    private long segmentLength;
    private transient String stringVersion;
    
    public static final int kCASIdentifierSize = SHA1HashID.kSHA1ByteLength + 8;	// sha1 size + long (8)
    /**
     * 
     */
    public CASIdentifier(SHA1HashID inHashID, long inSegmentLength)
    {
        hashID = inHashID;
        segmentLength = inSegmentLength;
        stringVersion = null;
    }
    
    public CASIdentifier(String stringCASID)
    {
        String hashStr = stringCASID.substring(0, SHA1HashID.kSHA1ByteLength * 2);
        String lengthStr = stringCASID.substring(SHA1HashID.kSHA1ByteLength * 2, SHA1HashID.kSHA1ByteLength * 2 + 8);
        hashID = new SHA1HashID(hashStr);
        segmentLength = Long.parseLong(lengthStr, 16);
    }

    public CASIdentifier(byte [] casIDBytes)
    {
    	if (casIDBytes.length != kCASIdentifierSize)
    		throw new IllegalArgumentException(casIDBytes.length+" != "+kCASIdentifierSize);
    	init(casIDBytes, 0);
    }
    
    public CASIdentifier(byte [] buffer, int offset)
    {
    	if (buffer.length - offset < kCASIdentifierSize)
    		throw new IllegalArgumentException("buffer not large enough");
    	init(buffer, offset);
    }
    
    private void init(byte [] buffer, int offset)
    {
    	hashID = new SHA1HashID(buffer, offset);
    	offset += SHA1HashID.kSHA1ByteLength;
    	segmentLength = BitTwiddle.byteArrayToLong(buffer, offset, BitTwiddle.kLittleEndian);
    }
    public long getSegmentLength()
    {
        return segmentLength;
    }
    public SHA1HashID getHashID()
    {
        return hashID;
    }
    /*
    public boolean equals(Object obj)
    {
        if (obj instanceof CASIdentifier)
            return equals((CASIdentifier)obj);
        return false;
    }
    
    public boolean equals(CASIdentifier id)
    {
        
        if (segmentLength == id.segmentLength && hashID.equals(id.hashID))
            return true;
        return false;
    }
    */
    
    
    @Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hashID == null) ? 0 : hashID.hashCode());
		result = prime * result
				+ (int) (segmentLength ^ (segmentLength >>> 32));
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
		CASIdentifier other = (CASIdentifier) obj;
		if (hashID == null)
		{
			if (other.hashID != null)
				return false;
		} else if (!hashID.equals(other.hashID))
			return false;
		if (segmentLength != other.segmentLength)
			return false;
		return true;
	}

	public String toString()
    {
        if (stringVersion == null)
        {
            StringBuffer returnStringBuffer = new StringBuffer(128+4);
            returnStringBuffer.append(hashID.toString());
            returnStringBuffer.append(BitTwiddle.toHexString(segmentLength, 8));
            stringVersion = returnStringBuffer.toString();
        }
        return stringVersion;
    }
    
    public String toString(int radix)
    {
        return hashID.toString(radix);
    }
    
    public byte [] getBytes()
    {
    	byte [] returnBytes = new byte[kCASIdentifierSize];
    	getBytes(returnBytes, 0);
    	return returnBytes;
    }
    
    public void getBytes(byte [] buffer, int offset)
    {
    	hashID.getBytes(buffer, offset);
    	offset += SHA1HashID.kSHA1ByteLength;
    	BitTwiddle.longToByteArray(segmentLength, buffer, offset, BitTwiddle.kLittleEndian);
    }
    
    public BigInteger getBigInteger()
    {
    	return new BigInteger(getBytes());
    }
}
