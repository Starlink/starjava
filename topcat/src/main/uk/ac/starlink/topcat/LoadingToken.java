package uk.ac.starlink.topcat;

/**
 * Object corresponding to a table currently in the process of being loaded.
 *
 * @author   Mark Taylor
 * @since    9 April 2009
 */
public class LoadingToken {

    private String targetText_;
    private String progText_;

    /**
     * Constructor.
     *
     * @param  targetText  short description of item being loaded
     */
    public LoadingToken( String targetText ) {
        targetText_ = targetText;
    }

    /**
     * Sets the target text.
     *
     * @param  targetText  a short description of the item being loaded.
     */
    public void setTarget( String targetText ) {
        targetText_ = targetText;
    }

    /**
     * Sets text to display which indicates progress.
     *
     * @param  progText  progress text
     */
    public void setProgress( String progText ) {
        progText_ = progText;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer()
            .append( "Loading " )
            .append( targetText_ );
        if ( progText_ != null ) {
            sbuf.append( ' ' )
                .append( progText_ );
        }
        return sbuf.toString();
    }
}
