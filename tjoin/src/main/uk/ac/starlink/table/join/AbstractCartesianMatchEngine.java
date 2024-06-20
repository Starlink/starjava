package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Abstract superclass for match engines working in a Cartesian space.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2011
 */
public abstract class AbstractCartesianMatchEngine implements MatchEngine {

    private final int ndim_;
    private final double[] scales_;
    private final double[] rBinSizes_;
    private final DescribedValue binFactorParam_;
    private double binFactor_;

    /**
     * Factor which determines bin size to use,
     * as a multiple of the maximum error distance, if no
     * bin factor is set explicitly.
     * This is a tuning parameter (any value will give correct results,
     * but performance may be affected).
     * The current value may not be optimal.
     */
    private static final double DEFAULT_BIN_FACTOR = 8;

    private static final DefaultValueInfo BINFACT_INFO =
        new DefaultValueInfo( "Bin Factor", Double.class,
                              "Scaling factor to adjust bin size; "
                            + "larger values mean larger bins" );

    /**
     * Constructor.
     *
     * @param   ndim  dimensionality of Cartesian space
     */
    public AbstractCartesianMatchEngine( int ndim ) {
        ndim_ = ndim;
        scales_ = new double[ ndim_ ];
        rBinSizes_ = new double[ ndim_ ];
        binFactor_ = DEFAULT_BIN_FACTOR;
        binFactorParam_ = new BinFactorParameter();
    }

    /**
     * Returns the dimensionality of the Cartesian space
     * in which this match engine works.
     *      
     * @return   number of spatial dimensions
     */
    public int getNdim() {
        return ndim_;
    }

    /**
     * Sets a multiplier for the length scale that determines bin size.
     *
     * @param  binFactor  bin size multiplier
     */
    public void setBinFactor( double binFactor ) {
        if ( ! ( binFactor > 0 ) ) {
            throw new IllegalArgumentException( "Bin factor must be >0" );
        }
        binFactor_ = binFactor;
        for ( int id = 0; id < ndim_; id++ ) {
            configureScale( id );
        }
    }

    /**
     * Returns the multiplier for length scale that determines bin size.
     *
     * @return  bin size multiplier
     */
    public double getBinFactor() {
        return binFactor_;
    }

    /**
     * Sets the scale isotropically.  All dimension scales are set to the
     * given value.
     *
     * @param  scale  guide error distance
     */
    public void setIsotropicScale( double scale ) {
        for ( int id = 0; id < ndim_; id++ ) {
            setScale( id, scale );
        }
    }

    /**
     * Returns the isotropic scale.  If all dimension scales are set to the
     * same value, that value is returned.  If they are not all set to the
     * same value, the return value is undefined.
     *
     * @return  scale  isotropic guide error distance
     */
    public double getIsotropicScale() {
        return scales_[ 0 ];
    }

    /**
     * Sets the scale value for a given dimension.  In conjunction with the
     * bin factor, this determines the bin size.
     *
     * @param  idim  dimension index
     * @param  scale  guide error distance in dimension <code>idim</code>
     */
    protected void setScale( int idim, double scale ) {
        if ( scale < 0 ) {
            throw new IllegalArgumentException( "Scale must be >0" );
        }
        scales_[ idim ] = scale;
        configureScale( idim );
    }

    /**
     * Returns the scale value for a given dimension.
     *
     * @param  idim  dimension index
     * @return  guide error distance in dimension <code>idim</code>
     */
    protected double getScale( int idim ) {
        return scales_[ idim ];
    }
 
    /**
     * Reconfigures internal state following a change to the tuning
     * parameters affecting a given dimension.
     *
     * @param  idim  dimension index
     */
    private void configureScale( int idim ) {
        rBinSizes_[ idim ] = 1.0 / ( scales_[ idim ] * binFactor_ );
    }

    public DescribedValue[] getTuningParameters() {
        return new DescribedValue[] { binFactorParam_ };
    }

    /**
     * Returns an immutable factory for CartesianBinner objects
     * associated with the current state of this object.
     *
     * @return  CartesianBinner factory
     */
    Supplier<CartesianBinner> createBinnerFactory() {
        final int ndim = ndim_;
        final double[] scales = scales_.clone();
        final double[] rBinSizes = rBinSizes_.clone();
        return () -> new CartesianBinner( ndim, scales, rBinSizes );
    }

