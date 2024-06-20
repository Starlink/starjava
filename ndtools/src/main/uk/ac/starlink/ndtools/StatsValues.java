package uk.ac.starlink.ndtools;

import java.io.IOException;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;

/**
 * Calculates statistics for an NDArray.  All stats are calculated in 
 * the constructor, and the results are accessible via final public fields
 * when it's done.
 * <p>
 * This is lifted from the StatsValues class in the Treeview package.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StatsValues {

    /** The sum of all the non-bad pixel values. */
    public final double total;

    /** The mean of all the non-bad pixel values. */
    public final double mean;

    /** The variance of all the non-bad pixel values. */
    public final double variance;

    /** The number of good pixels used. */
    public final long numGood;

    /** The minimum good-valued pixel; type reflects numeric type of array. */
    public final Number minValue;

    /** The maximum good-valued pixel; type reflects numeric type of array. */
    public final Number maxValue;

    /** The position of the minimum valued pixel. */
    public final long[] minPosition;

    /** The position of the maximum valued pixel. */
    public final long[] maxPosition;

    /**
     * Construct an object containing the statistics of a given NDArray.
     */
    public StatsValues( NDArray nda ) throws IOException {
        this( nda, new ChunkStepper( nda.getShape().getNumPixels() ) );
    }

    /**
     * Construct an object containing the statistics of a given NDArray,
     * specifying the chunk stepper.  This can be used monitor progress
     * by subclassing ChunkStepper and putting callbacks in, e.g. to
     * control a JProgressBar
     * (at time of writing there is an example of this in Treeview).
     *
     * @param  nda  the array to calculate stats for
     * @param  stepper  a stepper supplying the blocks in which the 
     *         calculation is done.  Must have getTotalLength the same
     *         as the number of pixels in <code>nda</code>
     * @throws  IOException if there is trouble reading the array
     * @throws  IllegalArgumentException if 
     *    <code>stepper.getTotalLength()!=nda.getShape().getNumPixels()</code>
     */
    public StatsValues( NDArray nda, ChunkStepper stepper ) throws IOException {
        long npix = nda.getShape().getNumPixels();
        if ( stepper.getTotalLength() != nda.getShape().getNumPixels() ) {
            throw new IllegalArgumentException( "Wrong length stepper" );
        }

        OrderedNDShape oshape = nda.getShape();
        Type type = nda.getType();

        long ngood = 0;
        double sum = 0.0;
        double sum2 = 0.0;
        long minindex = 0;
        long maxindex = 0;
        long index = 0;
        double dmin = type.maximumValue();
        double dmax = type.minimumValue();

        ArrayAccess acc = nda.getAccess();
        Object buf = type.newArray( stepper.getSize() );
        BadHandler bh = nda.getBadHandler();
        BadHandler.ArrayHandler ah = bh.arrayHandler( buf );
        for ( ; stepper.hasNext(); stepper.next() ) {
            int leng = stepper.getSize();
            long base = stepper.getBase();
            acc.read( buf, 0, leng );

            if ( type == Type.BYTE ) {
                byte[] buffer = (byte[]) buf;
                byte min = (byte) dmin;
                byte max = (byte) dmax;
                for ( int i = 0; i < leng; i++ ) {
                    if ( ! ah.isBad( i ) ) {
                        byte val = buffer[ i ];
                        ngood++;
                        sum += val;
                        sum2 += val * val;
                        if ( val > max ) { max = val; maxindex = base + i; }
                        if ( val < min ) { min = val; minindex = base + i; }
                    }
                }
                dmin = (double) min;
                dmax = (double) max;
            }

            else if ( type == Type.SHORT ) {
                short[] buffer = (short[]) buf;
                short min = (short) dmin;
                short max = (short) dmax;
                for ( int i = 0; i < leng; i++ ) {
                    if ( ! ah.isBad( i ) ) {
                        short val = buffer[ i ];
                        ngood++;
                        sum += val;
                        sum2 += val * val;
                        if ( val > max ) { max = val; maxindex = base + i; }
                        if ( val < min ) { min = val; minindex = base + i; }
                    }
                }
                dmin = (double) min;
                dmax = (double) max;
            }

            else if ( type == Type.INT ) {
                int[] buffer = (int[]) buf;
                int min = (int) dmin;
                int max = (int) dmax;
                for ( int i = 0; i < leng; i++ ) {
                    if ( ! ah.isBad( i ) ) {
                        int val = buffer[ i ];
                        ngood++;
                        sum += val;
                        sum2 += val * val;
                        if ( val > max ) { max = val; maxindex = base + i; }
                        if ( val < min ) { min = val; minindex = base + i; }
                    }
                }
                dmin = (double) min;
                dmax = (double) max;
            }

            else if ( type == Type.FLOAT ) {
                float[] buffer = (float[]) buf;
                float min = (float) dmin;
                float max = (float) dmax;
                for ( int i = 0; i < leng; i++ ) {
                    if ( ! ah.isBad( i ) ) {
                        float val = buffer[ i ];
                        ngood++;
                        sum += val;
                        sum2 += val * val;
                        if ( val > max ) { max = val; maxindex = base + i; }
                        if ( val < min ) { min = val; minindex = base + i; }
                    }
                }
                dmin = (double) min;
                dmax = (double) max;
            }

            else if ( type == Type.DOUBLE ) {
                double[] buffer = (double[]) buf;
                double min = dmin;
                double max = dmax;
                for ( int i = 0; i < leng; i++ ) {
                    if ( ! ah.isBad( i ) ) {
                        double val = buffer[ i ];
                        ngood++;
                        sum += val;
                        sum2 += val * val;
                        if ( val > max ) { max = val; maxindex = base + i; }
                        if ( val < min ) { min = val; minindex = base + i; }
                    }
                }
                dmin = (double) min;
                dmax = (double) max;
            }
        }
        acc.close();

        /* Calculate actual statistics. */
        total = sum;
        mean = sum / ngood;
        variance = ( sum2 / ngood ) - mean * mean;
        minPosition = oshape.offsetToPosition( minindex );
        maxPosition = oshape.offsetToPosition( maxindex );
        numGood = ngood;
        if ( dmin > dmax ) {
            minValue = null;
            maxValue = null;
        }
        else if ( type == Type.BYTE ) {
            minValue = Byte.valueOf( (byte) dmin );
            maxValue = Byte.valueOf( (byte) dmax );
        }
        else if ( type == Type.SHORT ) {
            minValue = Short.valueOf( (short) dmin );
            maxValue = Short.valueOf( (short) dmax );
        }
        else if ( type == Type.INT ) {
            minValue = Integer.valueOf( (int) dmin );
            maxValue = Integer.valueOf( (int) dmax );
        }
        else if ( type == Type.FLOAT ) {
            minValue = Float.valueOf( (float) dmin );
            maxValue = Float.valueOf( (float) dmax );
        }
        else if ( type == Type.DOUBLE ) {
            minValue = Double.valueOf( (double) dmin );
            maxValue = Double.valueOf( (double) dmax );
        }
        else {
            throw new AssertionError();
        }
    }
}
