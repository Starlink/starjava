package uk.ac.starlink.topcat;

/**
 * Defines an action to be performed on a row.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Aug 2004
 */
public interface Activator {

    /**
     * Invokes some kind of action on row indicated by a given index.
     * If the return value is non-null it is treated as some kind of
     * message which may be conveyed to the user.
     *
     * @param  lrow  row index to activate
     * @return  true iff the activation was successful
     */
    String activateRow( long lrow );

    /** Activator instance which does nothing.  */
    public static final Activator NOP = new Activator() {
        public String activateRow( long lrow ) {
            return null;
        }
        public String toString() {
            return "(no action)";
        }
    };
}
