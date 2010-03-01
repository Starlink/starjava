package uk.ac.starlink.vo;

/**
 * Enumeration class which describes verbosity levels permitted by the
 * Cone Search standard.
 *
 * @see <a href="http://www.ivoa.net/Documents/REC/DAL/ConeSearch-20080222.html"
 *         >Cone Search standard</a>
 */
public class ConeVerbosity {

    private final int level_;
    private final String comment_;

    /**
     * Constructor.
     *
     * @param  level   verbosity level
     * @param  comment  textual description of level
     */
    private ConeVerbosity( int level, String comment ) {
        level_ = level;
        comment_ = comment;
    }

    /**
     * Returns a one-word characterisation of this verbosity level.
     *
     * @return   verbosity in text
     */
    public String getComment() {
        return comment_;
    }

    /**
     * Returns the integer verbosity level.
     *
     * @return   verbosity as an integer
     */
    public int getLevel() {
        return level_;
    }

    /**
     * Stringified level, suitable for use in a combo box.
     */
    public String toString() {
        return getLevel() + " (" + getComment() + ")";
    }

    public boolean equals( Object other ) {
        return other instanceof ConeVerbosity
            && ((ConeVerbosity) other).getLevel() == getLevel();
    }

    public int hashCode() {
        return getLevel();
    }

    /**
     * Returns legal verbosities for a cone search.
     *
     * @return   array of verbosities 1, 2, 3
     */
    public static ConeVerbosity[] getOptions() {
        return new ConeVerbosity[] {
            new ConeVerbosity( 1, "minimum" ),
            new ConeVerbosity( 2, "normal" ),
            new ConeVerbosity( 3, "maximum" ),
        };
    }
}
