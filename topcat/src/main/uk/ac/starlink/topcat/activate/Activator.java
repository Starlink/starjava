package uk.ac.starlink.topcat.activate;

import uk.ac.starlink.topcat.Outcome;

/**
 * Defines an action to be performed on rows of a known table.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2004
 */
public interface Activator {

    /**
     * Indicates how this activator's <code>activateRow</code> method
     * should be executed.
     * If true, it is intended to be invoked synchronously
     * on the Event Dispatch Thread.
     * If false, it is intended to be invoked asynchronously
     * on some less time-critical thread.
     * False should be returned if this activator may be time-consuming.
     *
     * @return   true if it is a good idea to invoke this activator on the EDT
     */
    boolean invokeOnEdt();

    /**
     * Invokes some kind of action on the table row indicated by a given index.
     *
     * <p>Note that the row index supplied is that from the TopcatModel's
     * DataModel, not the Apparent Table, so that remapping the row index
     * according to any current row sorting has already been applied.
     * Possibly that's not the way it should have been done, and it
     * may be desirable to change that in future, but at time of writing
     * that's the way it is.
     *
     * @param  lrow   row index
     * @param  meta   additional activation metadata if available;
     *                may be null if no special information is available
     * @return  outcome
     */
    Outcome activateRow( long lrow, ActivationMeta meta );
}
