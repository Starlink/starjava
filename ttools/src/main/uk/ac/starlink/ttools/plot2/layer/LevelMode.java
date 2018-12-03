package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines a strategy for calculating contour level values from an 
 * array of data.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2013
 */
@Equality
public abstract class LevelMode {

    private final String name_;
    private final String description_;

    /** Fixed count of extreme values to exclude for clipping. */
    private static final int NCLIP = 16;

    /**
     * Linear scaling - level values are equally spaced.
     */
    public static LevelMode LINEAR =
            new LevelMode( "linear", "levels are equally spaced" ) {
        public double[] calculateLevels( NumberArray array, int nLevel,
                                         double offset, boolean isCounts ) {
            return calculateLinearLevels( array, nLevel, offset, isCounts );
        }
    };

    /**
     * Logarithmic scaling - level logarithms are equally spaced
     */
    public static LevelMode LOG =
            new LevelMode( "log", "level logarithms are equally spaced" ) {
        public double[] calculateLevels( NumberArray array, int nLevel,
                                         double offset, boolean isCounts ) {
            return calculateLogLevels( array, nLevel, offset, isCounts );
        }
    };

    /**
     * Equal-area scaling - levels are spaced to provide equally sized
     * inter-contour regions.
     */
    public static LevelMode EQU =
            new LevelMode( "equal",
                           "levels are spaced to provide equal-area "
                         + "inter-contour regions" ) {
        public double[] calculateLevels( NumberArray array, int nLevel,
                                         double offset, boolean isCounts ) {
            return calculateEquivLevels( array, nLevel, offset, isCounts );
        }
    };

    /** Known level mode instances. */
    public static final LevelMode[] MODES = { LINEAR, LOG, EQU, };

