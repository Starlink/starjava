package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Tokenizer;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;

/**
 * Filter for returning the first (or last) few rows of a sorted table.
 * This is functionally equivalent to using <code>sort</code> followed
 * by <code>head</code>, but the algorithm is different (a complete
 * sort is not necessary), so this way will usually be faster.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2005
 */
public class SortHeadFilter extends BasicFilter {

    public SortHeadFilter() {
        super( "sorthead",
               "[-tail] [-down] [-nullsfirst] <nrows> <key-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Performs a sort on the table according to the value of",
            "one or more algebraic expressions, retaining only",
            "<code>&lt;nrows&gt;</code> rows at the head",
            "of the resulting sorted table.",
            "The sort key expressions appear,",
            "as separate (space-separated) words,",
            "in <code>&lt;key-list&gt;</code>; sorting is done on the",
            "first expression first, but if that results in a tie then",
            "the second one is used, and so on.",
            "Each expression must evaluate to a type that",
            "it makes sense to sort, for instance numeric.",
            "</p>",
            "<p>If the <code>-tail</code> flag is used, then the",
            "last <code>&lt;nrows&gt;</code> rows rather than the first",
            "ones are retained.",
            "</p>",
            "<p>If the <code>-down</code> flag is used the sort order is",
            "descending rather than ascending.",
            "</p>",
            "<p>Blank entries are by default considered to come at the end",
            "of the collation sequence, but if the <code>-nullsfirst</code>",
            "flag is given then they are considered to come at the start",
            "instead.",
            "</p>",
            "<p>This filter is functionally equivalent to using",
            "<code>sort</code> followed by <code>head</code>,",
            "but it can be done in one pass and is usually cheaper",
            "on memory and faster, as long as <code>&lt;nrows&gt;</code>",
            "is significantly lower than the size of the table.",
            "</p>",
            explainSyntax( new String[] { "key-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        boolean up = true;
        boolean nullsLast = true;
        boolean keepHead = true;
        int nrows = -1;
        String exprs = null;
        while ( argIt.hasNext() || nrows < 0 || exprs == null ) {
            String arg = argIt.next();
            if ( arg.equals( "-tail" ) ) {
                argIt.remove();
                keepHead = false;
            }
            else if ( arg.equals( "-down" ) ) {
                argIt.remove();
                up = false;
            }
            else if ( arg.equals( "-nullsfirst" ) ) {
                argIt.remove();
                nullsLast = false;
            }
            else if ( nrows < 0 ) {
                argIt.remove();
                try {
                    nrows = Integer.parseInt( arg );
                }
                catch ( NumberFormatException e ) {
                    throw new ArgException( "<nrows> not numeric: " + arg );
                }
                if ( nrows <= 0 ) {
                    throw new ArgException( "Non-positive <nrows>: " + nrows );
                }
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

        /* Return a step implementation. */
        return new SortHeadStep( keys, up, nullsLast, nrows, keepHead );
    }

    private static class SortHeadStep implements ProcessingStep {
        final String[] keys_;
        final boolean up_;
        final boolean nullsLast_;
        final int nrows_;
        final boolean keepHead_;

        SortHeadStep( String[] keys, boolean up, boolean nullsLast,
                      int nrows, boolean keepHead ) {
            keys_ = keys;
            up_ = up;
            nullsLast_ = nullsLast;
            nrows_ = nrows;
            keepHead_ = keepHead;
        }

        public StarTable wrap( StarTable baseTable ) throws IOException {

            /* Compile expressions for the specified sort keys. */
            SequentialJELRowReader rseq =
                new SequentialJELRowReader( baseTable );
            Library lib = JELUtils.getLibrary( rseq );
            int nkey = keys_.length;
            CompiledExpression[] compExs = new CompiledExpression[ nkey ];
            try {
                for ( int i = 0; i < nkey; i++ ) {
                    compExs[ i ] =
                        JELUtils.compile( lib, baseTable, keys_[ i ] );
                }
            }
            catch ( CompilationException e ) {
                throw (IOException) new IOException( "Bad sort key(s)" )
                                   .initCause( e );
            }

            /* Prepare a SortedMap which will keep the top nrows rows.
             * The map keys are the Object arrays of sort key values, and the
             * map values are the full row Object arrays. */
            SortedMap<SortKey,Object[]> headMap =
                new TreeMap<SortKey,Object[]>();

            /* Iterate over rows in the input table. */
            while ( rseq.next() ) {

                /* Get the key values for this row. */
                SortKey sortKey = new SortKey( rseq, compExs );

                /* If there are not enough rows in the retention map yet,
                 * insert this one unconditionally. */
                if ( headMap.size() < nrows_ ) {
                    headMap.put( sortKey, rseq.getRow() );
                }

                /* Otherwise, if this row sorts nearer the top/bottom 
                 * than the current most marginal item, replace that
                 * with this one. */
                else {
                    SortKey marginal = keepHead_ ? headMap.lastKey()
                                                 : headMap.firstKey();
                    if ( ( marginal.compareTo( sortKey ) 
                           * ( keepHead_ ? +1 : -1 ) ) > 0 ) {
                        assert headMap.size() == nrows_;
                        headMap.remove( marginal );
                        headMap.put( sortKey, rseq.getRow() );
                        assert headMap.size() == nrows_;
                    }
                }
            }

            /* Prepare and return a new table containing the rows from
             * the retention map in order. */
            RowListStarTable outTable = new RowListStarTable( baseTable );
            for ( Iterator<Map.Entry<SortKey,Object[]>> it =
                      headMap.entrySet().iterator(); it.hasNext(); ) {
                outTable.addRow( it.next().getValue() );
                it.remove();
            }
            return outTable;
        }

        /**
         * Helper class used as the sort key for table rows.
         */
        private class SortKey implements Comparable<SortKey> {
            final int nkey1_;
            final Object[] keyVals_;

            SortKey( StarTableJELRowReader jelly, CompiledExpression[] compExs )
                    throws IOException {
                nkey1_ = compExs.length + 1;
                keyVals_ = new Object[ nkey1_ ];
                for ( int i = 0; i < nkey1_ - 1; i++ ) {
                    try {
                        keyVals_[ i ] = jelly.evaluate( compExs[ i ] );
                    }
                    catch ( IOException e ) {
                        throw e;
                    }
                    catch ( Throwable e ) {
                        throw (IOException) new IOException( e.getMessage() )
                                           .initCause( e );
                    }
                }
                keyVals_[ nkey1_ - 1 ] = Long.valueOf( jelly.getCurrentRow() );
            }

            public int compareTo( SortKey other ) {
                int c = 0;
                for ( int i = 0; i < nkey1_ && c == 0; i++ ) {
                    c = compareValues( this.keyVals_[i], other.keyVals_[i] );
                }
                return up_ ? c : -c;
            }
        }

        /**
         * Compares actual object values.
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
                if ( o1 instanceof Comparable &&
                     o2 instanceof Comparable ) {
                    return ((Comparable) o1).compareTo( (Comparable) o2 );
                }
                else {
                    assert false;
                    return 0;
                }
            }
        }
    }
}
