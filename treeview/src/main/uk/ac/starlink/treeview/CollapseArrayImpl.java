package uk.ac.starlink.treeview;

// NB: this version does not compile and may contain various kinds of
// errors.  It is a sketch of one way of doing it that I am probably
// going to ditch in favour of another way, but I'm checking it in 
// in case I want to go back to that.


/**
 * Wraps an NDArray to provide another with fewer dimensions.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CollapseArrayImpl extends WrapperArrayImpl {

    private final NDArray baseNda;
    private final OrderedNDShape baseShape;
    private final OrderedNDShape shape;
    private final long collOrigin;
    private final long collLimit;

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
        this.collOrigin = collOrigin;
        this.collLimit = collOrigin + collDim;
        int baseNdim = baseShape.getNumDims();
        long[] baseOrigin = baseShape.getOrigin();
        long[] baseDims = baseShape.getDims();

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

        /* Calculate the shape of this array. */
        long[] origin = new long[ ndim ];
        long[] dims = new long[ ndim ];
        for ( int i = 0, j = 0; i < ndim; i++ ) {
            if ( i != axis ) {
                origin[ j ] = baseOrigin[ i ];
                dims[ j ] = baseDims[ i ];
                j++;
            }
        }
        this.shape = new OrderedNDShape( origin, dims, baseNda.getOrder() );

        /* Calculate the step between base array pixels which map to
         * the same collapsed array pixel. */
        long step = 1L;
        for ( int i = 0; i < collAxis; i++ ) {
            step *= baseDims[ i ];
        }

    }

    public OrderedNDShape getShape() {
        return shape;
    }

    public boolean isWritable() {
        return false;
    }

    public AccessImpl getAccess() throws IOException {
        private final int chunkSize = ChunkStepper.defaultChunkSize;
        private Object baseBuf = getType().newArray( chunkSize );

        if ( baseType == Type.BYTE ) {
            assert type == Type.INTEGER;
            return new CollapseAccessImpl() {
                protected void fillWithZeros( Object buffer, int start,
                                              int size ) {
                    Arrays.fill( (int[]) buffer, start, start + size, 0 );
                }
                protected void accumulate( Object inBuf, int inOff,
                                           Object outBuf, int outOff ) {
                    ((int[]) outBuf)[ inOff ] += ((byte[]) inBuf)[ inOff ];
                }
            };
        }
        else if ( baseType == Type.SHORT ) {
            assert type == Type.INTEGER;
            return new CollapseAccessImpl() {
                protected void fillWithZeros( Object buffer, int start,
                                              int size ) {
                    Arrays.fill( (int[]) buffer, start, start + size, 0 );
                }
                protected void accumulate( Object inBuf, int inOff,
                                           Object outBuf, int outOff ) {
                    ((int[]) outBuf)[ outOff ] += ((short[]) inBuf)[ inOff ];
                }
            };
        }
        else if ( baseType == Type.INTEGER ) {
            assert type == Type.INTEGER;
            return new CollaseAccessImpl() {
                protected void fillWithZeros( Object buffer, int start,
                                              int size ) {
                    Arrays.fill( (int[]) buffer, start, start + size, 0 );
                }
                protected void accumulate( Object inBuf, int inOff,
                                           Object outBuf, int outOff ) {
                    ((int[]) outBuf)[ outOff ] += ((int[]) inBuf)[ inOff ];
                }
            };
        }
        else if ( baseType == Type.FLOAT ) {
            assert type == Type.FLOAT;
            return new CollapseAccessImpl() {
                protected void fillWithZeros( Object buffer, int start, 
                                              int size ) {
                    Arrays.fill( (float[]) buffer, start, start + size, 0.0f );
                }
                protected void accumulate( Object inBuf, int inOff,
                                           Object outBuf, int outOff ) {
                    ((float[]) outBuf)[ outOff ] += ((float[]) inBuf)[ inOff ];
                }
            };
        }
        else if ( baseType == Type.DOUBLE ) {
            assert type == Type.DOUBLE;
            return new CollapseAccessImpl() {
                protected void fillWithZeros( Object buffer, int start,
                                              int size ) {
                    Arrays.fill( (double[]) buffer, start, start + size, 0.0 );
                }
                protected void accumulate( Object inBuf, int inOff,
                                           Object outBuf, int outOff ) {
                    ((double[]) outBuf)[ outOff ] += ((double[])inBuf)[ inOff ];
                }
            };
        }
        else {
            assert false;
        }
    }


    private abstract class CollapseAccessImpl implements AccessImpl {
        private ArrayAccess baseAcc;
        private long[] basePos;

        abstract protected void fillWithZeros( Object buffer, 
                                               int start, int size );
        abstract protected void accumulate( Object inBuf, int inOff, 
                                            Object outBuf, int outOff );

        public CollapseAccessImpl() {
            baseAcc = baseNda.getAccess();
            basePos = (long[]) baseOrigin().clone();
            basePos[ collAxis ] = collOrigin;
        }

        public void setOffset( long off ) throws IOException {
            long[] pos = shape.offsetToPosition( off );
            System.arraycopy( pos, 0, basePos, 0, collAxis );
            System.arraycopy( pos, collAxis, basePos, collAxis + 1, 
                              ndim - collAxis );
            assert basePos[ collAxis ] == baseOrigin[ collAxis ];
            baseAcc.setPosition( basePos );
        }

        public void read( Object buffer, int start, int size )
                throws IOException {
            fillWithZeros( buffer, start, size );
            ChunkStepper cstep = new ChunkStepper( size, chunkSize );
            int boff = 0;
            for ( ChunkStepper cstep = new ChunkStepper( size, chunkSize );
                  cstep.hasNext(); cstep.next() ) {
                int csz = cstep.getSize();
                baseAcc.read( baseBuf, 0, csz );
                int baseOff = start;
                while ( csz-- > 0 ) {

                    /* Calculate the position in the output buffer to which
                     * the next input pixel maps. */
                    int stride = 1;
                    for ( int j = 0; j < ndim; j++ ) {
                        int i = hasFitsOrder ? j : ( baseNdim - 1 - j );
                        basePos[ i ]++;
                        if ( j != collAxis ) {
                            boff += stride;
                        }
                        if ( basePos[ i ] < baseLimits[ i ] ) {
                            break;
                        }
                        else {
                            if ( j != collAxis ) {
                                stride *= dims[ i ];
                                boff -= stride;
                            }
                            basePos[ i ] = baseOrigin[ i ];
                        }
                    }

                    /* Accumulate statistics in that pixel. */
                    accumulate( buffer, boff, baseBuf, baseOff );

                    baseOff++;
                }
            }
            assert boff = start + size;
        }

        public void write( Object buffer, int start, int size ) {
            throw UnsupportedOperationException();
        }

        public void close() throws IOException {
            baseAcc.close();
        }
    }

}
