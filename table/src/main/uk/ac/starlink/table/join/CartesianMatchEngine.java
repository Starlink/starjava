package uk.ac.starlink.table.join;

import java.util.HashSet;
import java.util.Set;

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
    private final double[] errs;
    private final double[] err2rs;
    private final double[] cellScales;
    private final int blockSize;

    /**
     * Scaling factor determining the size of a grid cell as a multiple of
     * the size of the matching error in each dimension.  It can be
     * used as a tuning parameter.  It must be &gt;=1.
     */
    public final static double CELL_SCALE = 5.0;

    /**
     * Constructs a matcher which matches points in an
     * <tt>ndim</tt>-dimensional Cartesian space with an anisotropic
     * error margin.
     *
     * @param   ndim  dimensionality of the space
     * @param   errs  <tt>ndim</tt> dimensional array of the principle radii
     *                of an ellipsoid that defines whether two points match
     */
    public CartesianMatchEngine( int ndim, double[] errs ) {
        this.ndim = ndim;
        this.errs = (double[]) errs.clone();
        err2rs = new double[ ndim ];
        cellScales = new double[ ndim ];
        assert CELL_SCALE >= 1.0;
        for ( int i = 0; i < ndim; i++ ) {
            err2rs[ i ] = 1.0 / ( errs[ i ] * errs[ i ] );
            cellScales[ i ] = 1.0 / ( CELL_SCALE * errs[ i ] );
        }
        blockSize = (int) Math.pow( 3, ndim );
    }

    /**
     * Constructs a matcher which matches points in an
     * <tt>ndim</tt>-dimensional Cartesian space with an isotropic 
     * error margin.
     *
     * @param   ndim dimensionality of the space
     * @param   err  maximum distance between two matching points
     */
    public CartesianMatchEngine( int ndim, double err ) {
        this( ndim, makeArray( ndim, err ) );
    }

    /**
     * Utility method which returns a new array containing copies of
     * the same value.
     *
     * @param  ndim  number of dimensions
     * @param  vall  value
     * @return  <tt>ndim</tt>-element array with all elements equal 
     *          to <tt>val</tt>
     */
    private static double[] makeArray( int ndim, double val ) {
        double[] arr = new double[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            arr[ i ] = val;
        }
        return arr;
    }

    /**
     * Matches two tuples if they represent the coordinates of nearby points.
     *
     * @param  tuple1  <tt>ndim</tt>-element array of <tt>Number</tt> objects
     *                 representing coordinates of first object
     * @param  tuple2  <tt>ndim</tt>-element array of <tt>Number</tt> objects
     *                 representing coordinates of second object
     * @return  <tt>true</tt> if <tt>tuple1</tt> and <tt>tuple2</tt> fall
     *          within the same error ellipsoid
     */
    public boolean matches( Object[] tuple1, Object[] tuple2 ) {

        /* If any of the coordinates is too far away, reject it straight away.
         * This is a cheap test which will normally reject most requests. */
        for ( int i = 0; i < ndim; i++ ) {
            if ( Math.abs( ((Number) tuple1[ i ]).doubleValue() - 
                           ((Number) tuple2[ i ]).doubleValue() ) 
                 > errs[ i ] ) {
                return false;
            }
        }

        /* We are in the right ball park - do an accurate calculation. */
        double dist = 0.0; 
        for ( int i = 0; i < ndim; i++ ) {
            double d = ((Number) tuple1[ i ]).doubleValue() - 
                       ((Number) tuple2[ i ]).doubleValue();
            dist += d * d * err2rs[ i ];
        }
        return dist <= 1.0;
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
}
