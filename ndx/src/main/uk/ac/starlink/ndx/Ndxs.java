package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.AccessImpl;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.TypeConverter;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.Converter;
import uk.ac.starlink.array.ConvertArrayImpl;
import uk.ac.starlink.array.Function;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.array.TypeConverter;
import uk.ac.starlink.array.WrapperArrayImpl;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.WinMap;

/**
 * Utility methods for <code>Ndx</code> manipulation.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Ndxs {

    private final static int BUFSIZ = ChunkStepper.defaultChunkSize;

    /**
     * Private dummy sole constructor.
     */
    private Ndxs() {}

    /**
     * Returns a view of the Image array of an Ndx with any masking 
     * implied by its Quality array taken care of, and additional 
     * constraints supplied by a <code>Requirements</code> object.
     *
     * @param  ndx  the NDX whose image is sought
     * @param  req  additional requirements for the returned array.
     *              May be <code>null</code>
     * @return  an NDArray representing the masked image data
     * @throws  IOException  if an I/O error occurs
     * @see    #maskArray
     */
    public static NDArray getMaskedImage( Ndx ndx, Requirements req ) 
            throws IOException {
        if ( ndx.hasQuality() && ndx.getBadBits() != 0 ) {
            return maskArray( ndx.getImage(), ndx.getQuality(), 
                              ndx.getBadBits(), req );
        }
        else {
            return NDArrays.toRequiredArray( ndx.getImage(), req );
        }
    }

    /**
     * Returns a view of the Image array of an Ndx with any masking
     * implied by its Quality array taken care of.
     *
     * @param  ndx  the NDX whose image is sought
     * @return  an NDArray representing the masked image data
     * @throws  IOException  if an I/O error occurs
     * @see    #maskArray
     */
    public static NDArray getMaskedImage( Ndx ndx ) 
            throws IOException {
        return getMaskedImage( ndx, null );
    }

    /**
     * Returns a view of the Variance array of an Ndx with any masking 
     * implied by its Quality array taken care of, and additional 
     * constraints supplied by an <code>Requirements</code> object.
     *
     * @param  ndx  the NDX whose variance is sought
     * @param  req  additional requirements for the returned array.
     *              May be <code>null</code>
     * @return  an NDArray representing the masked variance data
     * @throws  IOException  if an I/O error occurs
     * @see    #maskArray
     */
    public static NDArray getMaskedVariance( Ndx ndx, Requirements req ) 
            throws IOException {
        if ( ndx.hasQuality() && ndx.getBadBits() != 0 ) {
            return maskArray( ndx.getVariance(), ndx.getQuality(), 
                              ndx.getBadBits(), req );
        }
        else {
            return NDArrays.toRequiredArray( ndx.getVariance(), req );
        }
    }

    /**
     * Returns a view of the Variance array of an Ndx with any masking
     * implied by its Quality array taken care of.
     * 
     * @param  ndx  the NDX whose variance is sought
     * @return  an NDArray representing the masked variance data
     * @throws  IOException  if an I/O error occurs
     * @see    #maskArray
     */
    public static NDArray getMaskedVariance( Ndx ndx ) 
            throws IOException {
        return getMaskedVariance( ndx, null );
    }

    /**
     * Returns a view of the Errors of an Ndx with any masking implied
     * by its Quality array taken care of, and additional constraints
     * supplied by a <code>Requirements</code> object.
     * The resulting object has pixels which are the square roots of
     * those in the array returned by 
     * {@link #getMaskedVariance(Ndx,Requirements)}.
     *
     * @param  ndx  the NDX whose errors are sought
     * @param  req  additional requirements for the returned array.
     *              May be <code>null</code>
     * @return  an NDArray representing the square root of the variance data
     * @throws  IOException  if an I/O error occurs
     * @see    #maskArray
     */
    public static NDArray getMaskedErrors( Ndx ndx, Requirements req ) 
            throws IOException {
        req = ( req != null ) ? req : new Requirements();
        Requirements vreq = (Requirements) req.clone();
        vreq.setBadHandler( null );
        vreq.setType( null );
        NDArray var = getMaskedVariance( ndx, vreq );
        Type type = ( req.getType() != null ) ? req.getType() : var.getType();
        BadHandler handler;
        if ( req.getBadHandler() != null ) {
            handler = req.getBadHandler();
        }
        else if ( type == var.getType() ) {
            handler = var.getBadHandler();
        }
        else {
            handler = BadHandler.getHandler( type, type.defaultBadValue() );
        }
        Function rooter = new Function() {
            public double forward( double x ) { return Math.sqrt( x ); }
            public double inverse( double y ) { return y * y; }
        };
        Converter tconv = new TypeConverter( var.getType(), var.getBadHandler(),
                                             type, handler, rooter );
        return new BridgeNDArray( new ConvertArrayImpl( var, tconv ) );
    }

    /**
     * Returns a view of the Errors of an Ndx with any masking implied
     * by its Quality array taken care of.
     * The resulting object has pixels which are the square roots of
     * those in the array returned by 
     * {@link #getMaskedVariance(Ndx)}.
     *
     * @param  ndx  the NDX whose errors are sought
     * @return  an NDArray representing the square root of the masked 
     *          variance data
     * @throws  IOException  if an I/O error occurs
     * @see    #maskArray
     */
    public static NDArray getMaskedErrors( Ndx ndx ) 
            throws IOException {
        return getMaskedErrors( ndx, null );
    }

    /**
     * Applies quality masking to an NDArray based on another NDArray
     * representing quality values.
     * The pixels of the returned array are the same as those of the target,
     * except where a bitwise AND of the <code>badbits</code> mask and the 
     * corresponding pixel of the quality array is non-zero, in which
     * case they have the bad value.
     * A <code>Requirements</code> object may be supplied to specify 
     * additional required characteristics of the returned array.
     * <p>
     * This method does the work for the various Ndx array masking 
     * methods in this class.
     *
     * @param  target  the target NDArray to be masked
     * @param  quality  an NDArray of an integer type 
     * @param  badbits  the quality mask
     * @param  req   additional requirements on the returned object.
     *               May be <code>null</code>
     * @return  an NDArray based on the target array <code>target</code> but
     *          with bad pixels where the indicated by <code>quality</code>
     *          May or may not be the same object as <code>target</code>
     * @throws  IOException  if an I/O error occurs
     */
    public static NDArray maskArray( NDArray target, NDArray quality,
                                     final int badbits, Requirements req ) 
            throws IOException {

        /* Prepare a requirements object for the arrays.  This is necessary
         * to ensure that the pixel sequenc matches for the target and
         * quality arrays. */
        Requirements treq = ( req != null ) ? (Requirements) req.clone()
                                            : new Requirements();
        if ( treq.getWindow() == null ) {
            treq.setWindow( target.getShape() );
        }
        if ( treq.getOrder() == null ) {
            treq.setOrder( target.getShape().getOrder() );
        }
        OrderedNDShape oshape = 
            new OrderedNDShape( treq.getWindow(), treq.getOrder() );

        final Type qtype = quality.getType();
        if ( qtype.isFloating() ) {
            throw new IllegalArgumentException( 
                "Quality array " + quality + 
                " may not have floating point type" );
        }
        Requirements qreq = new Requirements( AccessMode.READ )
                           .setWindow( oshape )
                           .setOrder( oshape.getOrder() );

        /* Get the target and quality arrays to use; these are matched in
         * pixel sequence and tailored to any supplied requirements. */
        final NDArray tbase = NDArrays.toRequiredArray( target, treq );
        final NDArray qbase = NDArrays.toRequiredArray( quality, qreq );
        final Masker masker = getMasker( qtype, badbits, 
                                         tbase.getBadHandler() );

        /* Now construct an NDArray which behaves just like the target
         * array, except that the accessor's read method modifies
         * data according to quality values. */
        ArrayImpl maskedImpl = new WrapperArrayImpl( tbase ) {
            public boolean canMap() {
                return false;
            }
            public boolean isRandom() {
                return tbase.isRandom() && qbase.isRandom();
            }
            public boolean isWritable() {
                return tbase.isWritable() && qbase.isWritable();
            }
            public boolean multipleAccess() {
                return tbase.multipleAccess() && qbase.multipleAccess();
            }
            public void close() throws IOException {
                super.close();
                qbase.close();
            }
            public AccessImpl getAccess() throws IOException {
                final ArrayAccess tacc = tbase.getAccess();
                final ArrayAccess qacc = qbase.getAccess();
                final Object qbuf = qtype.newArray( BUFSIZ );
                return new AccessImpl() {
                    public void setOffset( long off ) throws IOException {
                        tacc.setOffset( off );
                        qacc.setOffset( off );
                    }
                    public void read( Object tbuf, int start, int size )
                            throws IOException {
                        if ( size <= BUFSIZ ) {  // optimisation
                            tacc.read( tbuf, 0, size );
                            qacc.read( qbuf, 0, size );
                            masker.mask( qbuf, tbuf, start, size );
                        }
                        else {
                            for ( ChunkStepper cIt = 
                                      new ChunkStepper( (long) size, BUFSIZ );
                                  cIt.hasNext(); cIt.next() ) {
                                int leng = cIt.getSize();
                                int base = (int) cIt.getBase();
                                qacc.read( qbuf, 0, leng );
                                masker.mask( qbuf, tbuf, base, leng );
                            }
                        }
                    }
                    public void write( Object tbuf, int start, int size ) 
                            throws IOException {
                        tacc.write( tbuf, start, size );
                    }
                    public void close() throws IOException {
                        tacc.close();
                        qacc.close();
                    }
                };
            }
        };
        return new BridgeNDArray( maskedImpl );
    }

    /**
     * Private interface for applying a quality array to a target array.
     */
    private static interface Masker {
        /**
         * Mask a target array using a quality array.  The masking bit
         * pattern is not specified in this method.
         *
         * @param  qbuf  a primitive array providing quality values in elements
         *               0..size
         * @param  tbuf  a primitive array providing target values in elements
         *               start..start+size
         * @param  start first element to mask in tbuf
         * @param  size  number of elements to mask
         */
        void mask( Object qbuf, Object tbuf, int start, int size );
    }

    /**
     * Obtain a masker object for a given bitmask and badhandler.
     *
     * @param   qtype  the numerical type of the quality array
     * @param   badbits  the bad bits bitmask pattern
     * @param   thandler  the bad value handler for the target array
     *                    (implies its numerical type)
     */
    private static Masker getMasker( final Type qtype, final int badbits,
                                     final BadHandler thandler ) {
        if ( qtype == Type.BYTE ) {
            return new Masker() {
                byte bitmask = (byte) badbits;
                public void mask( Object qb, Object tbuf, 
                                  int start, int size ) {
                    byte[] qbuf = (byte[]) qb;
                    int tpos = start;
                    int qpos = 0;
                    while ( size-- > 0 ) {
                        if ( ( qbuf[ qpos ] & bitmask ) != (byte) 0 ) {
                            thandler.putBad( tbuf, tpos );
                        }
                        tpos++;
                        qpos++;
                    }
                }
            };
        }
        else if ( qtype == Type.SHORT ) {
            return new Masker() {
                short bitmask = (short) badbits;
                public void mask( Object qb, Object tbuf, 
                                  int start, int size ) {
                    short[] qbuf = (short[]) qb;
                    int tpos = start;
                    int qpos = 0;
                    while ( size-- > 0 ) {
                        if ( ( qbuf[ qpos ] & bitmask ) != (short) 0 ) {
                            thandler.putBad( tbuf, tpos );
                        }
                        tpos++;
                        qpos++;
                    }
                }
            };
        }
        else if ( qtype == Type.INT ) {
            return new Masker() {
                int bitmask = badbits;
                public void mask( Object qb, Object tbuf,
                                  int start, int size ) {
                    int[] qbuf = (int[]) qb;
                    int tpos = start;
                    int qpos = 0;
                    while ( size-- > 0 ) {
                        if ( ( qbuf[ qpos ] & bitmask ) != 0 ) {
                            thandler.putBad( tbuf, tpos );
                        }
                        tpos++;
                        qpos++;
                    }
                }
            };
        }
        else {
            throw new AssertionError( "Wrong type " + qtype );
        }
    }

    /**
     * Unconditionally returns world coordinate system information for an
     * NDX as a {@link uk.ac.starlink.ast.FrameSet}.
     * If <code>ndx</code> has a WCS component it is generated from that,
     * otherwise a suitable default one is returned.
     *
     * @param  ndx  the NDX for which WCS are required
     */
    public static FrameSet getAst( Ndx ndx ) {
        if ( ndx.hasWCS() ) {
            return ndx.getAst();
        }
        else {
            return getDefaultAst( ndx );
        }
    }

    /**
     * Returns a default AST <code>FrameSet</code> for an Ndx.
     * This has GRID and PIXEL Frames, such that when the unit hypercube
     * (having coordinates <i>x<sub>i</sub></i> in the range
     * 0&lt;=<i>x<sub>i</sub></i>..1 in each dimension <i>i</i>) 
     * is transformed into the PIXEL frame it
     * becomes the hypercube with coordinates in the range
     * <i>Origin<sub>i</sub>-1&lt;=x<sub>i</sub>&lt;=Origin<sub>i</sub></i>,
     * where <i>Origin</i> is the origin of the Image array component.
     *
     * @param   ndx  the Ndx for which to find the default FrameSet
     * @return  the default FrameSet for <code>ndx</code>
     */
    public static FrameSet getDefaultAst( Ndx ndx ) {
        NDArray image = ndx.getImage();
        NDShape imshape = image.getShape();
        int ndim = imshape.getNumDims();
        Frame gridfrm = new Frame( ndim );
        gridfrm.setDomain( "GRID" );
        Frame imfrm = new Frame( ndim );
        imfrm.setDomain( "PIXEL" );
        FrameSet ast = new FrameSet( gridfrm );
        Mapping gpmap = translateMap( imshape );
        ast.addFrame( FrameSet.AST__BASE, gpmap, imfrm );
        return ast;
    }

    private static Mapping translateMap( NDShape shape ) {
        int ndim = shape.getNumDims();
        long[] origin = shape.getOrigin();
        double[] ina = new double[ ndim ];
        double[] inb = new double[ ndim ];
        double[] outa = new double[ ndim ];
        double[] outb = new double[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            ina[ i ] = 0.5;
            inb[ i ] = 1.5;
            outa[ i ] = (double) ( origin[ i ] - 1.0 );
            outb[ i ] = (double) origin[ i ];
        }
        return new WinMap( ndim, ina, inb, outa, outb );
    }


}
