package uk.ac.starlink.ndtools;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.MutableNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;

/**
 * Block averaging task.  The algorithm used is to calculate each 
 * output pixel one at a time by iterating over all the input pixels
 * which contribute to it.  This is naive and hopelessly inefficient.
 */
class SlowBlock implements Task {

    private ExistingNdxParameter inpar;
    private NewNdxParameter outpar;
    private IntegerParameter blockpar;

    SlowBlock() {
        inpar = new ExistingNdxParameter( "in" );
        inpar.setPosition( 1 );
        inpar.setPrompt( "Input NDX" );

        outpar = new NewNdxParameter( "out" );
        outpar.setPosition( 2 );
        outpar.setPrompt( "Output NDX" );

        blockpar = new IntegerParameter( "block" );
        blockpar.setPosition( 3 );
        blockpar.setPrompt( "Size of block in pixels" );
        blockpar.setOdd();
        blockpar.setMinimum( 1 );
    }

    public Parameter[] getParameters() {
        return new Parameter[] { inpar, outpar, blockpar };
    }

    public String getUsage() {
        return "in out block";
    }

    public void invoke( Environment env ) throws TaskException {
        try {
            doInvoke( env );
        }
        catch ( IOException e ) {
            throw new TaskException( e );
        }
    }

    private void doInvoke( Environment env ) throws TaskException, IOException {

        /* Get the input and output NDXs, their image arrays, and shape 
         * and type. */
        Ndx ndx1 = inpar.ndxValue( env );
        MutableNdx template = new DefaultMutableNdx( ndx1 );
        template.setImage( ndx1.getImage() );
        Ndx ndx2 = outpar.getOutputNdx( env, template );
        NDArray im1 = ndx1.getImage();
        Requirements req1 = new Requirements( AccessMode.READ )
                           .setRandom( true );
        im1 = NDArrays.toRequiredArray( im1, req1 );
        NDArray im2 = ndx2.getImage();
        NDShape shape = im1.getShape();
        Type type = im1.getType();
        BadHandler bh1 = im1.getBadHandler();
        BadHandler bh2 = im2.getBadHandler();
        Order order2 = im2.getShape().getOrder();
        int ndim = shape.getNumDims();
        long npix = shape.getNumPixels();

        /* Get the block size and set the blocking tile dimensions. */
        int block = blockpar.intValue( env );
        int gap = ( block - 1 ) / 2;
        long[] boxDims = new long[ ndim ];
        int boxpix = 1;
        for ( int i = 0; i < ndim; i++ ) {
            boxDims[ i ] = block;
            boxpix *= block;
        }

        /* Get an iterator which will iterate over the starting pixel of
         * the box to be sampled for each pixel in the output array.
         * The shape it iterates over will be the same as the shape of
         * the output array (including its ordering scheme), but offset
         * by -gap pixels in each dimension. */
        long[] samplesOrigin = new long[ ndim ];
        long[] samplesDims = shape.getDims();
        for ( int i = 0; i < ndim; i++ ) {
            samplesOrigin[ i ] = shape.getOrigin()[ i ] - gap;
        }
        OrderedNDShape samplesSequence = 
            new OrderedNDShape( samplesOrigin, samplesDims, order2 );
        Iterator samplesIt = samplesSequence.pixelIterator();

        /* Get array accessors for the input and output image arrays. */
        ArrayAccess iacc1 = im1.getAccess();
        ArrayAccess iacc2 = im2.getAccess();

        /* We can now iterate over each position in the input and output 
         * arrays, taking a tile from the input and averaging it to 
         * produce the output.  The output pixels can be tackled in 
         * sequence, the pixel iterator we just built will provide the
         * starting point for a block around each such position in the
         * input array. */
        ChunkStepper chunkIt = new ChunkStepper( npix );
        Object buf2 = type.newArray( chunkIt.getSize() );
        Object boxBuf = type.newArray( boxpix );
        Blocker blocker = makeBlocker( type, boxBuf, buf2, bh1, bh2 );
        for ( ; chunkIt.hasNext(); chunkIt.next() ) {
            int size = chunkIt.getSize();
            for ( int i = 0; i < size; i++ ) {
                long[] boxOrigin = (long[]) samplesIt.next();
                NDShape boxTile = new NDShape( boxOrigin, boxDims ); 
                iacc1.readTile( boxBuf, boxTile );
                blocker.calculateElement( i );
            }
            iacc2.write( buf2, 0, size );
        }
        iacc1.close();
        iacc2.close();
        im1.close();
        im2.close();
    }

