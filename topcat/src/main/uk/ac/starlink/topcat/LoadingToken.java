package uk.ac.starlink.topcat;

/**
 * Object corresponding to a table currently in the process of being loaded.
 *
 * @author   Mark Taylor
 * @since    9 April 2009
 */
public class LoadingToken {

    private final String text_;

    /**
     * Constructor.
     *
     * @param  text  short description of item being loaded
     */
    public LoadingToken( String text ) {
        text_ = text;
    }

    public String toString() {
        return "Loading " + text_;
    }
}
