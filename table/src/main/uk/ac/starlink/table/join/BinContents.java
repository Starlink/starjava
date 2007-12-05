package uk.ac.starlink.table.join;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Describes the contents of all the bins used in a matching operation.
 * This is basically a mapping from bin id (one of the objects
 * returned from a {@link MatchEngine#getBins} call) to a list of rows.
 *
 * <p>The way you would normally use it is for each row you are 
 * interested in, call {@link MatchEngine#getBins}, and then
 * associate it with each of the returned bins by calling 
 * {@link #putRowInBin} on this object.
 * When you're done, you can call {@link #addRowLinks} to find all the
 * distinct sets of rows that appear as bin contents.
 * Because of the declared semantics of the <tt>getBins</tt> call, 
 * what this gives you is a list of all the groups of rows which 
 * might be mutually matching according to the
 * {@link MatchEngine#matchScore} method.
 *
 * @author   Mark Taylor (Starlink)
 * @since    6 Aug 2004
 */
public class BinContents {

    private final Map map_;
    private final ListStore listStore_;
    private final ProgressIndicator indicator_;
    private long nrow_;

    /**
     * Constructs a new BinContents object.
     *
     * @param  indicator  progress indicator to use for logging time-consuming
     *         operations
     */
    public BinContents( ProgressIndicator indicator ) {
        indicator_ = indicator;
        map_ = new HashMap();
        listStore_ = ListStores.createListStore();
    }

    /**
     * Associates a row with a bin.
     *
     * @param   key  bin ID
     * @param   row  row reference
     */
    public void putRowInBin( Object key, RowRef row ) {
        nrow_++;
        map_.put( key, listStore_.addItem( map_.get( key ), row ) );
    }

    /**
     * Indicates whether the given key exists in this map; that is 
     * whether any rows have yet been associated with it.
     *
     * @param  key  bin ID
     * @return  true  iff <tt>putRowInBin(key,...)</tt> has yet been called
     */
    public boolean containsKey( Object key ) {
        return map_.containsKey( key );
    }

    /**
     * Returns the number of rows currently contained in this object.
     *
     * @return  row count
     */
    public long getRowCount() {
        return nrow_;
    }

    /**
     * Calculates a set of {@link RowLink} objects which represent all the
     * distinct groups of RowRefs associated with any of the bins,
     * and adds them to a given Set object.
     * Only RowLinks containing more than one entry are put in the 
     * resulting set, since the others aren't interesting.
     *
     * <p><strong>Note</strong> that this method will 
     * (for memory efficiency purposes) 
     * clear out the map; following a call to this method this 
     * object is effectively empty of any data.
     *
     * @param   links  set to which links will be added
     */
    public void addRowLinks( LinkSet links ) throws InterruptedException {
        indicator_.logMessage( nrow_ + " row refs in " + 
                               map_.size() + " bins" );
        indicator_.logMessage( "(average bin occupancy " + 
                               ( (float) nrow_ / (float) map_.size() ) + ")" );
        indicator_.startStage( "Consolidating potential match groups" );
        double nl = (double) map_.size();
        int il = 0;
        for ( Iterator it = map_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            List refList = listStore_.getList( entry.getValue() );

            /* If there is more than one RowRef, create and store the
             * corresponding RowLink.  Items with zero or 1 entry are 
             * not potential matches - take no action. */
            if ( refList.size() > 1 ) {
                links.addLink( new RowLink( refList ) );
            }

            /* Remove the entry from the map as we're going along, to
             * save on memory. */
            it.remove();
            indicator_.setLevel( ++il / nl );
        }
        assert map_.isEmpty();
        nrow_ = 0;
        indicator_.endStage();
    }
}
