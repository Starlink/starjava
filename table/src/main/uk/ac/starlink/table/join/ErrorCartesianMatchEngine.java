package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Matcher which matches in an N-dimensional Cartesian space with per-object
 * errors.  Two points are considered matching if the distance between them
 * is less than the sum of the errors associated with each object.
 *
 * @author   Mark Taylor
 * @since    31 Aug 2011
 */
public class ErrorCartesianMatchEngine implements MatchEngine {

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
    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Spatial distance between matched points" );
    static {
        BINFACT_INFO.setNullable( false );
        SCORE_INFO.setUCD( "pos.distance" );
    }

    /**
     * Constructor.
     *
     * @param   ndim  dimensionality
     * @param   scale   rough scale of errors 
     */
    public ErrorCartesianMatchEngine( int ndim, double scale ) {
        ndim_ = ndim;
        setScale( scale );
        binFactor_ = AbstractCartesianMatchEngine.DEFAULT_SCALE_FACTOR;
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

    public ValueInfo[] getTupleInfos() {
        List<ValueInfo> infoList = new ArrayList<ValueInfo>();
        for ( int id = 0; id < ndim_; id++ ) {
            infoList.add( AbstractCartesianMatchEngine
                         .createCoordinateInfo( ndim_, id ) ); 
        }
        DefaultValueInfo errInfo =
            new DefaultValueInfo( "Error", Number.class,
                                  "Per-object error radius" );
        errInfo.setNullable( false );
        infoList.add( errInfo );
        return infoList.toArray( new ValueInfo[ 0 ] );
    }

    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[] { scaleParam_ };
    }

    public DescribedValue[] getTuningParameters() {
        return new DescribedValue[] { binFactorParam_ };
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public String toString() {
        return ndim_ + "-d Cartesian with Errors";
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {

        /* Determine the distance threshold for matching of this pair. */
        double err1 = ((Number) tuple1[ ndim_ ]).doubleValue();
        double err2 = ((Number) tuple2[ ndim_ ]).doubleValue();
        double errSum = err1 + err2;

        /* If the distance in any dimension is greater than the maximum,
         * reject it straight away.  This is a cheap test which will normally
         * reject most pairs. */
        for ( int id = 0; id < ndim_; id++ ) {
            if ( Math.abs( ((Number) tuple1[ id ]).doubleValue() -
                           ((Number) tuple2[ id ]).doubleValue() ) > errSum ) {
                return -1;
            }
        }

        /* Otherwise, calculate the distance using Pythagoras. */
        double dist2 = 0;
        for ( int id = 0; id < ndim_; id++ ) {
            double dist = ((Number) tuple1[ id ]).doubleValue() -
                          ((Number) tuple2[ id ]).doubleValue();
            dist2 += dist * dist;
        }
        return dist2 <= errSum * errSum ? Math.sqrt( dist2 )
                                        : -1;
    }

    public Object[] getBins( Object[] tuple ) {

        /* Get the error associated with the submitted position. */
        double err = ((Number) tuple[ ndim_ ]).doubleValue();
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
            if ( tuple[ id ] instanceof Number ) {
                double c0 = ((Number) tuple[ id ]).doubleValue();
                lbase[ id ] = getLabelComponent( id, c0 - err );
                lcount[ id ] = getLabelComponent( id, c0 + err )
                             - lbase[ id ] + 1;
                ncell *= lcount[ id ];
            }
            else {
                return NO_BINS;
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

    public boolean canBoundMatch() {
        return true;
    }

    public Comparable[][] getMatchBounds( Comparable[] minIn,
                                          Comparable[] maxIn ) {

        /* Prepare output arrays. */
        Comparable[] minOut = new Comparable[ minIn.length ];
        Comparable[] maxOut = new Comparable[ maxIn.length ];

        /* Max/Min error values are unchanged. */
        minOut[ ndim_ ] = minIn[ ndim_ ];
        maxOut[ ndim_ ] = maxIn[ ndim_ ];

        /* Extend the bounds region both ways in each dimension
         * by the maximum error value. */
        if ( maxIn[ ndim_ ] instanceof Number ) {
            double err = ((Number) maxIn[ ndim_ ]).doubleValue();
            if ( err >= 0 ) {
                for ( int id = 0; id < ndim_; id++ ) {
                    if ( minIn[ id ] instanceof Number ) {
                        minOut[ id ] = AbstractCartesianMatchEngine
                                      .add( (Number) minIn[ id ], -err );
                    }
                    if ( maxIn[ id ] instanceof Number ) {
                        maxOut[ id ] = AbstractCartesianMatchEngine
                                      .add( (Number) maxIn[ id ], +err );
                    }
                }
            }
        }

        /* Return the result. */
        return new Comparable[][] { minOut, maxOut };
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
