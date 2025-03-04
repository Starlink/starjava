package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Tokenizer;
import uk.ac.starlink.ttools.gpl.SortUtils;
import uk.ac.starlink.ttools.jel.RandomJELEvaluator;

/**
 * Processing filter which sorts on one or more JEL expressions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mar 2005
 */
public class SortFilter extends BasicFilter {

    /** Boundary for default sequential/parallel sort behaviour threshold. */
    private static final int PARALLEL_THRESHOLD = 100_000;

    public SortFilter() {
        super( "sort",
               "[-down] [-nullsfirst] [-[no]parallel] " 
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
            "<p>By default, sorting is done sequentially for small tables",
            "and in parallel for large tables, but this can be controlled",
            "with the <code>-parallel</code> or <code>-noparallel</code> flag.",
            "</p>",
            explainSyntax( new String[] { "key-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        boolean up = true;
        boolean nullsLast = true;
        Boolean isParallel = null;
        String exprs = null;
        while ( argIt.hasNext() && exprs == null ) {
            String arg = argIt.next();
            if ( arg.equals( "-down" ) ) {
                argIt.remove();
                up = false;
            }
            else if ( arg.equals( "-nullsfirst" ) ) {
                argIt.remove();
                nullsLast = false;
            }
            else if ( arg.equals( "-parallel" ) ) {
                argIt.remove();
                isParallel = Boolean.TRUE;
            }
            else if ( arg.equals( "-noparallel" ) ) {
                argIt.remove();
                isParallel = Boolean.FALSE;
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
        return new SortStep( keys, up, nullsLast, isParallel );
    }

    /**
     * Step implementation which sorts all rows using a random table.
     */
    private static class SortStep implements ProcessingStep {
        final String[] keys_;
        final boolean up_;
        final boolean nullsLast_;
        final Boolean isParallel_;

        SortStep( String[] keys, boolean up, boolean nullsLast,
                  Boolean isParallel ) {
            keys_ = keys;
            up_ = up;
            nullsLast_ = nullsLast;
            isParallel_ = isParallel;
        }

        public StarTable wrap( StarTable baseTable ) throws IOException {
            baseTable = Tables.randomTable( baseTable );
            long lnrow = baseTable.getRowCount();
            if ( lnrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( 
                    "Sorry, can't sort tables with >2^31 rows" );
            }
            int nrow = (int) lnrow;
            boolean isParallel = isParallel_ == null
                               ? nrow > PARALLEL_THRESHOLD
                               : isParallel_.booleanValue();
            int[] rowMap = new int[ nrow ];
            for ( int i = 0; i < nrow; i++ ) {
                rowMap[ i ] = i;
            }
            try ( ComparatorManager cmpMgr =
                      new ComparatorManager( baseTable, keys_,
                                             up_, nullsLast_ ) ) {
                if ( isParallel ) {
                    SortUtils.parallelIntSort( rowMap, cmpMgr::getComparator );
                }
                else {
                    SortUtils.intSort( rowMap, cmpMgr.getComparator() );
                }
            }
            catch ( CompilationException e ) {
                throw (IOException) new IOException( "Bad sort key(s)" )
                                   .initCause( e );
            }
            catch ( SortException e ) {
                throw e.asIOException();
            }
            long[] rmap = new long[ nrow ];
            for ( int i = 0; i < nrow; i++ ) {
                rmap[ i ] = rowMap[ i ];
            }
            return new RowPermutedStarTable( baseTable, rmap );
        }
    }

    /** 
     * IntComparator which will compare two integers
     * representing row indices of a given table.
     * Instances are not thread-safe.
     */
    private static class RowComparator implements IntComparator, Closeable {

        final int nexpr_;
        final RandomJELEvaluator[] evaluators_;
        final boolean up_;
        final boolean nullsLast_;

        /**
         * Constructor.
         *
         * @param  keyEvaluators  array of evaluators for sort key values;
         *                        first is most significant for ordering,
         *                        second next, etc
         * @param   up  true for sorting into ascending order, false for
         *          descending order
         * @param   nullsLast  true if blank values should be considered
         *          last in the collation order, false if they should
         *          be considered first
         */
        public RowComparator( RandomJELEvaluator[] keyEvaluators, boolean up,
                              boolean nullsLast ) {
            evaluators_ = keyEvaluators;
            up_ = up;
            nullsLast_ = nullsLast;
            nexpr_ = evaluators_.length;
        }

        public void close() throws IOException {
            for ( RandomJELEvaluator rev : evaluators_ ) {
                rev.close();
            }
        }

        public int compare( int row1, int row2 ) {
            int c = 0;
            for ( int i = 0; i < nexpr_ && c == 0; i++ ) { 
                RandomJELEvaluator evaluator = evaluators_[ i ];
                Object val1;
                Object val2;
                try {
                    val1 = evaluator.evaluateObject( row1 );
                    val2 = evaluator.evaluateObject( row2 );
                }
                catch ( IOException e ) {
                    throw new SortException( "Sort error", e );
                }
                try {
                    c = compareValues( val1, val2 );
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
        @SuppressWarnings("unchecked")
        private int compareValues( Object o1, Object o2 ) {
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
                return ((Comparable<Object>) o1).compareTo( (Comparable) o2 );
            }
        }
    }

    /**
     * Supplier for RowComparators that keeps track of them and will
     * close them when it is closed.
     */
    private static class ComparatorManager implements Closeable {

        final List<Supplier<RandomJELEvaluator>> evaluatorSuppliers_;
        final boolean up_;
        final boolean nullsLast_;
        final Collection<RowComparator> comparators_;

        /**
         * Constructor.
         *
         * @param  table  table
         * @param  keys   JEL expressions for sort keys, most significant first
         * @param   up  true for sorting into ascending order, false for
         *          descending order
         * @param   nullsLast  true if blank values should be considered
         *          last in the collation order, false if they should
         *          be considered first
         */
        ComparatorManager( StarTable table, String[] keys, boolean up,
                           boolean nullsLast )
                throws CompilationException {
            up_ = up;
            nullsLast_ = nullsLast;
            evaluatorSuppliers_ = new ArrayList<Supplier<RandomJELEvaluator>>();
            comparators_ = ConcurrentHashMap.newKeySet();
            for ( String key : keys ) {
                evaluatorSuppliers_
               .add( RandomJELEvaluator.createEvaluatorSupplier( table, key ) );
            }
        }

        /**
         * Returns a RowComparator instance.  This instance is not thread-safe.
         *
         * @return  comparator
         */
        RowComparator getComparator() {
            RandomJELEvaluator[] evaluators =
                evaluatorSuppliers_.stream()
                                   .map( Supplier::get )
                                   .toArray( n -> new RandomJELEvaluator[ n ] );
            RowComparator cmp =
                new RowComparator( evaluators, up_, nullsLast_ );
            comparators_.add( cmp );
            return cmp;
        }

        /**
         * Closes any comparators that have been dispatched from this object.
         */
        public void close() throws IOException {
            for ( RowComparator cmp : comparators_ ) {
                cmp.close();
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
