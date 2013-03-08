package uk.ac.starlink.ttools.plot2.config;

/**
 * Contains metadata about configuration items.
 *
 * @author  Mark Taylor
 * @since   22 Feb 2013
 */
public class ConfigMeta {

    private final String shortName_;
    private final String longName_;

    /**
     * Constructor.
     *
     * @param  shortName  basic name, no spaces, not case-sensitive,
     *                    to be used in command-line interface
     * @param  longName   name for use in GUI
     */
    public ConfigMeta( String shortName, String longName ) {
        shortName_ = shortName;
        longName_ = longName;
    }

    /**
     * Returns the basic one-word name, not case-sensitive, for use in
     * command-line interfaces.
     *
     * @return  short name
     */
    public String getShortName() {
        return shortName_;
    }

    /**
     * Returns a potentially more descriptive name suitable for use in a GUI.
     *
     * @return  long name
     */
    public String getLongName() {
        return longName_;
    }
}
