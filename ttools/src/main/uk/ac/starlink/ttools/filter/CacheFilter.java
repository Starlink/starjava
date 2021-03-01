package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.MetaCopyStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;

/**
 * Processing step which caches the current table in a disk or memory
 * cache (according to the default {@link uk.ac.starlink.table.StoragePolicy}).
 * This is useful for efficiency reasons if downstream steps are 
 * going to make random-access or multiple use of an expensive step.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mar 2005
 */
public class CacheFilter extends BasicFilter implements ProcessingStep {

    public CacheFilter() {
        super( "cache", null );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Stores in memory or on disk a temporary copy of the table at",
            "this point in the pipeline.",
            "This can provide improvements in efficiency if there is",
            "an expensive step upstream and a step which requires",
            "more than one read of the data downstream.",
            "If you see an error like \"Can't re-read data from stream\"",
            "then adding this step near the start of the filters",
            "might help.",
            "</p>",
            "<p>The output table contains no code-level reference",
            "to the input table, so this filter can also be useful",
            "when managing tables that have become deeply nested",
            "as the result of successively applying many STILTS operations.",
            "</p>",
            "<p>The result of this filter is guaranteed to be random-access.",
            "</p>",
            "<p>See also the <ref id='random'><code>random</code></ref>",
            "filter, which caches only when the input table is not",
            "random-access.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt ) {
        return this;
    }

    public StarTable wrap( StarTable baseTable ) throws IOException {
        RowStore store = StoragePolicy.getDefaultPolicy().makeRowStore();
        store.acceptMetadata( new MetaOnlyTable( baseTable ) );
        try ( RowSequence rseq = baseTable.getRowSequence() ) {
            while ( rseq.next() ) {
                store.acceptRow( rseq.getRow() );
            }
            store.endRows();
            return store.getStarTable();
        }
    }

    /**
     * Skeleton table containing metadata copied from a template table,
     * but providing no data and retaining no reference to the template.
     * All data access methods will throw an UnsupportedOperationException.
     */
    private static class MetaOnlyTable extends AbstractStarTable {

        private final ColumnInfo[] cinfos_;
        private final List<DescribedValue> dvals_;
        private final long nrow_;

        /**
         * Constructor.
         *
         * @param  template  template table
         */
        MetaOnlyTable( StarTable template ) {
            int ncol = template.getColumnCount();
            cinfos_ = new ColumnInfo[ ncol ];
            for ( int ic = 0; ic < ncol; ic++ ) {
                cinfos_[ ic ] = new ColumnInfo( template.getColumnInfo( ic ) );
            }
            dvals_ = new ArrayList<DescribedValue>();
            for ( DescribedValue dval : template.getParameters() ) {
                dvals_.add( new DescribedValue( dval.getInfo(),
                                                dval.getValue() ) );
            }
            nrow_ = template.getRowCount();
            setName( template.getName() );
            setURL( template.getURL() );
        }
        public long getRowCount() {
            return nrow_;
        }
        public int getColumnCount() {
            return cinfos_.length;
        }
        public ColumnInfo getColumnInfo( int ic ) {
            return cinfos_[ ic ];
        }
        public RowSequence getRowSequence() {
            throw new UnsupportedOperationException();
        }
        public Object getCell( long irow, int icol ) {
            throw new UnsupportedOperationException();
        }
        public Object[] getRow( long irow ) {
            throw new UnsupportedOperationException();
        }
    }
}
