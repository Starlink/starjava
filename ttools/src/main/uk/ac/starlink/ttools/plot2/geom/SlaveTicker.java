package uk.ac.starlink.ttools.plot2.geom;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.BasicTicker;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Tick;
import uk.ac.starlink.ttools.plot2.TickRun;
import uk.ac.starlink.ttools.plot2.Ticker;

/**
 * Ticker implementation that provides ticks for a supplied function
 * based on a separate master axis.
 * This is suitable when two different axes are attached to the same
 * dimension of a plot, providing different numerical mappings of
 * the same underlying quantity.
 *
 * @author   Mark Taylor
 * @since    5 Dec 2022
 */
public class SlaveTicker implements Ticker {

    private final Axis masterAxis_;
    private final DoubleUnaryOperator masterToSlaveFunc_;
    private final Ticker basicTicker_;
    private Lut lut_;

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * Constructor.
     * The assumption is that the supplied mapping function is monotonic
     * and fairly well-behaved over the relevant range.
     * If not, the ticks might not come out well.
     *
     * @param  masterAxis  master axis
     * @param  masterToSlaveFunc   function mapping values on the master axis
     *                             to values on the slave axis
     * @param  basicTicker   ticker than can provide labels on a given range
     */
    public SlaveTicker( Axis masterAxis,
                        DoubleUnaryOperator masterToSlaveFunc,
                        Ticker basicTicker ) {
        masterAxis_ = masterAxis;
        masterToSlaveFunc_ = masterToSlaveFunc;
        basicTicker_ = basicTicker;
    }

    public TickRun getTicks( double masterDlo, double masterDhi,
                             boolean withMinor,
                             Captioner captioner, Orientation[] orients,
                             int npix, double crowding ) {
        double slaveD1 = masterToSlave( masterDlo );
        double slaveD2 = masterToSlave( masterDhi );
        if ( !PlotUtil.isFinite( slaveD1 ) || !PlotUtil.isFinite( slaveD2 ) ||
             slaveD1 == slaveD2 ) {
            return new TickRun( new Tick[ 0 ], orients[ 0 ] );
        }
        double slaveDlo = slaveD1 < slaveD2 ? slaveD1 : slaveD2;
        double slaveDhi = slaveD1 < slaveD2 ? slaveD2 : slaveD1;
        TickRun slaveTickRun =
            basicTicker_.getTicks( slaveDlo, slaveDhi, withMinor,
                                   captioner, orients, npix, crowding );
        Tick[] slaveTicks = slaveTickRun.getTicks();
        Orientation slaveOrient = slaveTickRun.getOrientation();
        int ntick = slaveTicks.length;
        Tick[] outTicks = new Tick[ ntick ];
        for ( int it = 0; it < ntick; it++ ) {
            Tick slaveTick = slaveTicks[ it ];
            outTicks[ it ] = new Tick( slaveToMaster( slaveTick.getValue() ),
                                       slaveTick.getLabel() );
        }
        return new TickRun( outTicks, slaveOrient );
    }

    /**
     * Maps a value on the master axis to a value on the slave axis.
     *
     * @param  masterValue  master axis value
     * @return  slave axis value
     */
    public double masterToSlave( double masterValue ) {
        return masterToSlaveFunc_.applyAsDouble( masterValue );
    }

    /**
     * Maps a value on the slave axis to a value on the master axis.
     *
     * @param  slaveValue  slave axis value
     * @return  master axis value
     */
    public double slaveToMaster( double slaveValue ) {
        return masterAxis_.graphicsToData( getLut().lookupIndex( slaveValue ) );
    }

    /**
     * Returns a lookup table for mapping graphics coordinates
     * to slave axis coordinates over the graphics coordinate range
     * of the axis.
     *
     * @return   lazily created graphics to slave data lookup function
     */
    private Lut getLut() {
        if ( lut_ == null ) {
            int[] glims = masterAxis_.getGraphicsLimits();
            lut_ = Lut.createLut( ig -> masterToSlave( masterAxis_
                                                      .graphicsToData( ig ) ),
                                  glims[ 0 ], glims[ 1 ] );
        }
        return lut_;
    }

    /**
     * Creates a SlaveTicker instance with automatic selection of basic ticker.
     *
     * @param  masterAxis  master axis
     * @param  masterToSlaveFunc   function mapping values on the master axis
     *                             to values on the slave axis
     * @return  new ticker
     */
    public static SlaveTicker
            createTicker( Axis masterAxis,
                          DoubleUnaryOperator masterToSlaveFunc ) {
        return new SlaveTicker( masterAxis, masterToSlaveFunc,
                                chooseBasicTicker( masterAxis,
                                                   masterToSlaveFunc ) );
    }

