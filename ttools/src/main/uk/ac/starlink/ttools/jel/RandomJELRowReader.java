package uk.ac.starlink.ttools.jel;

import gnu.jel.CompiledExpression;
import java.io.IOException;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.StarTable;

/**
 * Provides JELRowReader functionality for a random access table.
 * This abstract class adds the abstract method {@link #evaluateAtRow};
 * factory methods are provided to implement this in different ways
 * according to the requirements of multi-threaded usage.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Feb 2005
 */
public abstract class RandomJELRowReader extends StarTableJELRowReader {

    /**
     * Constructor.
     *
     * @param   table  table object
     */
    protected RandomJELRowReader( StarTable table ) {
        super( table );
    }

    /**
     * Evaluates a given compiled expression at a given row.
     * The returned value is wrapped up as an object if the result of
     * the expression is a primitive.
     *
     * @param  compEx  compiled expression
     * @param  lrow   row index
     * @return   expression result as an object
     */
    public abstract Object evaluateAtRow( CompiledExpression compEx, long lrow )
            throws Throwable;

    /**
     * Returns an instance that uses the threadsafe random access methods
     * of the supplied table.  The random access methods of the returned
     * object are synchronized, so that although it is safe for use from
     * multiple threads, it may not be efficient.
     *
     * @param  table  supplies data
     * @return   row reader
     */
    public static RandomJELRowReader
            createConcurrentReader( final StarTable table ) {
        return new RandomJELRowReader( table ) {
            private long lrow_ = -1;
            public long getCurrentRow() {
                return lrow_;
            }
            public Object getCell( int icol ) throws IOException {
                return table.getCell( lrow_, icol );
            }
            public synchronized Object evaluateAtRow( CompiledExpression compEx,
                                                      long lrow )
                    throws Throwable {
                lrow_ = lrow;
                return evaluate( compEx );
            }
        };
    }

    /**
     * Returns an instance that uses a RowAccess object from the table
     * for supplying data values.  This is only suitable for use from
     * a single thread.
     *
     * @param  table  table object
     * @param  racc   row access previously obtained from table
     * @return  row reader
     */
    public static RandomJELRowReader
            createAccessReader( StarTable table, final RowAccess racc ) {
        return new RandomJELRowReader( table ) {
            private long lrow_ = -1;
            public long getCurrentRow() {
                return lrow_;
            }
            public Object getCell( int icol ) throws IOException {
                return racc.getCell( icol );
            }
            public Object evaluateAtRow( CompiledExpression compEx, long lrow )
                    throws Throwable {
                if ( lrow != lrow_ ) {
                    racc.setRowIndex( lrow );
                    lrow_ = lrow;
                }
                return evaluate( compEx );
            }
        };
    }

    /**
     * Convenience method that obtains and uses a RowAccess from a given table.
     * Note it is not possible to close the RowAccess in this case.
     *
     * @param  table  table object
     * @return   result of
     *           <code>createAccessReader(table,table.getRowAccess())</code>
     */
    public static RandomJELRowReader createAccessReader( StarTable table )
            throws IOException {
        return createAccessReader( table, table.getRowAccess() );
    }
}
