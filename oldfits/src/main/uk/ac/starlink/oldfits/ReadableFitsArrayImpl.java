package uk.ac.starlink.oldfits;

import java.io.IOException;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.AccessImpl;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.util.IOUtils;

/**
 * ArrayImpl implementation for reading data from a FITS stream.
 *
 * @author   Mark Taylor (Starlink)
 */
class ReadableFitsArrayImpl implements ArrayImpl {

    private final ArrayDataInput stream;
    private final OrderedNDShape oshape;
    private final Type type;
    private final Number badValue;
    private final AccessMode mode;
    private final boolean isRandom;
    private final Header hdr;
    private final long strmBase;
    private final int nByte;
    private final TypedReader rdr;

    ReadableFitsArrayImpl( ArrayDataInput istream, AccessMode mode ) 
            throws FitsException, IOException {

        /* Save the stream and its starting position. */
        this.stream = istream;
        this.isRandom = stream instanceof RandomAccess;
        this.mode = mode;

        /* Read the FITS header cards. */
        hdr = Header.readHeader( stream );

        /* Initialise the position in the stream for the start of array data. */
        this.strmBase = isRandom ? ((RandomAccess) stream).getFilePointer()
                                 : 0L;

        /* Determine the shape. */
        long[] dims = getDimsFromHeader( hdr );
        long[] origin = getOriginFromHeader( hdr );
        this.oshape = ( origin != null )
                    ? new OrderedNDShape( origin, dims, Order.COLUMN_MAJOR )
                    : new OrderedNDShape( dims, Order.COLUMN_MAJOR );

        /* Determine the data type and blank value. */
        boolean hasBlank = hdr.containsKey( "BLANK" );
        switch ( hdr.getIntValue( "BITPIX" ) ) {
            case BasicHDU.BITPIX_BYTE:
                type = Type.BYTE;
                badValue = hasBlank
                         ? Byte.valueOf( (byte) hdr.getIntValue( "BLANK" ) )
                         : null;
                rdr = new TypedReader() {
                    public void read( Object buffer, int start, int size ) 
                            throws IOException {
                        stream.read( (byte[]) buffer, start, size );
                    }
                    public void write( Object buffer, int start, int size )
                            throws IOException {
                        ((ArrayDataOutput) stream).write( (byte[]) buffer,
                                                          start, size );
                    }
                };
                break;
            case BasicHDU.BITPIX_SHORT:
                type = Type.SHORT;
                badValue = hasBlank
                         ? Short.valueOf( (short) hdr.getIntValue( "BLANK" ) )
                         : null;
                rdr = new TypedReader() {
                    public void read( Object buffer, int start, int size ) 
                            throws IOException {
                        stream.read( (short[]) buffer, start, size );
                    }
                    public void write( Object buffer, int start, int size )
                            throws IOException {
                        ((ArrayDataOutput) stream).write( (short[]) buffer,
                                                          start, size );
                    }
                };
                break;
            case BasicHDU.BITPIX_INT:
                type = Type.INT;
                badValue = hasBlank
                         ? Integer.valueOf( hdr.getIntValue( "BLANK" ) )
                         : null;
                rdr = new TypedReader() {
                    public void read( Object buffer, int start, int size ) 
                            throws IOException {
                        stream.read( (int[]) buffer, start, size );
                    }
                    public void write( Object buffer, int start, int size )
                            throws IOException {
                        ((ArrayDataOutput) stream).write( (int[]) buffer,
                                                          start, size );
                    }
                };
                break;
            case BasicHDU.BITPIX_FLOAT:
                type = Type.FLOAT;
                badValue = Float.valueOf( Float.NaN );
                rdr = new TypedReader() {
                    public void read( Object buffer, int start, int size ) 
                            throws IOException {
                        stream.read( (float[]) buffer, start, size );
                    }
                    public void write( Object buffer, int start, int size )
                            throws IOException {
                        ((ArrayDataOutput) stream).write( (float[]) buffer,
                                                          start, size );
                    }
                };
                break;
            case BasicHDU.BITPIX_DOUBLE:
                type = Type.DOUBLE;
                badValue = Double.valueOf( Double.NaN );
                rdr = new TypedReader() {
                    public void read( Object buffer, int start, int size ) 
                            throws IOException {
                        stream.read( (double[]) buffer, start, size );
                    }
                    public void write( Object buffer, int start, int size )
                            throws IOException {
                        ((ArrayDataOutput) stream).write( (double[]) buffer,
                                                          start, size );
                    }
                };
                break;
            default:
                throw new UnsupportedOperationException(
                    "Unsupported FITS data type" );
        }

        /* Record the number of bytes per element. */
        this.nByte = type.getNumBytes();
    }

    public OrderedNDShape getShape() {
        return oshape;
    }
    public Type getType() {
        return type;
    }
    public Number getBadValue() {
        return badValue;
    }
    public boolean isReadable() {
        return true;
    }
    public boolean isWritable() {
        return mode.isWritable();
    }
    public boolean isRandom() {
        return isRandom;
    }
    public boolean canMap() {
        return false;
    }
    public Object getMapped() {
        throw new AssertionError();
    }
    public boolean multipleAccess() {
        return false;
    }

    public void open() {
    }

    public AccessImpl getAccess() {
        return new AccessImpl() {
            private long offset = 0L;
            public void setOffset( long off ) throws IOException {
                if ( isRandom ) {
                    ((RandomAccess) stream).seek( strmBase + ( off * nByte ) );
                }
                else {
                    IOUtils.skipBytes( stream, ( off - offset ) * nByte );
                }
                offset = off;
            }
            public void read( Object buffer, int start, int size ) 
                    throws IOException {
                rdr.read( buffer, start, size );
                offset += size;
            }
            public void write( Object buffer, int start, int size ) 
                    throws IOException {
                rdr.write( buffer, start, size );
                offset += size;
            }
            public void close() {
            }
        };
    }

    public void close() throws IOException {
        stream.close();
    }

    /**
     * Returns the original FITS Header object relating to this array.
     *
     * @return   the Header that was read
     */
    Header getHeader() {
        return hdr;
    }

    private static long[] getDimsFromHeader( Header hdr ) throws FitsException {
        int naxis = hdr.getIntValue( "NAXIS" );
        long[] dimensions = new long[ naxis ];
        for ( int i = 0; i < naxis; i++ ) {
            String key = "NAXIS" + ( i + 1 );
            if ( hdr.containsKey( key ) ) {
                dimensions[ i ] =  hdr.getLongValue( key );
            }
            else {
                throw new FitsException( "No header card + " + key );
            }
        }
        return dimensions;
    }

    private static long[] getOriginFromHeader( Header hdr ) {
        int naxis = hdr.getIntValue( "NAXIS" );
        long[] origin = new long[ naxis ];
        boolean ok = true;
        for ( int i = 0; i < naxis && ok; i++ ) {
            String cardName = FitsConstants.originCardName( i );
            if ( hdr.containsKey( cardName ) ) {
                origin[ i ] = hdr.getLongValue( cardName );
            }
            else {
                ok = false;
            }
        }
        return ok ? origin : null;
    }

    /**
     * Private helper type to assist in reading arrays of different types.
     */
    private static interface TypedReader {
        void read( Object buffer, int start, int size ) throws IOException;
        void write( Object buffer, int start, int size ) throws IOException;
    }

}
