package uk.ac.starlink.array;

import java.io.IOException;

/**
 * Wraps an NDArray to produce a virtual NDArray whose pixels are modified
 * using a Converter object.  Amongst other possibilities this can perform
 * type conversion and transform pixel values using a supplied real
 * function.
 * 
 * @author   Mark Taylor (Starlink)
 * @see  Converter
 * @see  TypeConverter
 */
public class ConvertArrayImpl extends WrapperArrayImpl {

    private final NDArray nda;
    private final Converter conv;
    private final Type type1;
    private final Type type2;
    private final static int BUFSIZE = ChunkStepper.defaultChunkSize;

    /**
     * Constructs a new ArrayImpl from an underlying NDArray and a supplied
     * converter object.
     *
     * @param  nda    the base NDArray which supplies the pixels to be 
     *                converted
     * @param  conv   a Converter object which operates on the pixels of nda.
     *                Its base type (type1) must match the type of nda.
     * @throws IllegalArgumentException  if conv.getType1() is not equal 
     *                to nda.getType().
     */
    public ConvertArrayImpl( NDArray nda, Converter conv ) {
        super( nda );
        this.nda = nda;
        this.conv = conv;
        if ( conv.getType1() != nda.getType() ) {
            throw new IllegalArgumentException(
                "Converter type 1 (" + conv.getType1() + ") does not match " +
                "NDArray type (" + nda.getType() + ")" );
        }
        type1 = conv.getType1();
        type2 = conv.getType2();
    }

    public Type getType() {
        return type2;
    }

    public Number getBadValue() {
        return conv.getBadHandler2().getBadValue();
    }

    public AccessImpl getAccess() throws IOException {
        return new AccessImpl() {

            private ArrayAccess acc = nda.getAccess();

            private Object standardBuffer1 = type1.newArray( BUFSIZE );

            public void setOffset( long off ) throws IOException {
                acc.setOffset( off );
            }

            public void read( Object buffer2, int start, int size ) 
                    throws IOException {
                Object buffer1 = getBuffer1( size );
                acc.read( buffer1, 0, size );
                conv.convert12( buffer1, 0, buffer2, start, size );
            }

            public void write( Object buffer2, int start, int size )
                    throws IOException {
                Object buffer1 = getBuffer1( size );
                conv.convert21( buffer2, start, buffer1, 0, size );
                acc.write( buffer1, 0, size );
            }

            public void close() throws IOException {
                acc.close();
            }

            private final Object getBuffer1( int size ) {
                return ( size <= BUFSIZE ) ? standardBuffer1
                                           : type1.newArray( size );
            }
        };
    }
}
