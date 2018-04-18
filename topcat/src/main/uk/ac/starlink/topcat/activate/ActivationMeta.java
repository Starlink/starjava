package uk.ac.starlink.topcat.activate;

/**
 * Additional information about the way a row activation request should
 * be carried out.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2018
 */
public abstract class ActivationMeta {

    /** Normal instance. */
    public static ActivationMeta NORMAL = new ActivationMeta() {
        public boolean isInhibitSend() {
            return false;
        }
    };

    /** Instance with isInhibitSend set to true. */
    public static ActivationMeta INHIBIT_SEND = new ActivationMeta() {
        public boolean isInhibitSend() {
            return true;
        }
    };

    /**
     * Indicates whether sending messages to external applications should
     * be prevented.  This is sometimes necessary to prevent loops in
     * which multiple applications play ping pong with row highlight
     * messages.
     *
     * @return  true  if sending row highlight messages to external
     *                applications should be inhibited
     */
    public abstract boolean isInhibitSend();
}
