package uk.ac.starlink.table;

import java.io.IOException;
import java.lang.reflect.Array;
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
        return new RowRandomWrapperStarTable( startab );
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

    /**
     * Copies the data and metadata from a <tt>StarTable</tt> into a 
     * table sink.  
     * This method is supplied for convenience; its implementation is
     * very straightforward.
     *
     * @param   source  table to be copied
     * @param   sink    table destination
     */
    public static void streamStarTable( StarTable source, TableSink sink ) 
            throws IOException {
        sink.acceptMetadata( source );
        for ( RowSequence rseq = source.getRowSequence(); rseq.hasNext(); ) {
            rseq.next();
            sink.acceptRow( rseq.getRow() );
        }
        sink.endRows();
    }

    /**
     * Diagnostic method which tests the invariants of a StarTable.
     * This method returns no value, and throws an exception if a table
     * is illegal in some way.
     * If this method throws an exception when called on a given StarTable,
     * that table is deemed to be bad in some way (buggy implementation
     * or I/O trouble during access).
     * This method is provided for convenience and diagnostics 
     * because there are a number of ways, some of them subtle, in which
     * a StarTable can fail to fulfil its general contract.
     * <p>
     * That a table passes this test does not guarantee that the table has
     * no bugs.  This method should not generally be used in production
     * code, since it may be expensive in time and/or memory.
     *
     * @param  table  table to test
     * @throws  AssertionError  if an invariant is violated
     * @throws  IOException   if there is an I/O error
     */
    public static void checkTable( StarTable table ) throws IOException {
        int formatChars = 100;
        int ncol = table.getColumnCount();
        long nrow = table.getRowCount();
        boolean isRandom = table.isRandom();

        /* Check a random-access table knows how many rows it has. */
        if ( isRandom ) {
            assertTrue( nrow >= 0 );
        }

        /* Check the shape arrays look OK. */
        int[] nels = new int[ ncol ];
        Class[] classes = new Class[ ncol ];
        ColumnInfo[] colinfos = new ColumnInfo[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            colinfos[ icol ] = cinfo;
            classes[ icol ] = cinfo.getContentClass();
            int[] dims = cinfo.getShape();
            if ( dims != null ) {
                int ndim = dims.length;
                assertTrue( ndim > 0 );
                assertTrue( cinfo.getContentClass().isArray() );
                int nel = 1;
                for ( int i = 0; i < ndim; i++ ) {
                    nel *= dims[ i ];
                    assertTrue( dims[ i ] != 0 );
                    if ( i < ndim - 1 ) {
                        assertTrue( dims[ i ] > 0 );
                    }
                }
                nels[ icol ] = nel;
            }
            assertTrue( cinfo.getContentClass().isArray() == cinfo.isArray() );
        }

        /* Get a RowSequence object and check it can't be read before a next. */
        RowSequence rseq = table.getRowSequence();
        try {
            rseq.getRow();
            assertTrue( false );
        }
        catch ( IllegalStateException e ) {
            // ok
        }

        /* Read all cells. */
        long lrow = 0L;
        while ( rseq.hasNext() ) {
            rseq.next();
            assertTrue( lrow == rseq.getRowIndex() );
            Object[] row = rseq.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {

                /* Check that elements got in different ways look the same. */
                Object cell = row[ icol ];
                Object val1 = cell;
                Object val2 = rseq.getCell( icol );
                Object val3 = null;
                if ( isRandom ) {
                    val3 = table.getCell( lrow, icol );
                }
                boolean isNull = cell == null;
                if ( isNull ) {
                    assertTrue( colinfos[ icol ].isNullable() );
                    assertTrue( val2 == null );
                    if ( isRandom ) {
                        assertTrue( val3 == null);
                    }
                }
                else {
                    String s1 = colinfos[ icol ]
                               .formatValue( val1, formatChars );
                    assertTrue( s1.equals( colinfos[ icol ]
                                          .formatValue( val2, formatChars ) ) );
                    if ( isRandom ) {
                        assertTrue( s1.equals( colinfos[ icol ]
                                              .formatValue( val3,
                                                            formatChars ) ) );
                    }
                }

                /* If the cell is an array, check it's the right shape. */
                if ( cell != null && cell.getClass().isArray() ) {
                    int nel = Array.getLength( cell );
                    if ( nels[ icol ] < 0 ) {
                        assertTrue( nel % nels[ icol ] == 0 );
                    }
                    else {
                        assertTrue( nels[ icol ] == nel );
                    }
                }

                /* Check the cell is of the declared type. */
                if ( cell != null ) {
                    if ( ! classes[ icol ]
                          .isAssignableFrom( cell.getClass() ) ) {
                        throw new AssertionError( "Column " + icol + ": " +
                            cell + " is a " + cell.getClass().getName() + 
                            " not a " + classes[ icol ].getName() );
                    }
                }
            }
            lrow++;
        }

        /* Check that the claimed number of rows is correct. */
        if ( nrow >= 0 ) {
            assertTrue( lrow == nrow );
        }
    }

    /**
     * Convenience method to get an <tt>int</tt> value from a <tt>long</tt>.
     * If the supplied long integer <tt>lval</tt> is out of the range
     * which can be represented in an <tt>int</tt>, then unlike a
     * typecast, this method will throw an <tt>IllegalArgumentException</tt>.
     *
     * @param  the <tt>long</tt> value to convert
     * @return an <tt>int</tt> value which has the same value as <tt>lval</tt>
     * @throws IllegalArgumentException  if the conversion cannot be done
     */
    public static int checkedLongToInt( long lval ) {
        int ival = (int) lval;
        if ( (long) ival == lval ) {
            return ival;
        }
        else {
            if ( ival < Integer.MIN_VALUE ) { 
                throw new IllegalArgumentException( "Out of supported range: "
                    + ival + " < Integer.MIN_VALUE" );
            }
            else if ( ival > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( "Out of supported range: "
                    + ival + " > Integer.MAX_VALUE" );
            }
            else {
                throw new AssertionError();
            }
        }
    }

    /**
     * Implements assertion semantics.  This differs from the <tt>assert</tt>
     * Java 1.4 language element in that the assertion is always done,
     * it doesn't depend on the JVM running in assertions-enabled mode.
     *
     * @param  ok  the thing that should be true
     * @throws  AssertionError  if <tt>ok</tt> is <tt>false</tt>
     */
    private static void assertTrue( boolean ok ) {
        if ( ! ok ) {
            throw new AssertionError();
        }
    }
}
