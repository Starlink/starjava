package uk.ac.starlink.topcat;

/**
 * Indicates the outcome of a user-initiated action, typically an
 * activation action.
 *
 * <p>As well as a success/failure flag, instances of this class
 * contain a message to be directed to the user who initiated the action.
 * In case of success this is a short indication of what happened,
 * and in case of failure it is an error message.
 * The context of the action may be assumed, and does not need to
 * be repeated here.
 * The message should be concise (one line).
 * In case of success, if there's really nothing to say, a null message
 * is permitted.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2018
 */
public class Outcome {

    private final String message_;
    private final boolean isSuccess_;
    private static final Outcome SUCCESS = new Outcome( true, null );

    /**
     * Constructor.
     *
     * @param  isSuccess   true for success, false for error
     * @param  message   one-line outcome message
     */
    protected Outcome( boolean isSuccess, String message ) {
        message_ = message;
        isSuccess_ = isSuccess;
    }

    /**
     * Returns the message text associated with this outcome.
     * 
     * @return  outcome message
     */
    public String getMessage() {
        return message_;
    }

    /**
     * Indicates whether the action was successful or not.
     *
     * @return   true for success, false for failure
     */
    public boolean isSuccess() {
        return isSuccess_;
    }

    /**
     * Returns a success outcome with no message.
     */
    public static Outcome success() {
        return SUCCESS;
    }

    /**
     * Returns a success outcome with a given message.
     *
     * @param  message   one-line message describing successful outcome
     */
    public static Outcome success( String message ) {
        return new Outcome( true, message );
    }

    /**
     * Returns a failure outcome with a given message.
     *
     * @param  message   one-line message giving reason for failure
     */
    public static Outcome failure( String message ) {
        return new Outcome( false, message );
    }

    /**
     * Returns a failure outcome based on an exception.
     *
     * @param  error  error that caused the action failure;
     *                if at all possible the message should explain
     *                in user-friendy terms what went wrong
     */
    public static Outcome failure( Throwable error ) {
        String errmsg = error.getMessage();
        return failure( errmsg == null ? error.toString() : errmsg );
    }
}
