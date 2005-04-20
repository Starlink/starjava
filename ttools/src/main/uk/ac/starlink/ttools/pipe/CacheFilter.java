package uk.ac.starlink.ttools.pipe;

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
public class CacheFilter implements ProcessingFilter, ProcessingStep {

    public String getName() {
        return "cache";
    }

    public String getFilterUsage() {
        return null;
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
