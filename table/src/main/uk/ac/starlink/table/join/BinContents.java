package uk.ac.starlink.table.join;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes the contents of all the bins used in a matching operation.
 * This is basically a mapping from bin id (one of the objects
 * returned from a {@link MatchEngine#getBins} call) to a list of rows.
 *
 * <p>The way you would normally use it is for each row you are 
 * interested in, call {@link MatchEngine#getBins}, and then
 * associate it with each of the returned bins by calling 
 * {@link #putRowInBin} on this object.
 * When you're done, you can call {@link #getRowLinks} to find all the
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

    /**
     * Bin ID -> value map.  The value represents a set of RowRefs.
     * For memory efficiency reasons, the values can have two different
     * classes; if there's only one RowRef to store, it's put in there 
     * directly.  If there are two or more, they're stored as a List - 
     * a currently LinkedList, since I reckon (without testing) it's 
     * likely to be more efficient.
     */
    private final Map map_ = new HashMap();
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
    }

    /**
     * Associates a row with a bin.
     *
     * @param   key  bin ID
     * @param   row  row reference
     */
    public void putRowInBin( Object key, RowRef row ) {
        nrow_++;
        Object value = map_.get( key );

        /* If the key doesn't exist in the map, create a new entry with a
         * single RowRef in it.  This has the same meaning as a list 
         * containing a single RowRef, but it's cheaper on memory. */
        if ( value == null ) {
            assert ! map_.containsKey( key );
            map_.put( key, row );
        }
        else {
            List rowList;

            /* If the entry is a single RowRef, replace it by a one-element
             * list containing that RowRef (ready to add a new one). */
            if ( value instanceof RowRef ) {
                rowList = new LinkedList();
                rowList.add( (RowRef) value );
                map_.put( key, rowList );
            }

            /* Otherwise, it must already be a list. */
            else {
                rowList = (List) value;
            }

            /* Add the new row. */
            rowList.add( row );
        }
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
     * Returns a set of {@link RowLink} objects which represent all the
     * distinct groups of RowRefs associated with any of the bins.
     * Only RowLinks containing more than one entry are put in the 
     * resulting set, since the others aren't interesting.
     *
     * <p><strong>Note</strong> that this method will 
     * (for memory efficiency purposes) 
     * clear out the map; following a call to this method this 
     * object is effecctively empty of any data.
     *
     * @return   set of <tt>RowLinks</tt>
     */
    public Set getRowLinks() throws InterruptedException {
        indicator_.logMessage( nrow_ + " row refs in " + 
                               map_.size() + " bins" );
        indicator_.logMessage( "(average bin occupancy " + 
                               ( (float) nrow_ / (float) map_.size() ) + ")" );
        Set links = new HashSet();
        indicator_.startStage( "Consolidating potential match groups" );
        double nl = (double) map_.size();
        int il = 0;
        for ( Iterator it = map_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Object value = entry.getValue();

            /* If there is more than one RowRef, create and store the
             * corresponding RowLink. */
            if ( value instanceof List ) {
                links.add( new RowLink( (List) value ) );
            }

            /* If there's only one RowRef, it doesn't constitute any 
             * interesting kind of potential match - throw it away. */
            else {
                assert value instanceof RowRef;
            }

            /* Remove the entry from the map as we're going along, to
             * save on memory. */
            it.remove();
            indicator_.setLevel( il++ / nl );
        }
        assert map_.isEmpty();
        nrow_ = 0;
        indicator_.endStage();
        return links;
    }
}