    /**
     * Utility method to calculate a match score using an isotropic error
     * radius between two given Carteian positions.
     *
     * @param  ndim  coordinate dimensionality
     * @param  coords1  position 1, ndim-element array
     * @param  coords2  position 2, ndim-element array
     * @param  err  maximum separation for match
     * @return   Pythagoras distance between positions 1 and 2 if they are
     *           within err of each other, otherwise -1
     * @see  MatchEngine#matchScore
     */
    static double matchScore( int ndim, double[] coords1, double[] coords2,
                              double err ) {
        double err2 = err * err;
        double dist2 = 0;
        for ( int id = 0; id < ndim; id++ ) {
            double d = coords2[ id ] - coords1[ id ];
            dist2 += d * d;
            if ( ! ( dist2 <= err2 ) ) {
                return -1;
            }
        }
        double score = Math.sqrt( dist2 );
        assert score >= 0 && score <= err;
        return score;
    }

    /**
     * Returns a description of the tuple element containing one of
     * the Cartesian coordinates.
     *
     * @param  idim  index of the coordinate in question
     * @return  metadata for coordinate <code>idim</code>
     */
    ValueInfo createCoordinateInfo( int idim ) {
        DefaultValueInfo info =
            new DefaultValueInfo( getCoordinateName( idim ), Number.class,
                                  getCoordinateDescription( idim ) );
        info.setNullable( false );
        return info;
    }

    /**
     * Returns a name for one of the coordinates.
     *
     * @param  idim  index of coordinate
     * @return  name to use for coordinate <code>idim</code>
     */
    String getCoordinateName( int idim ) {
        return ndim_ <= 3 ? new String[] { "X", "Y", "Z" }[ idim ]
                          : ( "Co-ord #" + ( idim + 1 ) );
    }

    /**
     * Returns the description of one of the coordinates.
     *
     * @param  idim  index of coordinate
     * @return  description to use for coordinate <code>idim</code>
     */
    String getCoordinateDescription( int idim ) {
        return "Cartesian co-ordinate #" + ( idim + 1 );
    }

    public abstract String toString();

    /**
     * Returns the numeric value for an object if it is a Number,
     * and NaN otherwise.
     *
     * @param  numobj  object
     * @return  numeric value
     */
    static double getNumberValue( Object numobj ) {
        return numobj instanceof Number
             ? ((Number) numobj).doubleValue()
             : Double.NaN;
    }

    /**
     * Returns a labeller for a given input array length.
     *
     * @param   ndim   length of arrays that will be fed to the labeller
     * @return   labeller
     */
    private static Labeller createLabeller( int ndim ) {

        /* For common cases (ndim 1 or 2), provide an implementation
         * with as small a memory footprint as possible.
         * But for n>2, which is not very common,
         * provide a generic fallback. */
        switch ( ndim ) {
            case 1:
                return array -> {
                    long val = array[ 0 ];
                    int ival = (int) val;
                    return ival == val ? Integer.valueOf( ival )
                                       : Long.valueOf( val );
                };
            case 2:
                return array -> {
                    long x = array[ 0 ];
                    long y = array[ 1 ];
                    int ix = (int) x;
                    int iy = (int) y;
                    return ix == x && iy == y
                         ? Long.valueOf( ( x << 32 ) | ( y & 0xffffffffL ) )
                         : new Cell( array.clone() );
                };
            default:
                return array -> new Cell( array.clone() );
        }
    }

    /**
     * Utility class providing functions required when manipulating
     * Cartesian grid bins.
     */
    static class CartesianBinner {

        private final int ndim_;
        private final double[] scales_;
        private final double[] rBinSizes_;
        private final Labeller labeller_;
        private final long[] llo_;
        private final long[] lhi_;

        private static final Object[] NO_BINS = new Object[ 0 ];

        /**
         * Constructor.
         *
         * @param  ndim  dimensionality
         * @param  scales  ndim-element array of per-dimension scale values
         * @param  rBinSizes  ndim-element array of per-dimension
         *                    reciprocal bin extents
         */
        CartesianBinner( int ndim, double[] scales, double[] rBinSizes ) {
            ndim_ = ndim;
            scales_ = scales;
            rBinSizes_ = rBinSizes;
            labeller_ = createLabeller( ndim );
            llo_ = new long[ ndim ];
            lhi_ = new long[ ndim ];
        }

        /**
         * Returns the dimensionality of this binner.
         *
         * @return  dimension count
         */
        public int getNdim() {
            return ndim_;
        }

        /**
         * Returns an array of the bin objects that may be covered within a
         * given distance of a given position.  Not all returned bins are
         * guaranteed to be so covered.  Validation is performed on the
         * arguments (NaNs will result in an empty return).
         *
         * @param  coords  central position
         * @param  radius  error radius
         * @return  bin objects that may be within <code>radius</code>
         *          of <code>coords</code>
         */
        public Object[] getRadiusBins( double[] coords, double radius ) {
            return radius >= 0 ? doGetBins( coords, radius )
                               : NO_BINS;
        }