    /**
     * Constructor.
     *
     * @param   name  mode name
     * @param   description  short description of mode
     */
    protected LevelMode( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Calculates the contour levels for a given data array.
     *
     * @param   array  data array; NaN elements are permitted and ignored
     * @param   nLevel   number of requested levels; actual level count
     *                   may not be the same as this depending on data
     * @param   offset  offset from zero of the value of the first contour,
     *                  expected in the range 0..1;
     *                  adjusting this will sweep contours over all positions
     * @param   isCounts  true if the values are counts rather than
     *                    continuously varying; if true, some adjustments
     *                    are made on the basis of the assumption that
     *                    differences of scale smaller than 1 don't make
     *                    much sense
     */
    public abstract double[] calculateLevels( NumberArray array, int nLevel,
                                              double offset, boolean isCounts );

    /**
     * Returns a short description of this mode.
     *
     * @return   description
     */
    public String getDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Does the work for calculating linearly spaced levels.
     *
     * @param   data array
     * @param   requested number of levels
     * @param   offset  normalised offset of first level
     * @param   isCounts  true if the values are counts rather than
     *                    continuously varying
     * @return  level array
     */
    private static double[] calculateLinearLevels( NumberArray array,
                                                   int nLevel, double offset,
                                                   boolean isCounts ) {
        double[] limits = getCutLimits( array, NCLIP, false );
        double min = limits[ 0 ];
        double max = limits[ 1 ];
        if ( isCounts ) {
            min = Math.max( 1, min );
            max = Math.max( 1, max );
        }
        if ( ! ( max > min ) ) {
            return new double[ 0 ];
        }
        double step = ( max - min ) / nLevel;
        if ( isCounts ) {
            step = Math.max( 1, step );
        }
        double[] levels = new double[ nLevel ];
        for ( int il = 0; il < nLevel; il++ ) {
            levels[ il ] = min + step * ( il + offset );
        }
        return levels;
    }

    /**
     * Does the work for calculating logarithmically spaced levels.
     *
     * @param   array  data array
     * @param   nLevel  requested number of levels
     * @param   offset  normalised offset of first level
     * @param   isCounts  true if the values are counts rather than
     *                    continuously varying
     * @return  level array
     */
    private static double[] calculateLogLevels( NumberArray array,
                                                int nLevel, double offset,
                                                boolean isCounts ) {
        double[] limits = getCutLimits( array, NCLIP, true );
        double min = isCounts ? Math.max( 1, limits[ 0 ] ) : limits[ 0 ];
        double max = isCounts ? Math.max( 1, limits[ 1 ] ) : limits[ 1 ];
        if ( ! ( max > min ) ) {
            return new double[ 0 ];
        }
        double step = ( Math.log( max ) - Math.log( min ) ) / nLevel;
        double[] levels = new double[ nLevel ];
        for ( int il = 0; il < nLevel; il++ ) {
            levels[ il ] = min * Math.exp( step * ( il + offset ) );
        }
        return levels;
    }

    /**
     * Does the work for calculating equal-area levels.
     * @param   array  data array
     * @param   nLevel  requested number of levels
     * @param   offset  normalised offset of first level
     * @param   isCounts  true if the values are counts rather than
     *                    continuously varying
     *
     * @return  level array
     */
    private static double[] calculateEquivLevels( NumberArray array,
                                                  int nLevel, double offset,
                                                  boolean isCounts ) {

        /* Sort the pixel values. */
        int npix = array.getLength();
        float[] values = new float[ npix ];
        int ngood = 0;
        for ( int ip = 0; ip < npix; ip++ ) {
            double v = array.getValue( ip );
            if ( ! Double.isNaN( v ) && ( ! isCounts || v > 0 ) ) {
                values[ ngood++ ] = (float) v;
            }
        }
        if ( ngood == 0 ) {
            return new double[ 0 ];
        }
        Arrays.sort( values, 0, ngood );

        /* Populate the levels by picking equally spaced distances along
         * the sorted array.  There is a threshold for the smallest
         * pixel value difference; if the above strategy delivers a
         * pixel value difference smaller than that, artificially boost
         * it to the threshold, and adjust the subsequent levels starting
         * from there. */
        double[] levels = new double[ nLevel ];
        int lastIndex = 0;
        double lastLevel = 0;
        for ( int il = 0; il < nLevel; il++ ) {
            int index =
                (int) ( lastIndex
                      + ( ngood - lastIndex ) * 1. / ( nLevel + offset - il ) );
            index = Math.min( index, ngood - 1 );
            double level = values[ index ];
            if ( isCounts && level - lastLevel < 1 ) {
                level = lastLevel + 1;
                index = Arrays.binarySearch( values, 0, ngood, (float) level );
                if ( index < 0 ) {
                    index = - ( index + 1 );
                }
            }
            levels[ il ] = level;
            lastIndex = index;
            lastLevel = level;
        }
        return levels;
    }

    /**
     * Determines the clipped data range of an array.
     * A fixed number of outliers at the top and bottom is excluded.
     *
     * @param  array  data array
     * @param  nExclude  number of extreme value to exclude at each end
     * @param  isLog  true if only positive values are acceptable
     * @return   2-element array giving (lower,upper) clipped range
     */
    private static double[] getCutLimits( NumberArray array, int nExclude,
                                          boolean isLog ) {
        double[] tops = new double[ nExclude + 1 ];
        double[] bots = new double[ nExclude + 1 ];
        Arrays.fill( tops, Double.NEGATIVE_INFINITY );
        Arrays.fill( bots, Double.POSITIVE_INFINITY );
        int np = array.getLength();
        for ( int ip = 0; ip < np; ip++ ) {
            double c = array.getValue( ip );
            if ( ! Double.isNaN( c ) && ( ! isLog || c > 0 ) ) {
                if ( c > tops[ 0 ] ) {
                    tops[ 0 ] = c;
                    Arrays.sort( tops );
                }
                if ( c < bots[ nExclude - 1 ] ) {
                    bots[ nExclude - 1 ] = c;
                    Arrays.sort( bots );
                }
            }
        }
        return new double[] { bots[ nExclude - 1 ], tops[ 0 ], };
    }
}