    /**
     * Returns a suitable basic ticker for a given master to slave
     * mapping function.  Either linear or logarithmic ticker will
     * be returned, depending on whether the function looks roughly
     * linear or not over the given range.
     *
     * @param  masterAxis  master axis
     * @param  masterToSlaveFunc   function mapping values on the master axis
     *                             to values on the slave axis
     * @return   basic ticker instance, linear or logarithmic
     */
    private static Ticker
            chooseBasicTicker( Axis masterAxis,
                               DoubleUnaryOperator masterToSlaveFunc ) {
        int[] glims = masterAxis.getGraphicsLimits();
        int glo = glims[ 0 ];
        int ghi = glims[ 1 ];
        IntToDoubleFunction sfunc = g ->
            masterToSlaveFunc.applyAsDouble( masterAxis.graphicsToData( g ) );
        double s1 = sfunc.applyAsDouble( glo );
        double s2 = sfunc.applyAsDouble( ghi );
        double slo = s1 < s2 ? s1 : s2;
        double shi = s1 < s2 ? s2 : s1;
        double smid = sfunc.applyAsDouble( ( ghi + glo ) / 2 );

        /* Any negative values involved, it has to be linear. */
        if ( slo <= 0 || shi <= 0 || smid <= 0 ) {
            return BasicTicker.LINEAR;
        }

        /* Otherwise look at the linear and logarithmic mid points of the
         * slave axis in the covered range. */
        double linearMid = ( smid - slo ) / shi;
        double logMid = Math.log( smid / slo ) / Math.log( shi / slo );

        /* If the linear midpoint is about in the middle of the axis,
         * use linear. */
        if ( Math.abs( linearMid - 0.5 ) < 0.1 ) {
            return BasicTicker.LINEAR;
        }

        /* Otherwise if the logarithmic midpoint is closer to the middle
         * than the linear one, use logarithmic. */
        else if ( Math.abs( logMid - 0.5 ) < Math.abs( linearMid - 0.5 ) ) {
            return BasicTicker.LOG;
        }

        /* Otherwise it's probably some weird function, go back to linear. */
        else {
            return BasicTicker.LINEAR;
        }
    }

    /**
     * Lookup table suitable for reverse lookups of monotonic functions
     * over a small range of integers.
     */
    private static class Lut {

        private final boolean isFlip_;
        private final double[] values_;
        private final int i0_;
        private final int length_;

        /**
         * Constructor.
         *
         * @param  values  tabulated values, in ascending order
         * @param  i0   index corresponding to first element of values
         * @param  isFlip  true iff values table has been reversed
         */
        private Lut( double[] values, int i0, boolean isFlip ) {
            values_ = values;
            i0_ = i0;
            isFlip_ = isFlip;
            length_ = values.length;
        }

        /**
         * Returns the index value corresponding to a given tabulated value.
         * The output corresponds to the integer scale,
         * but the fractional part indicates interpolation.
         *
         * @param  value  tabulated value to look up
         * @return   interpolated integer scale value
         */
        public double lookupIndex( double value ) {
            double rawIndex = rawLookup( value );
            return i0_ + ( isFlip_ ? length_ - 1 - rawIndex : rawIndex );
        }

        /**
         * Performs a reverse lookup for for a value and returns the
         * index into the tabulated values array, with a fractional part
         * indicating interpolation.
         *
         * @param  value  tabulated value to look up
         * @return  interpolated index into values array
         */
        private double rawLookup( double value ) {
            int ip = Arrays.binarySearch( values_, value );
            if ( ip >= 0 ) {
                return ip;
            }
            else if ( ip == -1 ) {
                return 0;
            }
            else if ( ip == - length_ - 1 ) {
                return length_ - 1;
            }
            else {
                int ig0 = - ip - 1;
                int ig1 = ig0 + 1;
                if ( ig1 < length_ ) {
                    double v0 = values_[ ig0 ];
                    double v1 = values_[ ig1 ];
                    double frac = ( value - v0 ) / ( v1 - v0 );
                    return ig0 + frac;
                }
                else {
                    return ig0;
                }
            }
        }

        /**
         * Constructs a Lut.
         * Output will only be reliable between the given integer limits.
         * The integer limits also define the required resources,
         * so should not be too large.
         *
         * @param  mapping  mapping from integer scale to tabulated values
         * @param  ilo      lower bound of integer scale
         * @param  ihi      upper bound of integer scale
         */
        public static Lut createLut( IntToDoubleFunction mapping,
                                     int ilo, int ihi ) {
            boolean isFlip = mapping.applyAsDouble( ilo )
                           > mapping.applyAsDouble( ihi );
            int n = ihi - ilo + 1;
            double[] values = new double[ n ];
            boolean isAscending = true;
            for ( int i = 0; i < n; i++ ) {
                int j = ilo + ( isFlip ? n - 1 - i : i );
                values[ i ] = mapping.applyAsDouble( j );
                if ( i > 0 && values[ i ] < values[ i - 1 ] ) {
                    isAscending = false;
                }
            }
            if ( ! isAscending ) {
                logger_.severe( "Non-monotonic lookup table "
                              + "likely to cause trouble");
            }
            return new Lut( values, ilo, isFlip );
        }
    }
}
