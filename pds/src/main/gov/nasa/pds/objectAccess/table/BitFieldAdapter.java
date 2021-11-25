// Copyright 2019, California Institute of Technology ("Caltech").
// U.S. Government sponsorship acknowledged.
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
// * Redistributions must reproduce the above copyright notice, this list of
// conditions and the following disclaimer in the documentation and/or other
// materials provided with the distribution.
// * Neither the name of Caltech nor its operating division, the Jet Propulsion
// Laboratory, nor the names of its contributors may be used to endorse or
// promote products derived from this software without specific prior written
// permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package gov.nasa.pds.objectAccess.table;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Implements a field adapter for binary bit fields.
 */
public class BitFieldAdapter implements FieldAdapter {

	private static final String NOT_SUPPORTED = "Operation not supported yet.";

	/** A long constant that has all bits on. */
	private static final long LONG_ALL_BITS_ONE = 0xFFFFFFFFFFFFFFFFL;

	private boolean isSigned;

	/**
	 * Creates a new bit field adapter with given signed-ness.
	 *
	 * @param isSigned true, if the bit field is signed
	 */
	public BitFieldAdapter(boolean isSigned) {
		this.isSigned = isSigned;
	}

	@Override
	public String getString(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return Long.toString(getFieldValue(buf, offset, length, startBit, stopBit));
	}

	@Override
	public String getString(byte[] buf, int offset, int length, int startBit,
			int stopBit, Charset charset) {
		return Long.toString(getFieldValue(buf, offset, length, startBit, stopBit));
	}

	@Override
	public byte getByte(byte[] buf, int offset, int length, int startBit, int stopBit) {
		long value = getFieldValue(buf, offset, length, startBit, stopBit);
		if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
			String msg = "Binary integer value out of range for byte (" + value + ")";
			throw new NumberFormatException();
		}

		return (byte) value;
	}

	@Override
	public short getShort(byte[] buf, int offset, int length, int startBit, int stopBit) {
		long value = getFieldValue(buf, offset, length, startBit, stopBit);
		if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
			String msg = "Binary integer value out of range for short (" + value + ")";
			throw new NumberFormatException(msg);
		}

		return (short) value;
	}

	@Override
	public int getInt(byte[] buf, int offset, int length, int startBit, int stopBit) {
		long value = getFieldValue(buf, offset, length, startBit, stopBit);
		if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
			String msg = "Binary integer value out of range for int (" + value + ")";
			throw new NumberFormatException(msg);
		}

		return (int) value;
	}

	@Override
	public long getLong(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return getFieldValue(buf, offset, length, startBit, stopBit);
	}

	@Override
	public float getFloat(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return getFieldValue(buf, offset, length, startBit, stopBit);
	}

	@Override
	public double getDouble(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return getFieldValue(buf, offset, length, startBit, stopBit);
	}

	@Override
	public void setString(String value, int offset, int length, ByteBuffer buf, boolean isRightJustifed) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setString(String value, int offset, int length, ByteBuffer buffer, boolean isRightJustifed, Charset charset) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setInt(int value, int offset, int length, ByteBuffer buffer, boolean isRightJustifed) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setDouble(double value, int offset, int length, ByteBuffer buffer, boolean isRightJustifed) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setFloat(float value, int offset, int length, ByteBuffer buffer, boolean isRightJustifed) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setShort(short value, int offset, int length, ByteBuffer buffer, boolean isRightJustifed) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setByte(byte value, int offset, int length, ByteBuffer buffer, boolean isRightJustifed) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setLong(long value, int offset, int length, ByteBuffer buffer, boolean isRightJustifed) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	private long getFieldValue(byte[] b, int offset, int length, int startBit, int stopBit) {
		if (startBit < 0) {
			String msg = "Start bit is negative (" + startBit + ")";
			throw new ArrayIndexOutOfBoundsException(msg);
		}
		if (stopBit >= length * Byte.SIZE) {
			String msg = "Stop bit past end of packed field (" + stopBit + " > " + (length * Byte.SIZE - 1) + ")";
			throw new ArrayIndexOutOfBoundsException(msg);
		}
		if (stopBit - startBit + 1 > Long.SIZE) {
			String msg = "Bit field is wider than long (" + (stopBit-startBit+1) + " > " + Long.SIZE + ")";
			throw new IllegalArgumentException(msg);
		}

		int startByte = startBit / Byte.SIZE;
		int stopByte = stopBit / Byte.SIZE;

		if (stopByte-startByte+1 > Long.SIZE / Byte.SIZE) {
			String msg = "Bit field spans bytes that are wider than a long "
				+ "(" + (stopByte-startByte+1) + " > " + (Long.SIZE / Byte.SIZE) + ")";
			throw new NumberFormatException(msg);
		}

		long bytesValue = getBytesAsLong(b, offset, startByte, stopByte);

		// Now shift right to get rid of the extra bits.
		int extraRightBits = (stopByte + 1)*Byte.SIZE - stopBit - 1;
		long shiftedValue = bytesValue >> extraRightBits;

		return rightmostBits(shiftedValue, stopBit-startBit+1, isSigned);
	}

	// Default scope, for unit testing.
	static long rightmostBits(long value, int nBits, boolean isSigned) {
		long mask = 0;
		if (nBits > 0) {
			mask = LONG_ALL_BITS_ONE >>> (Long.SIZE - nBits);
		}
		long maskedValue = value & mask;

		// Now sign-extend, if signed.
		if (isSigned && nBits < Long.SIZE) {
			long signBit = 1L << (nBits - 1);
			if ((maskedValue & signBit) != 0) {
				maskedValue |= (LONG_ALL_BITS_ONE << nBits);
			}
		}

		return maskedValue;
	}

	static long getBytesAsLong(byte[] source, int off, int startByte, int stopByte) {
		long value = 0;

		for (int i=off+startByte; i <= off+stopByte; ++i) {
			value = (value << Byte.SIZE) | (source[i] & 0xFF);
		}

		return value;
	}

	@Override
	public BigInteger getBigInteger(byte[] buf, int offset, int length,
			int startBit, int stopBit) {
		String stringValue = Long.toString(getLong(buf, offset, length, startBit, stopBit));
		return new BigInteger(stringValue);
	}

	@Override
	public void setBigInteger(BigInteger value, int offset, int length,
			ByteBuffer buffer, boolean isRightJustified) {
		String stringValue = value.toString();
		setLong(Long.parseLong(stringValue), offset, length, buffer, isRightJustified);
	}

}
