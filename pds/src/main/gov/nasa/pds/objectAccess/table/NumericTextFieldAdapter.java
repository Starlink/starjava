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

/**
 * Implements a field adapter for numeric fields stored in textual format, as in a character table.
 */
public class NumericTextFieldAdapter extends DefaultFieldAdapter {

  final public int radix;
  public NumericTextFieldAdapter(int radix) {
		super();
		this.radix = radix;
	}

@Override
  public byte getByte(byte[] buf, int offset, int length, int startBit, int stopBit) {
    int value = this.getBigInteger(buf, offset, length, startBit, stopBit).intValue();
    if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
      throw new NumberFormatException("Value is out of range of a byte (" + value + ")");
    }

    return (byte) value;
  }

  @Override
  public short getShort(byte[] buf, int offset, int length, int startBit, int stopBit) {
    int value = this.getBigInteger(buf, offset, length, startBit, stopBit).intValue();
    if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
      throw new NumberFormatException("Value is out of range of a short (" + value + ")");
    }

    return (short) value;
  }

  @Override
  public int getInt(byte[] buf, int offset, int length, int startBit, int stopBit) {
    return this.getBigInteger(buf, offset, length, startBit, stopBit).intValue();
  }

  @Override
  public long getLong(byte[] buf, int offset, int length, int startBit, int stopBit) {
    return this.getBigInteger(buf, offset, length, startBit, stopBit).longValue();
  }

  @Override
  public float getFloat(byte[] buf, int offset, int length, int startBit, int stopBit) {
    return Float.parseFloat(getString(buf, offset, length, startBit, stopBit).trim());
  }

  @Override
  public double getDouble(byte[] buf, int offset, int length, int startBit, int stopBit) {
    return Double.parseDouble(getString(buf, offset, length, startBit, stopBit).trim());
  }

  @Override
  public BigInteger getBigInteger(byte[] buf, int offset, int length, int startBit, int stopBit) {
    return new BigInteger(getString(buf, offset, length, startBit, stopBit).trim(), this.radix);
  }

  //
  // TODO: need to pass in charset to setString() in all setter?
  //

  @Override
  public void setByte(byte value, int offset, int length, ByteBuffer buffer,
      boolean isRightJustified) {
    setString(Integer.toString(value, this.radix), offset, length, buffer, isRightJustified);
  }

  @Override
  public void setShort(short value, int offset, int length, ByteBuffer buffer,
      boolean isRightJustified) {
    setString(Integer.toString(value, this.radix), offset, length, buffer, isRightJustified);
  }

  @Override
  public void setInt(int value, int offset, int length, ByteBuffer buffer,
      boolean isRightJustified) {
    setString(Integer.toString(value, this.radix), offset, length, buffer, isRightJustified);
  }

  @Override
  public void setLong(long value, int offset, int length, ByteBuffer buffer,
      boolean isRightJustified) {
    setString(Long.toString(value, this.radix), offset, length, buffer, isRightJustified);
  }

  @Override
  public void setFloat(float value, int offset, int length, ByteBuffer buffer,
      boolean isRightJustified) {
    setString(Float.toString(value), offset, length, buffer, isRightJustified);
  }

  @Override
  public void setDouble(double value, int offset, int length, ByteBuffer buffer,
      boolean isRightJustified) {
    setString(Double.toString(value), offset, length, buffer, isRightJustified);
  }

  @Override
  public void setBigInteger(BigInteger value, int offset, int length, ByteBuffer buffer,
      boolean isRightJustified) {
    setString(value.toString(this.radix), offset, length, buffer, isRightJustified);
  }

}
