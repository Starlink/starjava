package uk.ac.starlink.pds4;

import gov.nasa.pds.objectAccess.table.FieldAdapter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper for FieldAdapter that catches and logs RuntimeExceptions.
 * Probably such exceptions ought not to happen, but for instance
 * NumberFormatExceptions are not impossible, and it may be preferred
 * to log them than for the entire table read to fail.
 *
 * <p>Currently only the <code>get*</code> methods are so wrapped,
 * since the <code>set*</code> methods are (a) less likely to encounter
 * problems from data and (b) not used by this package.
 *
 * @author   Mark Taylor
 * @since    10 Nov 2022
 */
public class SafeFieldAdapter implements FieldAdapter {

    private final FieldAdapter base_;
    private int nWarn_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.pds4" );

    /**
     * Constructor.
     *
     * @param   base  instance on which this object's behaviour is based
     */
    public SafeFieldAdapter( FieldAdapter base ) {
        base_ = base;
    }

    public byte getByte( byte[] buf, int offset, int length,
                         int startBit, int stopBit ) {
        try {
            return base_.getByte( buf, offset, length, startBit, stopBit );
        }
        catch ( RuntimeException e ) {
            reportError( e, "0" );
            return (byte) 0;
        }
    }

    public short getShort( byte[] buf, int offset, int length,
                           int startBit, int stopBit ) {
        try {
            return base_.getShort( buf, offset, length, startBit, stopBit );
        }
        catch ( RuntimeException e ) {
            reportError( e, "0" );
            return (short) 0;
        }
    }

    public int getInt( byte[] buf, int offset, int length,
                       int startBit, int stopBit ) {
        try {
            return base_.getInt( buf, offset, length, startBit, stopBit );
        }
        catch ( RuntimeException e ) {
            reportError( e, "0" );
            return 0;
        }
    }

    public long getLong( byte[] buf, int offset, int length,
                         int startBit, int stopBit ) {
        try {
            return base_.getLong( buf, offset, length, startBit, stopBit );
        }
        catch ( RuntimeException e ) {
            reportError( e, "0" );
            return 0L;
        }
    }

    public BigInteger getBigInteger( byte[] buf, int offset, int length,
                                     int startBit, int stopBit ) {
        try {
            return base_.getBigInteger( buf, offset, length,
                                        startBit, stopBit );
        }
        catch ( RuntimeException e ) {
            reportError( e, "null" );
            return null;
        }
    }

    public float getFloat( byte[] buf, int offset, int length,
                           int startBit, int stopBit ) {
        try {
            return base_.getFloat( buf, offset, length, startBit, stopBit );
        }
        catch ( RuntimeException e ) {
            reportError( e, "NaN" );
            return Float.NaN;
        }
    }

    public double getDouble( byte[] buf, int offset, int length,
                             int startBit, int stopBit ) {
        try {
            return base_.getDouble( buf, offset, length, startBit, stopBit );
        }
        catch ( RuntimeException e ) {
            reportError( e, "NaN" );
            return Double.NaN;
        }
    }

    public String getString( byte[] buf, int offset, int length,
                             int startBit, int stopBit ) {
        try {
            return base_.getString( buf, offset, length, startBit, stopBit );
        }
        catch ( RuntimeException e ) {
            reportError( e, "null" );
            return null;
        }
    }

    public String getString( byte[] buf, int offset, int length,
                             int startBit, int stopBit, Charset charset ) {
        try {
            return base_.getString( buf, offset, length, startBit, stopBit,
                                    charset );
        }
        catch ( RuntimeException e ) {
            reportError( e, "null" );
            return null;
        }
    }

    public void setString( String value, int offset, int length,
                           ByteBuffer buf, boolean isRightJustified ) {
        base_.setString( value, offset, length, buf, isRightJustified );
    }

    public void setString( String value, int offset, int length,
                           ByteBuffer buf, boolean isRightJustified,
                           Charset charset ) {
        base_.setString( value, offset, length, buf, isRightJustified,
                         charset );
    }

    public void setInt( int value, int offset, int length,
                        ByteBuffer buffer, boolean isRightJustified ) {
        base_.setInt( value, offset, length, buffer, isRightJustified );
    }

    public void setDouble( double value, int offset, int length,
                           ByteBuffer buf, boolean isRightJustified ) {
        base_.setDouble( value, offset, length, buf, isRightJustified );
    }

    public void setFloat( float value, int offset, int length,
                          ByteBuffer buf, boolean isRightJustified ) {
        base_.setFloat( value, offset, length, buf, isRightJustified );
    }

    public void setShort( short value, int offset, int length,
                          ByteBuffer buf, boolean isRightJustified ) {
        base_.setShort( value, offset, length, buf, isRightJustified );
    }

    public void setByte( byte value, int offset, int length,
                         ByteBuffer buf, boolean isRightJustified ) {
        base_.setByte( value, offset, length, buf, isRightJustified );
    }

    public void setLong( long value, int offset, int length,
                         ByteBuffer buf, boolean isRightJustified ) {
        base_.setLong( value, offset, length, buf, isRightJustified );
    }

    public void setBigInteger( BigInteger value, int offset, int length,
                               ByteBuffer buf, boolean isRightJustified ) {
        base_.setBigInteger( value, offset, length, buf, isRightJustified );
    }

    /**
     * Reports an error encountered during processing in an appropriate way.
     * Currently, only the first such error encountered by this instance
     * is logged and others are silently ignored.
     * 
     * @param  e  error encountered during read
     * @param  dfltValue   value returned in place of failed read,
     *                     used to inform user
     */
    private void reportError( RuntimeException e, String dfltValue ) {
        if ( nWarn_ == 0 ) {
            String msg = "PDS4 library code failed at least once during read, "
                       + "using default value " + dfltValue;
            logger_.log( Level.WARNING, msg, e );
        }
        nWarn_++;
    }
}
