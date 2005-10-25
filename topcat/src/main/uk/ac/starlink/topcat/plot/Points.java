package uk.ac.starlink.topcat.plot;

import java.io.IOException;
import java.util.Date;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Encapsulates a list of N-dimensional points in data space.
 * This is an abstract class with factory methods to underline the fact
 * that the implementation may change, though currently only one is 
 * available.  
 *
 * <p>At present, the sole implementation reads all the data from a
 * table at construction time and stores it in a <code>double[][]</code> array.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 June 2004
 */
public abstract class Points {

    private final int ndim_;
    private static final double MILLISECONDS_PER_YEAR
                              = 365.25 * 24 * 60 * 60 * 1000;

    /**
     * Constructor.
     *
     * @param  ndim  dimensionality
     */
    private Points( int ndim ) {
        ndim_ = ndim;
    }

    /**
     * Returns the dimensionality of this point set.
     *
     * @return  number of coordinates in each point
     */
    public int getNdim() {
        return ndim_;
    }

    /**
     * Returns the number of points in this dataset.
     *
     * @return  numer of points
     */
    public abstract int getCount();

    /**
     * Returns the value of one of the coordinates.
     *
     * @param   ipoint  point index
     * @param   icoord  coordinate index
     */
    public abstract double getCoord( int ipoint, int icoord );

    /**
     * Factory method to create a Points object from columns of a table.
     *
     * @param   table  table containing data
     * @param   icols  list of column indices, one for each coordinate
     *                 in the resulting Points object
     * @return  new Points object with the same number of points as the
     *          rows in <code>table</code> and the same number of
     *          dimensions as the elements in <code>icols</code>
     */
    public static Points createPoints( StarTable table, int[] icols ) 
            throws IOException {

        /* Prepare data structures. */
        final int ndim = icols.length;
        final int nrow = Tables.checkedLongToInt( table.getRowCount() );
        final double[][] data = new double[ nrow ][ ndim ];

        /* Populate the data array. */
        RowSequence rseq = table.getRowSequence();
        for ( int irow = 0; rseq.next(); irow++ ) {
            for ( int idim = 0; idim < ndim; idim++ ) {
                data[ irow ][ idim ] =
                    doubleValue( rseq.getCell( icols[ idim ] ) );
            }
        }
        rseq.close();

        /* Return a new Points object. */
        return new Points( ndim ) {
            public int getCount() {
                return nrow;
            }
            public double getCoord( int ipoint, int icoord ) {
                return data[ ipoint ][ icoord ];
            }
        };
    }

    /**
     * Returns a numeric (double) value for the given object where it
     * can be done.
     *
     * @param  value  an object
     * @return  floating point representation of <tt>value</tt>, or
     *          NaN if it can't be done
     */
    private static double doubleValue( Object value ) {
        if ( value instanceof Number ) {
            return ((Number) value).doubleValue();
        }   
        else if ( value instanceof Date ) {
            long milliseconds = ((Date) value).getTime(); 
            return 1970.0 + milliseconds / MILLISECONDS_PER_YEAR;
        }
        else {
            return Double.NaN;
        }
    }
}
