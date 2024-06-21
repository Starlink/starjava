package uk.ac.starlink.table;

/**
 * Describes an object which can be used to store table data.
 * If you have a <code>RowStore</code> you can stream table data into it,
 * and then retrieve it as a random-access StarTable later.  This interface
 * abstracts that functionality so you don't need to worry how the 
 * storage is handled.  You should usually obtain a <code>RowStore</code> 
 * instance from a {@link StoragePolicy}.
 *
 * @author   Mark Taylor (Starlink)
 * @since    30 Jul 2004
 * @see      StoragePolicy
 */
public interface RowStore extends TableSink {

    /**
     * Obtains a StarTable which contains the data and metadata that have
     * been written into this sink.  In general it is <em>only</em>
     * legal to call this method following a call to 
     * {@link TableSink#endRows}; failing to observe this sequence may
     * earn you an <code>IllegalStateException</code>
     *
     * @return  a random-access StarTable containing the written rows
     */
    StarTable getStarTable();
}
