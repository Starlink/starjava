package uk.ac.starlink.table.join;

import java.util.Arrays;
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
 * @author   Mark Taylor (Starlink)
 */
public class CartesianMatchEngine implements MatchEngine {

    private final int ndim;
    private final int blockSize;
    private boolean normaliseScores;
    private double[] errs;
    private double[] err2rs;
    private double[] cellScales;
    private DescribedValue errorValue = new ErrorValue();

    /**
     * Scaling factor determining the size of a grid cell as a multiple of
     * the size of the matching error in each dimension.  It can be
     * used as a tuning parameter.  It must be &gt;=1.
     */
    public final static double CELL_SCALE = 5.0;

    /**
     * Constructs a matcher which matches points in an
     * <tt>ndim</tt>-dimensional Cartesian space.
     * An initial isotropic error margin is specified.
     *
     * @param   ndim  dimensionality of the space
     * @param   err  initial maximum distance between two matching points
     * @param   normaliseScores  <tt>true</tt> iff you want match scores 
     *                           to be normalised
     */
    public CartesianMatchEngine( int ndim, double err, 
                                 boolean normaliseScores ) {
        this.ndim = ndim;
        blockSize = (int) Math.pow( 3, ndim );
        setError( err );
        setNormaliseScores( normaliseScores );
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
        for ( int i = 0; i < ndim; i++ ) {
            if ( Math.abs( ((Number) tuple1[ i ]).doubleValue() - 
                           ((Number) tuple2[ i ]).doubleValue() ) 
                 > errs[ i ] ) {
                return -1.0;
            }
        }

        /* We are in the right ball park - do an accurate calculation. */
        double spaceDist2 = 0.0; 
        double normDist2 = 0.0;
        for ( int i = 0; i < ndim; i++ ) {
            double d = ((Number) tuple1[ i ]).doubleValue() - 
                       ((Number) tuple2[ i ]).doubleValue();
            double d2 = d * d;
            spaceDist2 += d2;
            normDist2 += d2 * err2rs[ i ];
        }
        if ( normDist2 <= 1.0 ) {
            return normaliseScores ? Math.sqrt( normDist2 ) 
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
        double[] coords = new double[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            coords[ i ] = ((Number) tuple[ i ]).doubleValue();
        }
        return getCellBlock( coords );
    }

    public ValueInfo[] getTupleInfos() {
        ValueInfo[] infos = new ValueInfo[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            DefaultValueInfo info = 
                new DefaultValueInfo( "Co-ordinate " + ( i + 1 ),
                                      Number.class,
                                      "Cartesian co-ordinate #" + ( i + 1 ) );
            info.setNullable( false );
            infos[ i ] = info;
        }
        if ( ndim == 2 || ndim == 3 ) {
            ((DefaultValueInfo) infos[ 0 ]).setName( "X" );
            ((DefaultValueInfo) infos[ 1 ]).setName( "Y" );
            if ( ndim == 3 ) {
                ((DefaultValueInfo) infos[ 2 ]).setName( "Z" );
            }
        }
        return infos;
    }

    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[] { errorValue };
    }

    /**
     * Returns an array containing the principle radii of an ellipsoid
     * that determines whether two points match. 
     * The returned array is a clone of the internal data structure; 
     * use <tt>setErrors</tt> to change the values.
     *
     * @return  error array
     */
    public double[] getErrors() {
        return (double[]) errs.clone();
    }

    /**
     * Sets the array containing the principle radii of an ellipsoid
     * that determines whether two points match.
     *
     * @param  errors  error array
     */
    public void setErrors( double[] errors ) {
        if ( errors.length != ndim ) {
            throw new IllegalArgumentException( 
                "Errors array wrong length " + errors.length +
                " (should match dimensionality " + ndim + ")" );
        }
        this.errs = (double[]) errors.clone();
        err2rs = new double[ ndim ];
        cellScales = new double[ ndim ];
        assert CELL_SCALE >= 1.0;
        for ( int i = 0; i < ndim; i++ ) {
            err2rs[ i ] = 1.0 / ( errs[ i ] * errs[ i ] );
            cellScales[ i ] = 1.0 / ( CELL_SCALE * errs[ i ] );
        }
    }

