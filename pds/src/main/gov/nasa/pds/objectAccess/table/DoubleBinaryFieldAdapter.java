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
 * Implements a field adapter for binary, double-precision, floating-point fields.
 */
public class DoubleBinaryFieldAdapter implements FieldAdapter {

	FieldAdapter longAdapter;

	public DoubleBinaryFieldAdapter(boolean isBigEndian) {
		longAdapter = new IntegerBinaryFieldAdapter(Double.SIZE / Byte.SIZE, false, isBigEndian);
	}

	@Override
	public String getString(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return Double.toString(getDouble(buf, offset, length, startBit, stopBit));
	}

	@Override
	public String getString(byte[] buf, int offset, int length, int startBit, int stopBit, Charset charset) {
		return Double.toString(getDouble(buf, offset, length, startBit, stopBit));
	}

	@Override
	public byte getByte(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException("Cannot get a binary float as an integer.");
	}

	@Override
	public short getShort(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException("Cannot get a binary float as an integer.");
	}

	@Override
	public int getInt(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException("Cannot get a binary float as an integer.");
	}

	@Override
	public long getLong(byte[] buf, int offset, int length, int startBit, int stopBit) {
		throw new UnsupportedOperationException("Cannot get a binary float as an integer.");
	}

	@Override
	public float getFloat(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return (float) getDouble(buf, offset, length, startBit, stopBit);
	}

	@Override
	public double getDouble(byte[] buf, int offset, int length, int startBit, int stopBit) {
		long bits = longAdapter.getLong(buf, offset, length, 0, 0);
		return Double.longBitsToDouble(bits);
	}

	@Override
	public void setString(String value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		setDouble(Double.parseDouble(value), offset, length, buffer, isRightJustified);
	}

	@Override
	public void setString(String value, int offset, int length, ByteBuffer buffer,	boolean isRightJustified, Charset charset) {
		setDouble(Double.parseDouble(value), offset, length, buffer, isRightJustified);
	}

	@Override
	public void setByte(byte value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException("Cannot set a binary double as an integer.");
	}

	@Override
	public void setShort(short value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException("Cannot set a binary double as an integer.");
	}

	@Override
	public void setInt(int value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException("Cannot set a binary double as an integer.");
	}

	@Override
	public void setLong(long value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		throw new UnsupportedOperationException("Cannot set a binary double as an integer.");
	}

	@Override
	public void setFloat(float value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		setDouble(value, offset, length, buffer, isRightJustified);
	}

	@Override
	public void setDouble(double value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		longAdapter.setDouble(value, offset, length, buffer, isRightJustified);
	}

	@Override
	public BigInteger getBigInteger(byte[] buf, int offset, int length,
			int startBit, int stopBit) {
		String stringValue = Double.toString(getDouble(buf, offset, length, startBit, stopBit));
		return new BigInteger(stringValue);
	}

	@Override
	public void setBigInteger(BigInteger value, int offset, int length,
			ByteBuffer buffer, boolean isRightJustified) {
		String stringValue = value.toString();
		setDouble(Double.parseDouble(stringValue), offset, length, buffer, isRightJustified);
	}

}
