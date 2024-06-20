package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import uk.ac.starlink.util.ValueWatcher;

/**
 * Abstract superclass for objects which do the nuts and bolts of
 * writing integer data for column-oriented storage.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2006
 * @see      ColumnStore
 */
abstract class IntegerStorage {

    private final char formatChar_;
    private final int typeBytes_;
    private final ValueWatcher badWatcher_;

    /**
     * Constructor.
     *
     * @param   formatChar  FITS formatting character
     * @param   typeBytes   number of bytes per written integer
     * @param   loBad       lower limit for possible bad values (inclusive)
     * @param   hiBad       upper limit for possible bad values (inclusive)
     */
    protected IntegerStorage( char formatChar, int typeBytes,
                              long loBad, long hiBad ) {
        formatChar_ = formatChar;
        typeBytes_ = typeBytes;
        badWatcher_ = new ValueWatcher( loBad, hiBad );
    }

    /**
     * Returns the FITS formatting character for this handler's type.
     * 
     * @return   formatting character
     */
    public char getFormatChar() {
        return formatChar_;
    }

    /**
     * Returns the number of bytes for each integer.
     * 
     * @return   bytes written by calls of <code>writeValue</code>
     */
    public int getTypeBytes() {
        return typeBytes_;
    }

    /**
     * Writes the bytes for a given integer value, and also ensures that
     * that pattern will not be returned by a subsequent call to
     * {@link #getBadBytes}.
     * Number of bytes written matches <code>getTypeBytes</code>.
     *
     * @param   value  value to write
     * @param   out    destination stream
     */
    public final void writeValue( long value, DataOutput out )
            throws IOException {
        badWatcher_.useValue( value );
        doWriteValue( value, out );
    }

    /**
     * Writes the bytes for a given integer value.
     * Number of bytes written matches <code>getTypeBytes</code>.
     *
     * @param   value  value to write
     * @param   out    destination stream
     */
    protected abstract void doWriteValue( long value, DataOutput out )
            throws IOException;

    /**
     * Returns a bit pattern representing a bad value.
     * This is guaranteed not to be one of the values which has
     * been so far passed to <code>noteValue</code>.
     * The length of the array matches <code>getTypeBytes</code>.
     *
     * <p>If it can't be done, null is returned.
     *
     * @return  bad value pattern
     */
    public byte[] getBadBytes() {
        Long badObj = badWatcher_.getUnused();
        if ( badObj == null ) {
            return null;
        }
        else {
            byte[] badbuf = new byte[ typeBytes_ ];
            long bad = badObj.longValue();
            for ( int i = typeBytes_ - 1; i >= 0; i-- ) {
                badbuf[ i ] = (byte) ( 0xff & bad );
                bad = bad >> 8;
            }
            return badbuf;
        }
    }

    /**
     * Returns the bad value as a Number object.
     * This represents the same value as the bit pattern given by
     * {@link #getBadBytes}, and like that, may be null.
     *
     * @return  bad value
     */
    public Number getBadNumber() {
        Long badLong = badWatcher_.getUnused();
        return badLong == null ? null
                               : convertLong( badLong.longValue() );
    }

    /**
     * Converts a <code>long</code> to a number object 
     * appropriate to this storage type.
     *
     * @param  val  value
     * @return  <code>val</code> converted to this type
     */
    protected abstract Number convertLong( long val );

    /**
     * Returns a new IntegerStorage instance suitable for <code>byte</code>s.
     *
     * @return  byte storage object
     */
    public static IntegerStorage createByteStorage() {
        return new IntegerStorage( 'B', 1, Byte.MIN_VALUE, Byte.MAX_VALUE ) {
            protected void doWriteValue( long value, DataOutput out ) 
                    throws IOException {
                out.writeByte( (byte) value );
            }
            protected Number convertLong( long val ) {
                return Byte.valueOf( (byte) val );
            }
        };
    }

    /**
     * Returns a new IntegerStorage instance suitable for <code>short</code>s.
     *
     * @return  short storage object
     */
    public static IntegerStorage createShortStorage() {
        return new IntegerStorage( 'I', 2, Short.MIN_VALUE, Short.MAX_VALUE ) {
            protected void doWriteValue( long value, DataOutput out )
                    throws IOException {
                out.writeShort( (short) value );
            }
            protected Number convertLong( long val ) {
                return Short.valueOf( (short) val );
            }
        };
    }

    /**
     * Returns a new IntegerStorage instance suitable for <code>int</code>s.
     *
     * @return  int storage object
     */
    public static IntegerStorage createIntStorage() {
        return new IntegerStorage( 'J', 4, Integer.MIN_VALUE,
                                   Integer.MIN_VALUE + 2048 ) {
            protected void doWriteValue( long value, DataOutput out )
                    throws IOException {
                out.writeInt( (int) value );
            }
            protected Number convertLong( long val ) {
                return Integer.valueOf( (int) val );
            }
        };
    }

    /**
     * Returns a new IntegerStorage instance suitable for <code>long</code>s.
     *
     * @return  long storage object
     */
    public static IntegerStorage createLongStorage() {
        return new IntegerStorage( 'K', 8, Long.MIN_VALUE,
                                   Long.MIN_VALUE + 2048 ) {
            protected void doWriteValue( long value, DataOutput out )
                    throws IOException {
                out.writeLong( value );
            }
            protected Number convertLong( long val ) {
                return Long.valueOf( val );
            }
        };
    };
}
