package uk.ac.starlink.ttools.plot2;

/**
 * Contains documentation metadata describing a plot report item.
 *
 * @author   Mark Taylor
 * @since    9 Dec 2014
 */
public class ReportMeta {

    private final String shortName_;
    private final String longName_;

    /**
     * Constructor.
     *
     * @param  shortName  basic name, no spaces, not case-sensitive,
     *                    to be used in command-line interface
     * @param  longName   name for use in GUI
     */
    public ReportMeta( String shortName, String longName ) {
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
     * Returns a potentially more descriptive name for use in a GUI.
     *
     * @return   long name
     */
    public String getLongName() {
        return longName_;
    }
}
