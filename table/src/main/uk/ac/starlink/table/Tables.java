package uk.ac.starlink.table;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.jdbc.JDBCStarTable;

/**
 * Utility class for miscellaneous table-related functionality.
 */
public class Tables {

    /**
     * Returns a table based on a given table and guaranteed to have 
     * random access.  If the original table <tt>stab</tt> has random
     * access then it is returned, otherwise a new random access table
     * is built using its data.
     *
     * @param  stab  original table
     * @return  a table with the same data as <tt>startab</tt> and with 
     *          <tt>isRandom()==true</tt>
     */
    public static StarTable randomTable( StarTable startab )
            throws IOException {

        /* If it has random access already, we don't need to do any work. */
        if ( startab.isRandom() ) {
            return startab;
        }

        /* If it's JDBC we can turn it random. */
        else if ( startab instanceof JDBCStarTable ) {
            try {
                ((JDBCStarTable) startab).setRandom();
                return startab;
            }
            catch ( SQLException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }

        /* Otherwise, we need to construct a table based on the sequential
         * table that acts random. */
        return new RowScratchTable( startab );
    }

    /**
     * Convenience method to return an array of all the column headers
     * in a given table.  Modifying this array will not affect the table.
     *
     * @param  startab  the table being enquired about
     * @return an array of all the column headers
     */
    public static ColumnInfo[] getColumnInfos( StarTable startab ) {
        int ncol = startab.getColumnCount();
        ColumnInfo[] infos = new ColumnInfo[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            infos[ i ] = startab.getColumnInfo( i );
        }
        return infos;
    }

    /**
     * Returns a table equivalent to the original but with a given column
     * deleted.  The result may or may not be the same object as the
     * input table.
     *
     * @param  startab  the table from which to delete a column
     * @param  icol     the index of the column to be deleted
     * @throws  IndexOutOfBoundsException if <tt>startab</tt> has no column
     *          at <tt>icol</tt>
     */
    public static StarTable deleteColumn( StarTable startab, int icol ) {
        int ncol = startab.getColumnCount();
        if ( icol < 0 || icol >= ncol ) {
            throw new IndexOutOfBoundsException( 
                "Deleted column " + icol + " out of range 0.." + ( ncol - 1 ) );
        }
        int[] colmap = new int[ ncol - 1 ];
        int j = 0;
        for ( int i = 0; i < ncol; i++ ) {
            if ( i != icol ) {
                colmap[ j++ ] = i;
            }
        }
        assert j == ncol - 1;
        return new ColumnPermutedStarTable( startab, colmap );
    }
}
