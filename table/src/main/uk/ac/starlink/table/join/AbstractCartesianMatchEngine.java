package uk.ac.starlink.table.join;

import java.util.HashSet;
import java.util.Set;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * A matching engine which can match points in an 
 * <tt>ndim</tt>-dimensional space.
 * All tuples (coordinate vectors) submitted to it must be 
 * <ndim>-element arrays of {@link java.lang.Number} objects.
 * Tuples are considered matching if they fall within an ellipsoid
 * defined by a scalar or vector error parameter submitted at construction
 * time.
 *
 * <p>This abstract class defines the mechanics of the matching,
 * but not the match parameters, which will presumably be to do 
 * with error radii.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class AbstractCartesianMatchEngine implements MatchEngine {

    private final int ndim_;
    private final int blockSize_;
    private boolean normaliseScores_;
    private double[] errors_;
    private double[] err2rs_;
    private double[] cellScales_;

    /**
     * Scaling factor determining the size of a grid cell as a multiple of
     * the size of the matching error in each dimension.  It can be
     * used as a tuning parameter.  It must be &gt;=1.
     */
    public final static double CELL_SCALE = 2.0;

    /**
     * Constructs a matcher which matches points in an
     * <tt>ndim</tt>-dimensional Cartesian space.
     * The error array (error ellipsoid dimensions) is not initialised to
     * anything sensible by this constructor.
     *
     * @param   ndim  dimensionality of the space
     * @param   normaliseScores  <tt>true</tt> iff you want match scores 
     *                           to be normalised
     */
    protected AbstractCartesianMatchEngine( int ndim, 
                                            boolean normaliseScores ) {
        ndim_ = ndim;
        errors_ = new double[ ndim ];
        err2rs_ = new double[ ndim ];
        cellScales_ = new double[ ndim ];
        blockSize_ = (int) Math.pow( 3, ndim );
        setNormaliseScores( normaliseScores );
    }

    /**
     * Returns the number of dimensions of this matcher.
     *
     * @param  dimensionality of Cartesian space
     */
    public int getDimensions() {
        return ndim_;
    }

    /**
     * Matches two tuples if they represent the coordinates of nearby points.
     * If they match (fall within the same error ellipsoid) the return
     * value is a non-negative value giving the distance between them.
     * According to the value of the <tt>normaliseScores</tt> flag,
     * this is either the actual distance between the points (Pythagoras)
     * or the same thing normalised to the range between 0 (same position) 
     * and 1 (on the boundary of the error ellipsoid).
     * If they don't match, -1 is returned.
     *
     * @param  tuple1  <tt>ndim</tt>-element array of <tt>Number</tt> objects
     *                 representing coordinates of first object
     * @param  tuple2  <tt>ndim</tt>-element array of <tt>Number</tt> objects
     *                 representing coordinates of second object
     * @return  the separation of the points represented by <tt>tuple1</tt>
     *          and <tt>tuple2</tt> if they match, or -1 if they don't
     */
    public double matchScore( Object[] tuple1, Object[] tuple2 ) {

        /* If any of the coordinates is too far away, reject it straight away.
         * This is a cheap test which will normally reject most requests. */
        for ( int i = 0; i < ndim_; i++ ) {
            if ( Math.abs( ((Number) tuple1[ i ]).doubleValue() - 
                           ((Number) tuple2[ i ]).doubleValue() ) 
                 > errors_[ i ] ) {
                return -1.0;
            }
        }

        /* We are in the right ball park - do an accurate calculation. */
        double spaceDist2 = 0.0; 
        double normDist2 = 0.0;
        for ( int i = 0; i < ndim_; i++ ) {
            double d = ((Number) tuple1[ i ]).doubleValue() - 
                       ((Number) tuple2[ i ]).doubleValue();
            double d2 = d * d;
            spaceDist2 += d2;
            normDist2 += d2 * err2rs_[ i ];
        }
        if ( normDist2 <= 1.0 ) {
            return normaliseScores_ ? Math.sqrt( normDist2 ) 
                                    : Math.sqrt( spaceDist2 );
        }
        else {
            return -1.0;
        }
    }

    /**
     * Returns a set of Cell objects representing the cell in which 
     * this tuple falls and somr or all of its neighbouring ones.
     *
     * @param  tuple  <tt>ndim</tt>-element array of <tt>Number</tt> objects
     *                representing coordinates of an object
     */
    public Object[] getBins( Object[] tuple ) {
        double[] coords = new double[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            if ( tuple[ i ] instanceof Number ) {
                coords[ i ] = ((Number) tuple[ i ]).doubleValue();
            }
            else {
                return NO_BINS;
            }
        }
        return getCellBlock( coords );
    }

    /**
     * Returns an array of tuple infos, one for each Cartesian dimension.
     */
    public ValueInfo[] getTupleInfos() {
        ValueInfo[] infos = new ValueInfo[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            DefaultValueInfo info = 
                new DefaultValueInfo( getCoordinateName( i ),
                                      Number.class,
                                      getCoordinateDescription( i ) );
            info.setNullable( false );
            infos[ i ] = info;
        }
        return infos;
    }

    public abstract DescribedValue[] getMatchParameters();

    /**
     * Returns the matching error along a given axis.
     * This is the principle radius of an ellipsoid within which two points
     * must fall in order to match.
     *
     * @return  error array
     */
    protected double getError( int idim ) {
        return errors_[ idim ];
    }

    /**
     * Sets one of the principle radii of the ellipsoid within which 
     * two points have to fall in order to match.
     *
     * @param  idim  index of axis
     * @param  error  error along axis <tt>idim</tt>
     */
    public void setError( int idim, double error ) {
        assert CELL_SCALE >= 1.0;
        errors_[ idim ] = error;
        err2rs_[ idim ] = 1.0 / ( error * error );
        cellScales_[ idim ] = 1.0 / ( CELL_SCALE * error );
    }

    /**
     * Determines whether the results of the {@link #matchScore} method
     * will be normalised or not.  
     * If <tt>norm</tt> is true, 
     * successful matches always result in a score between 0 and 1; 
     * if it's false, 
     * the score is the distance in the space defined by the supplied tuples.
     *
     * <p>If your errors are significantly anisotropic 
     * and/or your coordinates do not represent a physical space, 
     * you probably want to set this false.
     *
     * @param  norm  <tt>true</tt> iff you want match scores to be normalised
     */
    public void setNormaliseScores( boolean norm ) {
        normaliseScores_ = norm;
    }

    /**
     * Indicates whether the results of the {@link #matchScore} method
     * will be normalised.
     *
     * @return   <tt>true</tt> iff match scores will be normalised
     */
    public boolean getNormaliseScores() {
        return normaliseScores_;
    }

    /**
     * Returns the name of one of the coordinates.
     *
     * @param  idim  index of coordinate
     * @return  name to use for coordinate <tt>idim</tt>
     */
    String getCoordinateName( int idim ) {
        return ndim_ <= 3 ? new String[] { "X", "Y", "Z" }[ idim ]
                          : ( "Co-ord #" + ( idim + 1 ) );
    }

    /**
     * Returns the description of one of the coordinates.
     *
     * @param  idim  index of coordinate
     * @return  description to use for coordinate <tt>idim</tt>
     */
    String getCoordinateDescription( int idim ) {
        return "Cartesian co-ordinate #" + ( idim + 1 );
    }

    public abstract String toString();

    /**
     * Returns the cell label corresponding to the given coordinate set.
     *
     * @param  coords  ndim-dimensional array of coordinate values
     * @return  ndim-dimensional array of cell label indices
     */
    private int[] getBaseLabel( double[] coords ) {
        int[] label = new int[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            label[ i ] = (int) Math.floor( coords[ i ] * cellScales_[ i ] );
        }
        return label;
    }

    /**
     * Returns an array of Cell objects corresponding to the cell in which
     * <tt>coords</tt> falls and all its nearest neighbours.
     *
     * @param  coords  coordinates of reference points
     * @return  <tt>3^ndim</tt>-element array of Cells surrounding 
     *          <tt>coords</tt>
     */
    private Cell[] getCellBlock( double[] coords ) {

        /* Iterate over the 3^ndim points which are the given point and
         * all the points separated from it by err[i] in any direction i,
         * and accumulate a set of the cells in which each such point lies.
         * Any point which is near the given one must lie in one of those 
         * cells. */
        Set cells = new HashSet();
        int[] offset = new int[ ndim_ ];
        double[] pos = new double[ ndim_ ];
        for ( int icell = 0; icell < blockSize_; icell++ ) {

            /* Get the position of the next point. */
            for ( int i = 0; i < ndim_; i++ ) {
                pos[ i ] = coords[ i ] + ( offset[ i ] - 1 ) * errors_[ i ];
            }

            /* Ensure that the grid cell in which that point lies is 
             * in the accumulated set. */
            Cell cell = new Cell( getBaseLabel( pos ) );
            cells.add( cell );

            /* Bump the n-dimensional offset to the next point. */
            for ( int j = 0; j < ndim_; j++ ) {
                if ( ++offset[ j ] < 3 ) {
                    break;
                }
                else {
                    offset[ j ] = 0;
                }
            }
        }

        /* Sanity check. */
        for ( int i = 0; i < ndim_; i++ ) {
            assert offset[ i ] == 0;
        }

        /* Returns the set of cells as an array. */
        return (Cell[]) cells.toArray( new Cell[ cells.size() ] );
    }


    /**
     * Represents cells in the grid which represents the cartesian space.
     * Each cell has a label represented by <tt>ndim</tt> integral indices.
     */
    private class Cell {

        private final int[] label_;
        private final AbstractCartesianMatchEngine encloser_;

        Cell( int[] label ) {
            label_ = label;
            encloser_ = AbstractCartesianMatchEngine.this;
        }

        public boolean equals( Object o ) {
            if ( o instanceof Cell ) {
                Cell other = (Cell) o;
                if ( this.encloser_ == other.encloser_ ) {
                    int[] otherLabel = other.label_;
                    for ( int i = 0; i < ndim_; i++ ) {
                        if ( otherLabel[ i ] != label_[ i ] ) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        public int hashCode() {
            int code = 37;
            for ( int i = 0; i < ndim_; i++ ) {
                code = 23 * code + label_[ i ];
            }
            return code;
        }

        public String toString() {
            StringBuffer sbuf = new StringBuffer( "(" );
            for ( int i = 0; i < ndim_; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( "," ); 
                }
                sbuf.append( label_[ i ] );
            }
            sbuf.append( ")" );
            return sbuf.toString();
        }
    }
}