    private static interface Blocker {
        void calculateElement( int i );
    }

    private static Blocker makeBlocker( Type type, final Object inbuf, 
                                        final Object outbuf, BadHandler inbh, 
                                        final BadHandler outbh ) 
            throws ExecutionException {
        final int size = Array.getLength( inbuf );
        final BadHandler.ArrayHandler inAh = inbh.arrayHandler( inbuf );
        if ( type == Type.BYTE ) {
            return new Blocker() {
                byte[] inBuffer = (byte[]) inbuf;
                byte[] outBuffer = (byte[]) outbuf;
                byte badval = outbh.getBadValue().byteValue();
                public void calculateElement( int outPos ) {
                    int ngood = 0;
                    double sum = 0;
                    for ( int i = 0; i < size; i++ ) {
                        if ( ! inAh.isBad( i ) ) {
                            ngood++;
                            sum += inBuffer[ i ];
                        }
                    }
                    outBuffer[ outPos ] = 
                        ( ngood > 0 ) ? (byte) Math.round( sum / ngood )
                                      : badval;
                }
            };
        }
        else if ( type == Type.SHORT ) {
            return new Blocker() {
                short[] inBuffer = (short[]) inbuf;
                short[] outBuffer = (short[]) outbuf;
                short badval = outbh.getBadValue().shortValue();
                public void calculateElement( int outPos ) {
                    int ngood = 0;
                    double sum = 0;
                    for ( int i = 0; i < size; i++ ) {
                        if ( ! inAh.isBad( i ) ) {
                            ngood++;
                            sum += inBuffer[ i ];
                        }
                    }
                    outBuffer[ outPos ] = 
                        ( ngood > 0 ) ? (short) Math.round( sum / ngood )
                                      : badval;
                }
            };
        }
        else if ( type == Type.INT ) {
            return new Blocker() {
                int[] inBuffer = (int[]) inbuf;
                int[] outBuffer = (int[]) outbuf;
                int badval = outbh.getBadValue().intValue();
                public void calculateElement( int outPos ) {
                    int ngood = 0;
                    double sum = 0;
                    for ( int i = 0; i < size; i++ ) {
                        if ( ! inAh.isBad( i ) ) {
                            ngood++;
                            sum += inBuffer[ i ];
                        }
                    }
                    outBuffer[ outPos ] =
                        ( ngood > 0 ) ? (int) Math.round( sum / ngood )
                                      : badval;
                }
            };
        }
        else if ( type == Type.FLOAT ) {
            return new Blocker() {
                float[] inBuffer = (float[]) inbuf;
                float[] outBuffer = (float[]) outbuf;
                float badval = outbh.getBadValue().floatValue();
                public void calculateElement( int outPos ) {
                    int ngood = 0;
                    double sum = 0;
                    for ( int i = 0; i < size; i++ ) {
                        if ( ! inAh.isBad( i ) ) {
                            ngood++;
                            sum += inBuffer[ i ];
                        }
                    }
                    outBuffer[ outPos ] = 
                        ( ngood > 0 ) ? (float) ( sum / ngood ) : badval;
                }
            };
        }
        else if ( type == Type.DOUBLE ) {
            return new Blocker() {
                double[] inBuffer = (double[]) inbuf;
                double[] outBuffer = (double[]) outbuf;
                double badval = outbh.getBadValue().doubleValue();
                public void calculateElement( int outPos ) {
                    int ngood = 0;
                    double sum = 0;
                    for ( int i = 0; i < size; i++ ) {
                        if ( ! inAh.isBad( i ) ) {
                            ngood++;
                            sum += inBuffer[ i ];
                        }
                    }
                    outBuffer[ outPos ] = 
                        ( ngood > 0 ) ? (double) ( sum / ngood ) : badval;
                }
            };
        }
        else {
            throw new ExecutionException( "Unknown data type " + type );
        }
    }
}
