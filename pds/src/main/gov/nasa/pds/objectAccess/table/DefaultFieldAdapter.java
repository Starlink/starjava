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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Implements an adapter object for table fields that can
 * read the field value from a byte array or write the
 * field value into a byte array. Also has methods for
 * formatting the value into an output writer for either
 * delimited or fixed-width output. Methods not appropriate
 * for the field type will throw {@link java.lang.UnsupportedOperationException}.
 */
public class DefaultFieldAdapter implements FieldAdapter {

	private static final String NOT_SUPPORTED = "Operation not supported";

	private static final Charset US_ASCII;
	static {
		US_ASCII = Charset.forName("US-ASCII");
	}

	@Override
	public byte getByte(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public short getShort(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public int getInt(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public long getLong(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public float getFloat(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public double getDouble(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public String getString(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return new String(buf, offset, length, US_ASCII);
	}

	@Override
	public String getString(byte[] buf, int offset, int length, int startBit, int stopBit, Charset charset) {
		return new String(buf, offset, length, charset);
	}

	@Override
	public void setByte(byte value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setShort(short value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setInt(int value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setLong(long value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setFloat(float value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setDouble(double value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setString(String value, int offset, int length, ByteBuffer buffer, boolean isRightJustified, Charset charset) {
		if (value.length() > length) {
			throw new IllegalArgumentException("The size of the value is greater than the field length.");
		}
		((Buffer) buffer).position(offset);
		buffer.put(getJustifiedValue(value, length, isRightJustified, charset), 0, length);
	}

	@Override
	public void setString(String value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		this.setString(value, offset, length, buffer, isRightJustified, Charset.forName("US-ASCII"));
	}

	public void setString(String value, ByteBuffer buffer, Charset charset) {
		buffer.put(value.getBytes(charset));
	}

	private byte[] getJustifiedValue(String value, int fieldLen, boolean isRightJustified, Charset charset) {
		// Add padding for left/right justification
		StringBuffer sb = new StringBuffer();
		int padding = fieldLen - value.length();

		if (isRightJustified) {
			for (int i = 0; i < padding; i++) {
				sb.append(' ');
			}
		}
		sb.append(value);
		if (!isRightJustified) {
			for (int i = 0; i < padding; i++) {
				sb.append(' ');
			}
		}

		return sb.toString().getBytes(charset);
	}

	@Override
	public BigInteger getBigInteger(byte[] buf, int offset, int length,
			int startBit, int stopBit) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void setBigInteger(BigInteger value, int offset, int length,
			ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}
}
