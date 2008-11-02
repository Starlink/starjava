package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
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
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Tokenizer;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;

/**
 * Processing filter which sorts on one or more JEL expressions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mar 2005
 */
public class SortFilter extends BasicFilter {

    public SortFilter() {
        super( "sort",
               "[-down] [-nullsfirst] " 
             + "<key-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Sorts the table according to the value of one or more",
            "algebraic expressions.",
            "The sort key expressions appear,",
            "as separate (space-separated) words,",
            "in <code>&lt;key-list&gt;</code>; sorting is done on the",
            "first expression first, but if that results in a tie then",
            "the second one is used, and so on.",
            "</p>",
            "<p>Each expression must evaluate to a type that",
            "it makes sense to sort, for instance numeric.",
            "If the <code>-down</code> flag is used, the sort order is",
            "descending rather than ascending.",
            "</p>",
            "<p>Blank entries are by default considered to come at the end",
            "of the collation sequence, but if the <code>-nullsfirst</code>",
            "flag is given then they are considered to come at the start",
            "instead.",
            "</p>",
            explainSyntax( new String[] { "key-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        boolean up = true;
        boolean nullsLast = true;
        String exprs = null;
        while ( argIt.hasNext() && exprs == null ) {
            String arg = (String) argIt.next();
            if ( arg.equals( "-down" ) ) {
                argIt.remove();
                up = false;
            }
            else if ( arg.equals( "-nullsfirst" ) ) {
                argIt.remove();
                nullsLast = false;
            }
            else if ( exprs == null ) {
                argIt.remove();
                exprs = arg;
            }
        }
        if ( exprs == null ) {
            throw new ArgException( "No sort keys given" );
        }

        /* Split the sort keys up into words. */
        String[] keys;
        try {
            keys = Tokenizer.tokenizeWords( exprs );
            if ( keys.length == 0 ) {
                throw new ArgException( "No sort keys given" );
            }
        }
        catch ( TaskException e ) {
            throw new ArgException( "Bad <key-list>: " + exprs, e );
        }

        /* Return the appropriate step implementation. */
        return new SortStep( keys, up, nullsLast );
    }

    /**
     * Step implementation which sorts all rows using a random table.
     */
    private static class SortStep implements ProcessingStep {
        final String[] keys_;
        final boolean up_;
        final boolean nullsLast_;

        SortStep( String[] keys, boolean up, boolean nullsLast ) {
            keys_ = keys;
            up_ = up;
            nullsLast_ = nullsLast;
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
                keyComparator = new RowComparator( baseTable, keys_, up_,
                                                   nullsLast_ );
            }
            catch ( CompilationException e ) {
                throw (IOException) new IOException( "Bad sort key(s)" )
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
        boolean up_;
        boolean nullsLast_;

        /**
         * Constructor.
         *
         * @param  table  table whose rows are to be examined
         * @param  keys   array of column identifiers; first is most
         *                significant for ordering, second next, etc
         * @param   up  true for sorting into ascending order, false for
         *          descending order
         * @param   nullsLast  true if blank values should be considered
         *          last in the collation order, false if they should
         *          be considered first
         */
        public RowComparator( StarTable table, String[] keys, boolean up,
                              boolean nullsLast )
                throws CompilationException {
            nexpr_ = keys.length;
            up_ = up;
            nullsLast_ = nullsLast;

            /* Prepare compiled expressions for reading the data from 
             * table rows. */
            rowReader_ = new RandomJELRowReader( table );
            Library lib = JELUtils.getLibrary( rowReader_ );
            compExs_ = new CompiledExpression[ nexpr_ ];
            for ( int i = 0; i < nexpr_; i++ ) {
                compExs_[ i ] = JELUtils.compile( lib, table, keys[ i ] );
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
            return up_ ? c : -c;
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
                return nullsLast_ ? +1 : -1;
            }
            else if ( null2 ) {
                return nullsLast_ ? -1 : +1;
            }
            else {
                return o1.compareTo( o2 );
            }
        }
    }

    /**
     * Helper class defining private exception type.
     * Used to smuggle a checked exception out of a sort comparison.
     */
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
