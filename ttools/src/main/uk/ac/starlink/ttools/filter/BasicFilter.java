package uk.ac.starlink.ttools.filter;

/**
 * Basic implementation of ProcessingFilter methods.
 * Utility superclass for implementing concrete ProcessingFilter.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2005
 */
public abstract class BasicFilter implements ProcessingFilter {

    private final String name_;
    private final String usage_;

    /**
     * Constructor.
     *
     * @param  name  filter name
     * @param  usage  filter usage
     */
    protected BasicFilter( String name, String usage ) {
        name_ = name;
        usage_ = usage;
    }

    public String getName() {
        return name_;
    }

    public String getUsage() {
        return usage_;
    }
}
