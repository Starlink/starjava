package uk.ac.starlink.task;

/**
 * An Exception thrown when a task is invoked with the wrong usage.
 */
public class UsageException extends TaskException {
    private String usage;

    /**
     * Constructs a UsageException without a message.  The default usage
     * message can usually be used.
     */
    public UsageException() {
        super();
    }

    /**
     * Constructs a UsageException with a message which contains the 
     * correct usage.  This should not include the task name, just 
     * a list of the arguments that should be used. 
     * It is only usually necessary to supply the correct usage string
     * if it differs from its default value.
     *
     * @param  the correct argument usage string
     */
    public UsageException( String correctUsage ) {
        usage = correctUsage;
    }
    public String getUsage() {
        return usage;
    }
}
