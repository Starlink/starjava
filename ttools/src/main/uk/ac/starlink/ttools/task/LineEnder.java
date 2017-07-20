package uk.ac.starlink.ttools.task;

/**
 * Defines how line endings are handled when formatting stilts commands.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2017
 */
public class LineEnder {

    private final String name_;
    private final String eol_;
    private final boolean includesNewline_;

    /** Just a carriage return. */
    public static final LineEnder PLAIN;

    /** Backslash followed by carriage return (Un*x shells). */
    public static final LineEnder BACKSLASH;

    /** Caret followed by carriage return (DOS CMD). */
    public static final LineEnder CARET;

    /** Backtick followed by carriage return (Windows PowerShell). */
    public static final LineEnder BACKTICK;

    /** No line breaks, just one long line. */
    public static final LineEnder ONELINE;

    /** All options. */
    public static final LineEnder[] OPTIONS = {
        PLAIN = new LineEnder( "plain", "\n" ),
        BACKSLASH = new LineEnder( "backslash", "\\\n" ),
        CARET = new LineEnder( "caret", "^\n" ),
        BACKTICK = new LineEnder( "backtick", "`\n" ),
        ONELINE = new LineEnder( "none", "" ),
    };

    /**
     * Constructor.
     *
     * @param   name  instance name, suitable for presentation to users
     * @param   eol   end of line text
     */
    public LineEnder( String name, String eol ) {
        name_ = name;
        eol_ = eol;
        includesNewline_ = eol_.indexOf( '\n' ) >= 0;
    }

    /**
     * Returns the end of line text.
     *
     * @return  text separating two lines
     */
    public String getEndOfLine() {
        return eol_;
    }

    /**
     * Indicates whether a newline forms part of the EOL string.
     *
     * @return  true iff end of line includes a newline character
     */
    public boolean includesNewline() {
        return includesNewline_;
    }

    /**
     * Returns name.
     */
    @Override
    public String toString() {
        return name_;
    }
}
