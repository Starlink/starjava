package uk.ac.starlink.ttools;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Processing filter which sorts on one or more JEL expressions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mar 2005
 */
public class ExpressionSortFilter implements ProcessingFilter {

    /**
     * Whether the -subexpr flag is allows to specify additional expressions
     * of decreasing significance.  It works, but turn it off for now
     * because the syntax is a bit confusing.
     */
    private static final boolean ALLOW_SUBKEYS = false;

    public String getName() {
        return "sortexpr";
    }

    public String getFilterUsage() {
        return ALLOW_SUBKEYS ? "[-subexpr <expr> ...] <expr>"
                             : "<expr>";
    }

    public ProcessingStep createStep( Iterator argIt ) {
        List keyList = new LinkedList();
        while ( argIt.hasNext() ) {
            String arg = (String) argIt.next();
            if ( ALLOW_SUBKEYS && arg.equals( "-subkey" ) ) {
                argIt.remove();
                if ( argIt.hasNext() ) {
                    keyList.add( (String) argIt.next() );
                    argIt.remove();
                }
                else {
                    return null;
                }
            }
            else {
                argIt.remove();
                keyList.add( 0, arg );
                break;
            }
        }
        return new SortStep( (String[]) keyList.toArray( new String[ 0 ] ) );
    }

    private static class SortStep implements ProcessingStep {
        final String[] keys_;

        SortStep( String[] keys ) {
            keys_ = keys;
        }

        public StarTable wrap( StarTable baseTable ) throws IOException {
            baseTable = Tables.randomTable( baseTable );
            long lnrow = baseTable.getRowCount();
            if ( lnrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( 
                    "Sorry, can't sort tables with >2^31 rows" );
            }
            int nrow = (int) lnrow;
            Number[] rowMap = new Number[ nrow ];
            for ( int i = 0; i < nrow; i++ ) {
                rowMap[ i ] = new Integer( i );
            }
            Comparator keyComparator;
            try {
                keyComparator = new RowComparator( baseTable, keys_ );
            }
            catch ( CompilationException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
            try {
                Arrays.sort( rowMap, keyComparator );
            }
            catch ( SortException e ) {
                throw e.asIOException();
            }
            long[] rmap = new long[ nrow ];
            for ( int i = 0; i < nrow; i++ ) {
                rmap[ i ] = rowMap[ i ].longValue();
            }
            return new RowPermutedStarTable( baseTable, rmap );
        }
    }

    /** 
     * Comparator which will compare two objects which are Numbers 
     * representing row indices of a given table.
     */
    private static class RowComparator implements Comparator {

        final CompiledExpression[] compExs_;
        final int nexpr_;
        final RandomJELRowReader rowReader_;
        boolean nullsLast_;

        /**
         * Constructor.
         *
         * @param  table  table whose rows are to be examined
         * @param  keys   array of column identifiers; first is most
         *                significant for ordering, second next, etc
         */
        public RowComparator( StarTable table, String[] keys )
                throws CompilationException {
            nexpr_ = keys.length;

            /* Prepare compiled expressions for reading the data from 
             * table rows. */
            rowReader_ = new RandomJELRowReader( table );
            Library lib = JELUtils.getLibrary( rowReader_ );
            compExs_ = new CompiledExpression[ nexpr_ ];
            for ( int i = 0; i < nexpr_; i++ ) {
                compExs_[ i ] = Evaluator.compile( keys[ i ], lib );
            }
        }

        public int compare( Object o1, Object o2 ) {
            long row1 = ((Number) o1).longValue();
            long row2 = ((Number) o2).longValue();
            int c = 0;
            for ( int i = 0; i < nexpr_ && c == 0; i++ ) { 
                CompiledExpression compEx = compExs_[ i ];
                Object val1;
                Object val2;
                try {
                    val1 = rowReader_.evaluateAtRow( compEx, row1 );
                    val2 = rowReader_.evaluateAtRow( compEx, row2 );
                }
                catch ( Throwable e ) {
                    throw new SortException( "Sort error", e );
                }
                try {
                    c = compareValues( (Comparable) val1, (Comparable) val2 );
                }
                catch ( ClassCastException e ) {
                    throw new SortException( 
                        "Expression comparison error during sorting", e );
                }
            }
            return c;
        }

        /**
         * Compares the actual cell values.
         */
        private int compareValues( Comparable o1, Comparable o2 ) {
            boolean null1 = Tables.isBlank( o1 );
            boolean null2 = Tables.isBlank( o2 );
            if ( null1 && null2 ) {
                return 0;
            }
            else if ( null1 ) {
                return nullsLast_ ? -1 : +1;
            }
            else if ( null2 ) {
                return nullsLast_ ? +1 : -1;
            }
            else {
                return o1.compareTo( o2 );
            }
        }
    }

    private static class SortException extends RuntimeException {
        SortException( String msg, Throwable e ) {
            super( msg, e );
        }
        IOException asIOException() {
            Throwable error = getCause();
            return error instanceof IOException
                 ? (IOException) error
                 : (IOException) new IOException( error.getMessage() )
                                .initCause( error );
        }
    }
}
