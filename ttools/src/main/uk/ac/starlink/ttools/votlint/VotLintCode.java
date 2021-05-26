package uk.ac.starlink.ttools.votlint;

/**
 * Provides a label for a message passed to a VotLint context.
 * This is just a typed wrapper around a 3-letter string,
 * intended to identify message instances that are essentially
 * reporting problems that are the same or similar.
 *
 * @author   Mark Taylor
 * @since    26 May 2021
 */
public class VotLintCode {

    private final String txt_;

    /**
     * Constructor.
     *
     * @param  txt  3-character string identifying this code;
     *              conventionally the characters are upper case alphanumerics
     * @throws  IllegalArgumentException   if it's not three characters long
     */
    public VotLintCode( String txt ) {
        if ( txt.trim().length() != 3 ) {
            throw new IllegalArgumentException( "Not 3 characters: " + txt );
        }
        txt_ = txt;
    }

    /**
     * Returns the three-letter identifier for this object.
     *
     * @return  3-letter code
     */
    public String getCode() {
        return txt_;
    }

    @Override
    public int hashCode() {
        return txt_.hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof VotLintCode ) {
            VotLintCode other = (VotLintCode) o;
            return this.txt_.equals( other.txt_ );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return txt_;
    }
}
