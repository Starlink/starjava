package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Contains user-directed metadata to describe
 * user-supplied input data coordinate values used for plots.
 *
 * <p>A number of the setter methods return this object,
 * to facilitate declarations where method invocations are chained
 * so that the configured metadata object can be returned in a
 * single expression rather than multiple statements (cf StringBuffer).
 *
 * @author   Mark Taylor
 * @since    12 Sep 2014
 */
public class InputMeta {

    private final String shortName_;
    private final String longName_;
    private String shortDescription_;
    private String xmlDescription_;
    private String usage_;

    /**
     * Constructor.
     *
     * @param  shortName  basic name, no spaces, not case-sensitive,
     *                    to be used in command-line interface
     * @param  longName   name for use in GUI
     */
    public InputMeta( String shortName, String longName ) {
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

    /**
     * Returns a a short usage fragment that describes the form of the
     * data values represented by this coordinate.
     *
     * <p>Examples might be something like "<code>deg</code>"
     * or "<code>boolean</code>";
     *
     * @return   usage fragment
     */
    public String getValueUsage() {
        return usage_;
    }

    /**
     * Returns a short description string.
     *
     * @return   one-line description
     */
    public String getShortDescription() {
        return shortDescription_;
    }

    /**
     * Returns an XML string suitable for insertion into a user document.
     * It should be a sequence of one or more &lt;p&gt; elements.
     * If null, the short description can be used instead.
     *
     * @return  XML string, or null
     */
    public String getXmlDescription() {
        if ( xmlDescription_ != null ) {
            return xmlDescription_;
        }
        else {
            return new StringBuffer()
                .append( "<p>" )
                .append( getShortDescription().replaceAll( "&", "&amp;" )
                                              .replaceAll( "<", "&lt;" )
                                              .replaceAll( ">", "&gt;" ) )
                .append( "." )
                .append( "</p>" )
                .toString();
        }
    }

    /**
     * Sets a usage string which should give some clue how to specify this
     * key from a string.
     *
     * <p>Examples might be something like "<code>float</code>"
     * or "<code>boolean</code>";
     *
     * @param  usage  usage string
     * @return  this object, as a convenience
     */
     public InputMeta setValueUsage( String usage ) {
         usage_ = usage;
         return this;
     }

    /**
     * Sets a short description string.
     * This may be used as a prompt on the command line or a tooltip
     * in a GUI.  It should preferably be no longer than about 40 characters.
     *
     * @param  shortDescription  one-line description
     * @return  this object, as a convenience
     */
    public InputMeta setShortDescription( String shortDescription ) {
        shortDescription_ = shortDescription;
        return this;
    }

    /**
     * Sets a documentation string.  This is written in XML, intended for
     * presentation as user documentation.
     * The whole thing should be one or more &lt;p&gt; elements.
     *
     * <p>Permissible elements include p, ul, li, dl, dt, dd, em, code, strong.
     * Lists go inside paragraphs.
     *
     * @param  xmlDescription  documentation string
     * @return  this object, as a convenience
     */
    public InputMeta setXmlDescription( String xmlDescription ) {
        xmlDescription_ = xmlDescription;
        return this;
    }

    /**
     * Convenience method to set the XML description from an array of text
     * lines.
     * {@link #setXmlDescription(java.lang.String) setXmlDescription}
     * is called on the result of joining the lines with newline characters.
     *
     * @param   lines  lines of documentation string
     * @return  this object, as a convenience
     */
    public InputMeta setXmlDescription( String[] lines ) {
        setXmlDescription( PlotUtil.concatLines( lines ) );
        return this;
    }
}
