package uk.ac.starlink.ttools.build;

/**
 * Enumeration class describing the function categories used by the
 * JEL-accessible methods.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Heading {

    private final String userString_;
    private final String docSuffix_;
    private final String description_;

    /** Heading for general category of functions. */
    public static final Heading GENERAL = 
        new Heading( "General Functions", "-genFunc.html", new String[] {
            "Functions available for general use.",
            "These generally just produce a value from the arguments",
            "in the same way as a mathematical function.",
            "They can be used within definitions of synthetic columns,",
            "algebraically defined row subsets,",
            "or activation actions.",
        } );

    /** Heading for category of functions available only for activation. */
    public static final Heading ACTIVATION =
        new Heading( "Activation Functions", "-activFunc.html", new String[] {
            "Functions which may only be used in defining activation actions.",
            "These generally cause some action to occur,",
            "such as display of a file in an external viewer.",
        } );

    /** Array containing all known heading instances. */
    public static final Heading[] ALL_HEADINGS = new Heading[] {
        GENERAL, ACTIVATION,
    };

    /**
     * Private constructor.
     *
     * @param  userString  string for presentation to the user (heading text)
     * @param  docSuffix   text used for generating the filename
     * @param  descriptionLines  array of strings which will be concatenated
     *         to form the description of what this heading is about
     */
    private Heading( String userString, String docSuffix, 
                     String[] descriptionLines ) {
        userString_ = userString;
        docSuffix_ = docSuffix;
        StringBuffer dbuf = new StringBuffer();
        for ( int i = 0; i < descriptionLines.length; i++ ) {
            dbuf.append( descriptionLines[ i ] )
                .append( '\n' );
        }
        description_ = dbuf.toString();
    }

    /**
     * Returns heading text.
     */
    public String getUserString() {
        return userString_;
    }

    /**
     * Returns unique text suitable for forming a filename.
     */
    public String getDocSuffix() {
        return docSuffix_;
    }

    /**
     * Returns HTML description string.
     */
    public String getDescription() {
        return description_;
    }

    public String toString() {
        return userString_;
    }
    
}
