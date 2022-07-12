package uk.ac.starlink.pds4;

import gov.nasa.pds.objectAccess.table.DefaultFieldAdapter;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Implements a field adapter for numeric fields stored in
 * textual format, as in a character table.
 *
 * <p>This class is written with reference to
 * gov.nasa.pds.objectAccess.table.NumericTextFieldAdapter;
 * this implementation just adds a radix where appropriate to
 * provide I/O in a non-decimal base.
 *
 * @author   Mark Taylor
 * @since    12 Jul 2022
 */
public class RadixTextFieldAdapter extends DefaultFieldAdapter {

        private final int radix;

        /**
         * Constructor.
         *
         * @param  radix  numeric base in which values are represented
         */
        public RadixTextFieldAdapter(int radix) {
            this.radix = radix;
        }

	@Override
	public byte getByte(byte[] buf, int offset, int length, int startBit, int stopBit) {
		int value = Integer.parseInt(getString(buf, offset, length, startBit, stopBit).trim(), radix);
		if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
			throw new NumberFormatException("Value is out of range of a byte (" + value + ")");
		}

		return (byte) value;
	}

	@Override
	public short getShort(byte[] buf, int offset, int length, int startBit, int stopBit) {
		int value = Integer.parseInt(getString(buf, offset, length, startBit, stopBit).trim(), radix);
		if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
			throw new NumberFormatException("Value is out of range of a short (" + value + ")");
		}

		return (short) value;
	}

	@Override
	public int getInt(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return Integer.parseInt(getString(buf, offset, length, startBit, stopBit).trim(), radix);
	}

	@Override
	public long getLong(byte[] buf, int offset, int length, int startBit, int stopBit) {
		return Long.parseLong(getString(buf, offset, length, startBit, stopBit).trim(), radix);
	}

	@Override
	public float getFloat(byte[] buf, int offset, int length, int startBit, int stopBit) {
                throw new UnsupportedOperationException();
	}

	@Override
	public double getDouble(byte[] buf, int offset, int length, int startBit, int stopBit) {
                throw new UnsupportedOperationException();
	}

	@Override
	public BigInteger getBigInteger(byte[] buf, int offset, int length,
			int startBit, int stopBit) {
		return new BigInteger(getString(buf, offset, length, startBit, stopBit), radix);
	}

	//
	// TODO: need to pass in charset to setString() in all setter?
	//

	@Override
	public void setByte(byte value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		setString(Integer.toString(value, radix), offset, length, buffer, isRightJustified);
	}

	@Override
	public void setShort(short value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		setString(Integer.toString(value, radix), offset, length, buffer, isRightJustified);
	}

	@Override
	public void setInt(int value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		setString(Integer.toString(value, radix), offset, length, buffer, isRightJustified);
	}

	@Override
	public void setLong(long value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
		setString(Long.toString(value, radix), offset, length, buffer, isRightJustified);
	}

	@Override
	public void setFloat(float value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
                throw new UnsupportedOperationException();
	}

	@Override
	public void setDouble(double value, int offset, int length, ByteBuffer buffer, boolean isRightJustified) {
                throw new UnsupportedOperationException();
	}

	@Override
	public void setBigInteger(BigInteger value, int offset, int length,
			ByteBuffer buffer, boolean isRightJustified) {
		setString(value.toString(radix), offset, length, buffer, isRightJustified);
	}

}