    /**
     * Sets the maximum distance between two points that counts as a match.
     * This is equivalent to calling {@link #setErrors} with an array 
     * that has all the same values.
     *
     * @param  error  maximum distance for matching
     */
    public void setError( double error ) {
        double[] errs = new double[ ndim ];
        Arrays.fill( errs, error );
        setErrors( errs );
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
     * (you've used {@link #setErrors} not {@link #setError}) 
     * and/or your coordinates do not represent a physical space, 
     * you probably want to set this false.
     *
     * @param  norm  <tt>true</tt> iff you want match scores to be normalised
     */
    public void setNormaliseScores( boolean norm ) {
        normaliseScores = norm;
    }

    /**
     * Indicates whether the results of the {@link #matchScore} method
     * will be normalised.
     *
     * @return   <tt>true</tt> iff match scores will be normalised
     */
    public boolean getNormaliseScores() {
        return normaliseScores;
    }

    public String toString() {
        return ndim + "d Cartesian";
    }

    /**
     * Returns the cell label corresponding to the given coordinate set.
     *
     * @param  coords  ndim-dimensional array of coordinate values
     * @return  ndim-dimensional array of cell label indices
     */
    private int[] getBaseLabel( double[] coords ) {
        int[] label = new int[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            label[ i ] = (int) Math.floor( coords[ i ] * cellScales[ i ] );
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
        int[] offset = new int[ ndim ];
        double[] pos = new double[ ndim ];
        for ( int icell = 0; icell < blockSize; icell++ ) {

            /* Get the position of the next point. */
            for ( int i = 0; i < ndim; i++ ) {
                pos[ i ] = coords[ i ] + ( offset[ i ] - 1 ) * errs[ i ];
            }

            /* Ensure that the grid cell in which that point lies is 
             * in the accumulated set. */
            Cell cell = new Cell( getBaseLabel( pos ) );
            cells.add( cell );

            /* Bump the n-dimensional offset to the next point. */
            for ( int j = 0; j < ndim; j++ ) {
                if ( ++offset[ j ] < 3 ) {
                    break;
                }
                else {
                    offset[ j ] = 0;
                }
            }
        }

        /* Sanity check. */
        for ( int i = 0; i < ndim; i++ ) {
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

        private final int[] label;
        private final CartesianMatchEngine encloser;

        Cell( int[] label ) {
            this.label = label;
            this.encloser = CartesianMatchEngine.this;
        }

        public boolean equals( Object o ) {
            if ( o instanceof Cell ) {
                Cell other = (Cell) o;
                if ( this.encloser == other.encloser ) {
                    int[] otherLabel = other.label;
                    for ( int i = 0; i < ndim; i++ ) {
                        if ( otherLabel[ i ] != label[ i ] ) {
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
            for ( int i = 0; i < ndim; i++ ) {
                code = 23 * code + label[ i ];
            }
            return code;
        }

        public String toString() {
            StringBuffer sbuf = new StringBuffer( "(" );
            for ( int i = 0; i < ndim; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( "," ); 
                }
                sbuf.append( label[ i ] );
            }
            sbuf.append( ")" );
            return sbuf.toString();
        }
    }

    /**
     * Implements the parameter which controls the matching error.
     */
    private class ErrorValue extends DescribedValue {
        ErrorValue() {
            super( new DefaultValueInfo( "Error", Double.class, 
                                         "Maximum separation for match" ) );
        }
        public Object getValue() {
            for ( int i = 0; i < ndim; i++ ) {
                if ( errs[ i ] != errs[ 0 ] ) {
                    throw new IllegalStateException( 
                        "Uh oh - can't cope with anistotropic error" );
                }
            }
            return new Double( errs[ 0 ] );
        }
        public void setValue( Object value ) {
            setError( ((Double) value).doubleValue() );
        }
    }
}
