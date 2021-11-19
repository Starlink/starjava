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
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Implements a field adapter for binary integer fields.
 */
public class IntegerBinaryFieldAdapter implements FieldAdapter {

	private int dataLength;
	private boolean isSigned;
	private boolean isBigEndian;

	public IntegerBinaryFieldAdapter(int length, boolean isSigned, boolean isBigEndian) {
		this.dataLength = length;
		this.isSigned = isSigned;
		this.isBigEndian = isBigEndian;
	}

	@Override
	public String getString(byte[] buf, int offset, int length, int startBit, int stopBit) {
		if (length < Long.SIZE/Byte.SIZE) {
			return Long.toString(getFieldValue(buf, offset, length));
		} else {
			return getBigIntegerFieldValue(buf, offset, length).toString();
		}
	}

	@Override
	public String getString(byte[] buf, int offset, int length, int startBit, int stopBit, Charset charset) {
		return Long.toString(getFieldValue(buf, offset, length));
	}

	@Override
	public byte getByte(byte[] buf, int offset, int length, int startBit, int stopBit) {
		long value = getFieldValue(buf, offset, length);
		if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
			throw new NumberFormatException("Binary integer value out of range for byte (" + value + ")");
		}

		return (byte) value;
	}

	@Override
	public short getShort(byte[] buf, int offset, int length, int startBit, int stopBit) {
		long value = getFieldValue(buf, offset, length);
		if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
			throw new NumberFormatException("Binary integer value out of range for short (" + value + ")");
		}

		return (short) value;
	}

	@Override
	public int getInt(byte[] buf, int offset, int length, int startBit, int stopBit) {
		long value = getFieldValue(buf, offset, length);
		if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
			throw new NumberFormatException("Binary integer value out of range for int (" + value + ")");
		}

		return (int) value;
	}

	@Override
	public long getLong(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return getFieldValue(buf, offset, length);
	}

	@Override
	public float getFloat(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return getFieldValue(buf, offset, length);
	}

	@Override
	public double getDouble(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return getFieldValue(buf, offset, length);
	}

	@Override
	public void setString(String value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		this.setString(value, offset, length, buffer, isRightJustified, Charset.forName("US-ASCII"));
	}

	@Override
	public void setString(String value, int offset, int length, ByteBuffer buffer, boolean isRightJustified, Charset charset) {
		if (value.length() > length) {
			throw new IllegalArgumentException("The size of the value is greater than the field length.");
		}
		buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		((Buffer) buffer).position(offset);
		buffer.put(value.getBytes(charset), 0, length);
	}

	@Override
	public void setByte(byte value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		buffer.put(offset, value);
	}

	@Override
	public void setShort(short value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		buffer.putShort(offset, value);
	}

	@Override
	public void setInt(int value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(offset, value);
	}

	@Override
	public void setLong(long value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(offset, value);
	}

	@Override
	public void setFloat(float value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		buffer.putFloat(offset, value);
	}

	@Override
	public void setDouble(double value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		buffer.putDouble(offset, value);
	}

	private long getFieldValue(byte[] b, int offset, int length) {
		if (dataLength != length) {
			throw new IllegalArgumentException("Declared field length does not match data type length "
					+ "(" + length + "!=" + this.dataLength + ")");
		} else if (offset > b.length) {
		  throw new IllegalArgumentException("Field offset '" + offset
		      + "' is greater than the number of bytes in the record '" + b.length + "'");
		} else if ((offset + length) > b.length) {
		  throw new IllegalArgumentException("Field length '" + length
		      + "' with offset '" + offset + "' exceeds the number of bytes in the record '" + b.length + "'");
		}

		if (isBigEndian) {
			return getFieldValueBigEndian(b, offset, length);
		} else {
			return getFieldValueLittleEndian(b, offset, length);
		}
	}

	private long getFieldValueBigEndian(byte[] b, int offset, int length) {
		long result = 0;

		for (int i=0; i < length; ++i) {
			if (i==0 && isSigned) {
				if (b[offset + i] < 0) {
					result = -1;
				}
			}
			result = (result << 8) | (b[offset + i] & 0xFF);
		}

		return result;
	}

	private long getFieldValueLittleEndian(byte[] b, int offset, int length) {
		long result = 0;

		for (int i = offset+length-1; i >= offset; --i) {
			if (i==offset+length-1 && isSigned) {
				if (b[i] < 0) {
					result = -1;
				}
			}
			result = (result << 8) | (b[i] & 0xFF);
		}

		return result;
	}

	private BigInteger getBigIntegerFieldValue(byte[] b, int offset, int length) {
		if (dataLength != length) {
			throw new IllegalArgumentException("Declared field length does not match data type length "
					+ "(" + length + "!=" + this.dataLength + ")");
		} else if (offset > b.length) {
      throw new IllegalArgumentException("Field offset '" + offset
          + "' is greater than the number of bytes in the record '" + b.length + "'");
    } else if ((offset + length) > b.length) {
      throw new IllegalArgumentException("Field length '" + length
          + "' with offset '" + offset + "' exceeds the number of bytes in the record '" + b.length + "'");
    }

		if (isBigEndian) {
			return getBigIntegerFieldValueBigEndian(b, offset, length);
		} else {
			return getBigIntegerFieldValueLittleEndian(b, offset, length);
		}
	}

	private BigInteger getBigIntegerFieldValueBigEndian(byte[] b, int offset, int length) {
		byte[] temp = new byte[length+1];
		System.arraycopy(b, offset, temp, 1, length);

		if (!isSigned || b[offset] >= 0) {
			temp[0] = 0;
		} else {
			temp[0] = (byte) 0xFF;
		}

		return new BigInteger(temp);
	}

	private BigInteger getBigIntegerFieldValueLittleEndian(byte[] b, int offset, int length) {
		byte[] temp = new byte[length+1];
		for (int i=1; i < temp.length; ++i) {
			temp[i] = b[offset + length - i];
		}

		if (!isSigned || b[offset+length-1] >= 0) {
			temp[0] = 0;
		} else {
			temp[0] = (byte) 0xFF;
		}

		return new BigInteger(temp);
	}

	@Override
	public BigInteger getBigInteger(byte[] buf, int offset, int length,
			int startBit, int stopBit) {
		return getBigIntegerFieldValue(buf, offset, length);
	}

	@Override
	public void setBigInteger(BigInteger value, int offset, int length,
			ByteBuffer buffer, boolean isRightJustified) {

		byte[] b = getBytes(value);
		if (b.length > length) {
			throw new IllegalArgumentException("Value too large to fit in field (value="
					+ value.toString() + ", length=" + length + ")");
		}

		setBigIntegerFieldValue(b, offset, length, buffer);
	}

	private byte[] getBytes(BigInteger value) {
		byte[] b = value.toByteArray();
		if (b.length <= 1 || b[0] != 0) {
			return b;
		} else {
			byte[] temp = new byte[b.length - 1];
			System.arraycopy(b, 1, temp, 0, temp.length);
			return temp;
		}
	}

	private void setBigIntegerFieldValue(byte[] b, int offset, int length, ByteBuffer buffer) {
		if (isBigEndian) {
			setBigIntegerFieldValueBigEndian(b, offset, length, buffer);
		} else {
			setBigIntegerFieldValueLittleEndian(b, offset, length, buffer);
		}
	}

	private void setBigIntegerFieldValueBigEndian(byte[] b, int offset,
			int length, ByteBuffer buffer) {

		if (b.length < length) {
			byte[] temp = new byte[length - b.length];
			if (isSigned && b[0] < 0) {
				Arrays.fill(temp, (byte) 0xFF);
			}
			((Buffer) buffer).position(offset);
			buffer.put(temp);
		}

		buffer.put(b);
	}

	private void setBigIntegerFieldValueLittleEndian(byte[] b, int offset,
			int length, ByteBuffer buffer) {

		for (int i=b.length-1; i >= 0; --i) {
			buffer.put(offset++, b[i]);
		}

		byte fillValue = (byte) 0;
		if (isSigned && b[0] < 0) {
			fillValue = (byte) 0xFF;
		}
		for (int i=b.length; i < length; ++i) {
			buffer.put(offset++, fillValue);
		}
	}

}
