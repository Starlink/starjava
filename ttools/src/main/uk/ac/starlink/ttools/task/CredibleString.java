package uk.ac.starlink.ttools.task;

/**
 * Aggregates a string value and a credibility assessment.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2017
 */
public class CredibleString {

    private final String value_;
    private final Credibility cred_;

    /**
     * Constructor.
     *
     * @param  value  string value to use
     * @param  cred  credibility for usage
     */
    public CredibleString( String value, Credibility cred ) {
        value_ = value;
        cred_ = cred;
    }

    /**
     * Returns the value.
     *
     * @return  value
     */
    public String getValue() {
        return value_;
    }

    /**
     * Returns the credibility.
     *
     * @return  value credibility
     */
    public Credibility getCredibility() {
        return cred_;
    }

    @Override
    public String toString() {
        return cred_ + ":" + value_;
    }
}
