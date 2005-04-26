package uk.ac.starlink.ttools.pipe;

/**
 * Checked exception thrown when arguments encountered on the command 
 * line are illegal.
 * As well as being checked, this differs from IllegalArgumentException 
 * in that it can have a <code>usageFragment</code> member.
 * Where appropriate, this should be set to a usage message which 
 * demonstrates how the arguments ought to look.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Apr 2005
 */
public class ArgException extends Exception {
 
    private String usageFragment_;

    /** 
     * Constructor.
     * 
     * @param   basic message
     */
    public ArgException( String message ) {
        this( message, null );
    }

    /**
     * Constructor which sets usage fragment.
     *
     * @param   message  basic message
     * @param   usageFragment   usage message explaining what went wrong
     */
    public ArgException( String message, String usageFragment ) {
        super( message );
        usageFragment_ = usageFragment;
    }

    /**
     * Sets the usage fragment.
     *
     * @param  usageFragment   usage message
     */
    public void setUsageFragment( String usageFragment ) {
        usageFragment_ = usageFragment;
    }

    /**
     * Returns the usage fragment.
     *
     * @return  usage fragment
     */
    public String getUsageFragment() {
        return usageFragment_;
    }

}
