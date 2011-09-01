package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.HashSet;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;

/**
 * Abstract superclass for match engines which work in an N-dimensional
 * Cartesian space with per-object errors.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2011
 */
public abstract class AbstractErrorCartesianMatchEngine implements MatchEngine {

    private final int ndim_;
    private final DescribedValue scaleParam_;
    private final DescribedValue binFactorParam_;
    private double scale_;
    private double binFactor_;
    private double rBinSize_;

    private static final double DEFAULT_SCALE = 1.0;
    private static final DefaultValueInfo SCALE_INFO =
        new DefaultValueInfo( "Scale", Number.class,
                              "Rough average of per-object error distance; "
                            + "just used for tuning in conjunction with "
                            + "bin factor" );
    private static final DefaultValueInfo BINFACT_INFO =
        new DefaultValueInfo( "Bin Factor", Double.class,
                              "Scaling factor to adjust bin size; "
                            + "larger values mean larger bins" );

    /**
     * Constructor.
     *
     * @param   ndim  dimensionality
     * @param   scale   rough scale of errors
     */
    public AbstractErrorCartesianMatchEngine( int ndim, double scale ) {
        ndim_ = ndim;
        setScale( scale );
        binFactor_ = AbstractCartesianMatchEngine.DEFAULT_BIN_FACTOR;
        scaleParam_ = new ScaleParam();
        binFactorParam_ = new BinFactorParam();
    }

    /**
     * Returns the dimensionality in which this match engine works.
     *
     * @return   number of spatial dimensions
     */
    public int getNdim() {
        return ndim_;
    }

    /**
     * Sets the guideline length scale for errors.
     * This is effectively a tuning parameter.
     *
     * @param  scale  guide error distance
     */
    public void setScale( double scale ) {
        if ( Double.isNaN( scale ) ) {
            scale = DEFAULT_SCALE;
        }
        if ( scale < 0 ) {
            throw new IllegalArgumentException( "Scale must be >0" );
        }
        scale_ = scale;
        configureScale();
    }

    /**
     * Returns the guideline length scale for errors.
     *
     * @return   guide error distance
     */
    public double getScale() {
        return scale_;
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
        configureScale();
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
     * Used to reconfigure internal state following a change to the tuning
     * parameters.
     */
    private void configureScale() {
        rBinSize_ = 1.0 / ( scale_ * binFactor_ );
    }

    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[] { scaleParam_ };
    }

    public DescribedValue[] getTuningParameters() {
        return new DescribedValue[] { binFactorParam_ };
    }

    /**
     * Returns an array of the bin objects that may be covered within a
     * given distance of a given position.  Not all returned bins are
     * guaranteed to be so covered.  Validation is performed on the
     * arguments (NaNs will result in an empty return).
     *
     * @param  coords  central position
     * @param  err     error radius
     * @return  bin objects that may be within <code>err</code>
     *          of <code>coords</code>
     */
    protected Object[] getBins( double[] coords, double err ) {
        if ( ! ( err >= 0 ) ) {
            return NO_BINS;
        }

        /* Work out the range of cell label coordinates in each dimension
         * corresponding to a cube extending + and -err away from the
         * submitted position. */
        int[] lbase = new int[ ndim_ ];   // lowest coord label index
        int[] lcount = new int[ ndim_ ];  // number of coord labels
        int ncell = 1;                    // total number of cells in cube
        for ( int id = 0; id < ndim_; id++ ) {
            double c0 = coords[ id ];
            if ( Double.isNaN( c0 ) ) {
                return NO_BINS;
            }
            else {
                lbase[ id ] = getLabelComponent( id, c0 - err );
                lcount[ id ] = getLabelComponent( id, c0 + err )
                             - lbase[ id ] + 1;
                ncell *= lcount[ id ];
            }
        }

        /* Iterate over the cube of cells in ndim dimensions to construct
         * a list of all the cells inside it. */
        Cell[] cells = new Cell[ ncell ];
        int[] label = (int[]) lbase.clone();
        for ( int ic = 0; ic < ncell; ic++ ) {
            cells[ ic ] = new Cell( (int[]) label.clone() );
            for ( int jd = 0; jd < ndim_; jd++ ) {
                if ( ++label[ jd ] < lcount[ jd ] ) {
                    break;
                }
                else {
                    label[ jd ] -= lcount[ jd ];
                    assert label[ jd ] == lbase[ jd ];
                }
            }
        }

        /* Sanity check. */
        assert Arrays.equals( label, lbase );
        assert new HashSet( Arrays.asList( cells ) ).size() == cells.length;

        /* Return the list of cells. */
        return cells;
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
     * Returns the integer label of a cell position in a given dimension.
     * This identifies one of the coordinates of the discrete cube
     * corresponding to any continuous position.
     *
     * @param   idim  dimension index
     * @param   coord  position in space in dimension <code>idim</code>
     * @return   index of cell coordinate in dimension <code>idim</code>
     */
    private int getLabelComponent( int idim, double coord ) {
        return (int) Math.floor( coord * rBinSize_ );
    }

    /**
     * Parameter which controls the scale value.
     */
    private class ScaleParam extends DescribedValue {
        ScaleParam() {
            super( SCALE_INFO );
        }
        public Object getValue() {
            return new Double( getScale() );
        }
        public void setValue( Object value ) {
            setScale( value == null ? DEFAULT_SCALE
                                    : ((Number) value).doubleValue() );
        }
    }

    /**
     * Tuning parameter which controls the bin factor.
     */
    private class BinFactorParam extends DescribedValue {
        BinFactorParam() {
            super( BINFACT_INFO );
        }
        public Object getValue() {
            return new Double( getBinFactor() );
        }
        public void setValue( Object value ) {
            setBinFactor( ((Number) value).doubleValue() );
        }
    }
}