        /**
         * Returns an array of the bin objects that may be covered within the
         * current anisotropic scale length in each direction of a given
         * position.
         * Not all returned bins are guaranteed to be so covered.
         * Validation is performed on the arguments (NaNs will result in
         * an empty return).
         *
         * @param  coords  central position
         * @return  bin objects within a scale length of <code>coords</code>
         */
        public Object[] getScaleBins( double[] coords ) {
            return doGetBins( coords, Double.NaN );
        }

        /**
         * Calculates the Cartesian coordinates for a given match tuple.
         *
         * @param  tuple  input tuple
         * @param  ndim-element coordinate array,
         *         populated on output with numeric coordinate values
         */
        public void toCoords( Object[] tuple, double[] coords ) {
            for ( int id = 0; id < ndim_; id++ ) {
                coords[ id ] = getNumberValue( tuple[ id ] );
            }
        }

        /**
         * Does the work for the get*Bins methods.
         * Returns bins within some range of the given position.
         * If radius is a number, it is used;
         * if it's NaN, the scale length is used instead.
         *
         * @param   coords  central position
         * @param  radius  error radius or NaN
         * @return  list of bin objects
         */
        private Object[] doGetBins( double[] coords, double radius ) {
            boolean useScale = Double.isNaN( radius );

            /* Work out the range of cell label coordinates in each dimension
             * corresponding to a cube extending + and -err away from the
             * submitted position. llo_ and lhi_ are workspace arrays
             * holding lowest and highest coord label indices. */
            int ncell = 1;                    // total number of cells in cube
            for ( int id = 0; id < ndim_; id++ ) {
                double c0 = coords[ id ];
                if ( Double.isNaN( c0 ) ) {
                    return NO_BINS;
                }
                else {
                    double r = useScale ? scales_[ id ] : radius;
                    llo_[ id ] = getLabelComponent( id, c0 - r );
                    lhi_[ id ] = getLabelComponent( id, c0 + r );
                    long extent = lhi_[ id ] - llo_[ id ] + 1;
                    assert (int) extent == extent;
                    ncell *= (int) extent;
                }
            }

            /* Iterate over the cube of cells in ndim dimensions to construct
             * a list of all the cells inside it. */
            Object[] cells = new Object[ ncell ];
            long[] indices = llo_.clone();
            for ( int ic = 0; ic < ncell; ic++ ) {
                cells[ ic ] = labeller_.createLabel( indices );
                for ( int jd = 0; jd < ndim_; jd++ ) {
                    if ( ++indices[ jd ] <= lhi_[ jd ] ) {
                        break;
                    }
                    else {
                        indices[ jd ] = llo_[ jd ];
                    }
                }
            }

            /* Sanity check. */
            assert Arrays.equals( indices, llo_ );
            assert new HashSet<Object>( Arrays.asList( cells ) ).size()
                   == cells.length;
    
            /* Return the list of cells. */
            return cells;
        }

        /** 
         * Returns the integer label of a cell position in a given dimension.
         * This identifies one of the coordinates of the discrete cube 
         * corresponding to any continuous position.
         *      
         * @param   idim  dimension index 
         * @param   coord  position in space in dimension <code>idim</code>
         * @return   index of cell coordinate in dimension <code>idim</code>
         */     
        private long getLabelComponent( int idim, double coord ) { 
            return (long) Math.floor( coord * rBinSizes_[ idim ] ); 
        }
    }

    /**
     * Tuning parameter which controls the bin factor.
     */
    class BinFactorParameter extends DescribedValue {
        BinFactorParameter() {
            super( BINFACT_INFO );
        }
        public Object getValue() {
            return Double.valueOf( getBinFactor() );
        }
        public void setValue( Object value ) {
            setBinFactor( ((Number) value).doubleValue() );
        }
    }

    /**
     * Parameter which controls the isotropic scale value.
     */
    class IsotropicScaleParameter extends DescribedValue {
        public IsotropicScaleParameter( ValueInfo info ) {
            super( info );
        }
        public Object getValue() {
            return Double.valueOf( getIsotropicScale() );
        }
        public void setValue( Object value ) {
            setIsotropicScale( ((Number) value).doubleValue() );
        }
    }

    /**
     * Maps an array of indices to a corresponding object suitable
     * as a map key.
     */
    @FunctionalInterface
    private interface Labeller {

        /**
         * Maps a fixed-length array of longs to an object suitable for
         * use as a map key.
         * The return value must implement the
         * {@link java.lang.Object#equals} and
         * {@link java.lang.Object#hashCode}
         * methods such that results for equivalent input arrays are equal,
         * and as far as possible results for different input arrays are
         * not equal.
         *
         * <p>The output value must represent the value of the input array
         * at the time of the call, and must not be affected by any subsequent
         * changes to the content of the input <code>array</code> object.
         *
         * @param   array   array giving input values
         * @return   object representing array, without reference to the
         *           input array object itself
         */
        Object createLabel( long[] array );
    }
}
