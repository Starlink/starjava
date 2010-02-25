package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;

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
            "<p>The result of this filter is guaranteed to be random-access.",
            "</p>",
            "<p>See also the <ref id='random'><code>random</code></ref>",
            "filter, which caches only when the input table is not",
            "random-access.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) {
        return this;
    }

    public StarTable wrap( StarTable baseTable ) throws IOException {
        RowStore store = StoragePolicy.getDefaultPolicy().makeRowStore();
        Tables.streamStarTable( baseTable, store );
        return store.getStarTable();
    }
}
