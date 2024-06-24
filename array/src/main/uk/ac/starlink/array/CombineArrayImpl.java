package uk.ac.starlink.array;

import java.io.IOException;

/**
 * Virtual NDArray implementation combining two given NDArrays using an
 * arbitrary arithmetic function.  Each pixel in the resulting array
 * is the result of combining the two corresponding pixels of the input
 * arrays using a supplied {@link Combiner} object.
 * Type conversions are taken care of, and values which end up outside
 * the range of the type of the resulting array are automatically 
 * turned Bad.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CombineArrayImpl implements ArrayImpl {

    private Combiner combi;
    private OrderedNDShape oshape;
    private Type type;
    private Type internalType = Type.DOUBLE;
    private BadHandler internalHandler = internalType.defaultBadHandler();
    private NDArray nda1;
    private NDArray nda2;
    private NDArray rnda1;
    private NDArray rnda2;
    private Requirements req;
    private BadHandler bh1;
    private BadHandler bh2;
    private BadHandler bh;
    private Converter tconv1;
    private Converter tconv2;
    private Converter tconv;
    private boolean isRandom;
    private boolean multipleAccess;
    private static final int BUFSIZE = ChunkStepper.defaultChunkSize;

    /**
     * Construct a new CombineArrayImpl based on two underlying NDArrays
     * and a combiner object.  The supplied array objects do not need
     * to be the same type or shape or the same type or shape as the
     * resulting array object.  Values outside the shape of one or the other
     * will be presented as a Bad value to the Combiner object.
     *
     * @param  nda1  the first array to combine
     * @param  nda2  the second array to combine
     * @param  combi  the object which performs the arithmetic combination
     * @param  shape  the shape of the resulting array object
     * @param  type  the type of the resulting array object
     * @param  bh    the bad value handler for the resulting array object.
     *               If <code>null</code>, a non-null bad value handler using a
     *               default value is used.
     */
    public CombineArrayImpl( NDArray nda1, NDArray nda2, Combiner combi,
                             NDShape shape, Type type, BadHandler bh ) {
        this.combi = combi;
        this.oshape = new OrderedNDShape( shape, nda1.getShape().getOrder() );
        this.type = type;
        this.nda1 = nda1;
        this.nda2 = nda2;
        isRandom = nda1.isRandom() && nda2.isRandom();
        multipleAccess = nda1.isRandom() && nda2.multipleAccess();
        req = new Requirements( AccessMode.READ )
                          .setType( internalType )
                          .setWindow( shape );
        this.bh = ( bh != null ) 
                     ? bh 
                     : BadHandler.getHandler( type, type.defaultBadValue() );
        tconv = new TypeConverter( internalType, internalHandler,
                                   type, this.bh );
    }
    
    public OrderedNDShape getShape() {
        return oshape;
    }

    public Type getType() {
        return type;
    }

    public Number getBadValue() {
        return bh.getBadValue();
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isWritable() {
        return false;
    }

    public boolean isRandom() {
        return isRandom;
    }

    public boolean multipleAccess() {
        return multipleAccess;
    }

    public void open() throws IOException {
        rnda1 = NDArrays.toRequiredArray( nda1, req );
        rnda2 = NDArrays.toRequiredArray( nda2, req );
    }
        
    public boolean canMap() {
        return false;
    }

    public Object getMapped() {
        return null;
    }

    public AccessImpl getAccess() throws IOException {
        return new AccessImpl() {

            private ArrayAccess acc1 = rnda1.getAccess();
            private ArrayAccess acc2 = rnda2.getAccess();

            private Object standardBuffer1 = internalType.newArray( BUFSIZE );
            private Object standardBuffer2 = internalType.newArray( BUFSIZE );
            private Object standardBuffer3 = internalType.newArray( BUFSIZE );

            public void setOffset( long off ) throws IOException {
                acc1.setOffset( off );
                acc2.setOffset( off );
            }
            public void read( Object buffer, int start, int size )
                    throws IOException {
                double[] buf1 = (double[]) getBuffer1( size );
                double[] buf2 = (double[]) getBuffer2( size );
                double[] buf3 = (double[]) getBuffer3( size );
                acc1.read( buf1, 0, size );
                acc2.read( buf2, 0, size );
                for ( int i = 0; i < size; i++ ) {
                    buf3[ i ] = combi.combination( buf1[ i ], buf2[ i ] );
                }
                tconv.convert12( buf3, 0, buffer, start, size );
            }
            public void write( Object buffer, int start, int size ) {
                throw new AssertionError();
            }
            public void close() throws IOException {
                acc1.close();
                acc2.close();
            }

            private Object getBuffer1( int size ) {
                return ( size <= BUFSIZE ) ? standardBuffer1
                                           : internalType.newArray( size );
            }
            private Object getBuffer2( int size ) {
                return ( size <= BUFSIZE ) ? standardBuffer2
                                           : internalType.newArray( size );
            }
            private Object getBuffer3( int size ) {
                return ( size <= BUFSIZE ) ? standardBuffer3
                                           : internalType.newArray( size );
            }
        };
    }

    public void close() throws IOException {
        if ( rnda1 != null ) {
            rnda1.close();
        }
        if ( rnda1 != null ) {
            rnda2.close();
        }
    }
    
}
