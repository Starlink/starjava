package uk.ac.starlink.treeview;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.array.AccessImpl;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.array.WrapperArrayImpl;

/**
 * Wraps an NDArray to provide another with fewer dimensions.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CollapseArrayImpl extends WrapperArrayImpl {

    private final NDArray baseNda;
    private final OrderedNDShape baseShape;
    private final long[] baseOrigin;
    private final long[] baseDims;
    private final BadHandler baseHandler;
    private final Type baseType;
    private final OrderedNDShape shape;
    private final Type type;
    private final BadHandler handler;
    private final boolean hasFitsOrder;
    private final long[] origin;
    private final long[] dims;
    private final int ndim;
    private final int collAxis;
    private final long collOrigin;
    private final long collDim;
    private final Collapsor collapsor;
    private final long stride;
    private final long[] st2;

    /**
     * Constructs a new CollapseArrayImpl from an underlying NDArray
     * over the whole of a given axis.
     */
    public CollapseArrayImpl( NDArray nda, int collAxis ) {
        this( nda, collAxis, nda.getShape().getOrigin()[ collAxis ], 
              nda.getShape().getDims()[ collAxis ] );
    }

    /**
     * Constructs a new CollapseArrayImpl from an underlying NDArray
     * over a given interval along a given axis.
     */
    public CollapseArrayImpl( NDArray nda, int collAxis, long collOrigin, 
                              long collDim ) {
        super( nda );
        this.baseNda = nda;
        this.baseShape = baseNda.getShape();
        this.baseHandler = baseNda.getBadHandler();
        this.baseType = baseNda.getType();
        this.collAxis = collAxis;
        this.collOrigin = collOrigin;
        this.collDim = collDim;
        this.collapsor = getMeanCollapsor( baseType );
        int baseNdim = baseShape.getNumDims();
        this.hasFitsOrder = baseShape.getOrder().isFitsLike();
        this.ndim = baseNdim - 1;
        this.baseOrigin = baseShape.getOrigin();
        this.baseDims = baseShape.getDims();

        /* Validate. */
        if ( collAxis < 0 || collAxis >= baseNdim ) {
            throw new IllegalArgumentException( 
                "Collapse axis " + collAxis + " does not exist" );
        }
        if ( collOrigin < baseShape.getOrigin()[ collAxis ] ||
             collOrigin + collDim > baseShape.getLimits()[ collAxis ] ) {
            throw new IllegalArgumentException(
                "Collapse limits out of base array bounds" );
        }
        if ( ! baseNda.isRandom() ) {
            throw new IllegalArgumentException(
                "Base array does not have random access" );
        }
        if ( collDim > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                "Implemented does not permit collapse dimension > " +
                "Integer.MAX_VALUE, sorry" );
        }

        /* Calculate the shape of this array. */
        this.origin = new long[ ndim ];
        this.dims = new long[ ndim ];
        for ( int i = 0, j = 0; i < baseNdim; i++ ) {
            if ( i != collAxis ) {
                origin[ j ] = baseOrigin[ i ];
                dims[ j ] = baseDims[ i ];
                j++;
            }
        }
        this.shape = new OrderedNDShape( origin, dims, baseShape.getOrder() );

        /* Decide on the data type of this array. */
        this.type = collapsor.getOutputType();
        this.handler = type.defaultBadHandler();

        /* Calculate the step between base array pixels which map to
         * the same collapsed array pixel. */
        long str = 1L;
        for ( int j = 0; j < ndim; j++ ) {
            int i = hasFitsOrder ? j : ( ndim - 1 - j );
            if ( i < collAxis ) {
                str *= baseDims[ i ];
            }
        }
        stride = str;

        /* Calculate the coefficients for working out what base array 
         * offset corresponds to a given position in the collapsed array. */
        long[] st1 = new long[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            st1[ i ] = ( i == 0 ) ? 1L : ( st1[ i - 1 ] * dims[ i - 1 ] );
            if ( i == collAxis ) {
                st1[ i ] *= baseDims[ collAxis ];
            }
        }
        st2 = new long[ ndim ];
        for ( int j = 0; j < ndim; j++ ) {
            int i = hasFitsOrder ? j : ( ndim - 1 - j );
            st2[ i ] = st1[ j ];
        }
    }

    public OrderedNDShape getShape() {
        return shape;
    }

    public Type getType() {
        return type;
    }

    public BadHandler getBadHandler() {
        return handler;
    }

    public boolean isWritable() {
        return false;
    }

    public AccessImpl getAccess() throws IOException {
        return new CollapseAccessImpl();
    }


    private class CollapseAccessImpl implements AccessImpl {
        private ArrayAccess baseAcc;
        private long offset;
        private Object cbuf;
        private long baseStart0;

        public CollapseAccessImpl() throws IOException {
            baseAcc = baseNda.getAccess();
            offset = 0L;
            assert collDim < Integer.MAX_VALUE; // checked in constructor
            cbuf = baseType.newArray( (int) collDim );

            baseStart0 = collOrigin - baseOrigin[ collAxis ];
            for ( int i = 0; i < collAxis; i++ ) {
                baseStart0 *= baseDims[ i ];
            }
        }

        public void setOffset( long off ) throws IOException {
            this.offset = off;
        }

        public void read( Object buffer, int start, int size ) 
                throws IOException {

            /* Loop over the pixels in the output array. */
            Iterator it = shape.pixelIterator( offset, size );
            for ( int i = 0; i < size; i++ ) {

                /* Get the position of the first pixel in the base array
                 * which contributes to the current output pixel. */
                long baseStart = posToBaseStart( (long[]) it.next() );

                /* Step through all the pixels in the base array which
                 * contribute to the current output pixel. */
                int ngood = 0;
                for ( int j = 0; j < collDim; j++ ) {
                    baseAcc.setOffset( baseStart + j * stride );
                    baseAcc.read( cbuf, ngood, 1 );
                    if ( ! baseHandler.isBad( cbuf, ngood ) ) {
                        ngood++;
                    }
                }

                /* Combine the good pixels. */
                if ( ngood > 0 ) {
                    collapsor.collapse( cbuf, ngood, buffer, start + i );
                }
                else {
                    handler.putBad( buffer, start + i );
                }
            }
            offset += size;
        }
            
        public void write( Object buffer, int start, int size ) {
            throw new UnsupportedOperationException();
        }

        public void close() throws IOException {
            baseAcc.close();
        }

        private long posToBaseStart( long[] pos ) {
            long baseStart = baseStart0;
            for ( int i = 0; i < ndim; i++ ) {
                baseStart += ( pos[ i ] - origin[ i ] ) * st2[ i ];
            }
            return baseStart;
        }

    }


    private static interface Collapsor {
        public Type getOutputType();
        public void collapse( Object inBuf, int inSize,
                              Object outBuf, int outOff );
    }

    private static Collapsor getMeanCollapsor( Type inType ) {
        if ( inType == Type.BYTE ) {
            return new Collapsor() {
                public Type getOutputType() { return Type.FLOAT; }
                public void collapse( Object inBuf, int inSize,
                                      Object outBuf, int outOff ) { 
                    float sum = 0.0f;
                    for ( int i = 0; i < inSize; i++ ) {
                        sum += ((byte[]) inBuf)[ i ];
                    }
                    ((float[]) outBuf)[ outOff ] = sum / inSize;
                }
            };
        }
        else if ( inType == Type.SHORT ) {
            return new Collapsor() {
                public Type getOutputType() { return Type.FLOAT; }
                public void collapse( Object inBuf, int inSize,
                                      Object outBuf, int outOff ) {
                    float sum = 0.0f;
                    for ( int i = 0; i < inSize; i++ ) {
                        sum += ((short[]) inBuf)[ i ];
                    }
                    ((float[]) outBuf)[ outOff ] = sum / inSize;
                }
            };
        }
        else if ( inType == Type.INT ) {
            return new Collapsor() {
                public Type getOutputType() { return Type.FLOAT; }
                public void collapse( Object inBuf, int inSize,
                                      Object outBuf, int outOff ) {
                    float sum = 0.0f;
                    for ( int i = 0; i < inSize; i++ ) {
                        sum += ((int[]) inBuf)[ i ];
                    }
                    ((float[]) outBuf)[ outOff ] = sum / inSize;
                }
            };
        }
        else if ( inType == Type.FLOAT ) {
            return new Collapsor() {
                public Type getOutputType() { return Type.FLOAT; }
                public void collapse( Object inBuf, int inSize,
                                      Object outBuf, int outOff ) {
                    float sum = 0.0f;
                    for ( int i = 0; i < inSize; i++ ) {
                        sum += ((float[]) inBuf)[ i ];
                    }
                    ((float[]) outBuf)[ outOff ] = sum / inSize;
                }
            };
        }
        else if ( inType == Type.DOUBLE ) {
            return new Collapsor() {
                public Type getOutputType() { return Type.DOUBLE; }
                public void collapse( Object inBuf, int inSize,
                                      Object outBuf, int outOff ) {
                    double sum = 0.0;
                    for ( int i = 0; i < inSize; i++ ) {
                        sum += ((double[]) inBuf)[ i ];
                    }
                    ((double[]) outBuf)[ outOff ] = sum / inSize;
                }
            };
        }
        else {
            throw new AssertionError( "Unknown type" );
        }
    }

}
