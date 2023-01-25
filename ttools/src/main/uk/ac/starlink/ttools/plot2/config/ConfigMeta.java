package uk.ac.starlink.ttools.plot2.config;

import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Contains metadata about configuration items.
 *
 * <p>A number of the setter methods return this object,
 * to facilitate declarations where method invocations are chained
 * so that the configured metadata object can be returned in a
 * single expression rather than multiple statements (cf StringBuffer).
 *
 * @author  Mark Taylor
 * @since   22 Feb 2013
 */
public class ConfigMeta {

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

    /**
     * Returns a usage string which should some clue how to specify this
     * key from a string.
     *
     * <p>Examples might be something like "<code>true|false</code>"
     * or "<code>&lt;RRGGBB&gt;</code>".
     *
     * @return  usage string
     */
    public String getStringUsage() {
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
     *
     * @return  XML string
     */
    public String getXmlDescription() {
        return xmlDescription_;
    }

    /**
     * Sets a usage string which should give some clue how to specify this
     * key from a string.
     *
     * <p>Examples might be something like "<code>true|false</code>"
     * or "<code>&lt;RRGGBB&gt;</code>".
     *
     * @param  usage  usage string
     * @return  this object, as a convenience
     */
    public ConfigMeta setStringUsage( String usage ) {
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
    public ConfigMeta setShortDescription( String shortDescription ) {
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
    public ConfigMeta setXmlDescription( String xmlDescription ) {
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
    public ConfigMeta setXmlDescription( String[] lines ) {
        setXmlDescription( PlotUtil.concatLines( lines ) );
        return this;
    }

    /**
     * Convenience method to add additional lines to the existing XML
     * documentation string.
     *
     * @param   moreXml  additinoal lines of documentation string
     * @return  this object, as a convenience
     */
    public ConfigMeta appendXmlDescription( String[] moreXml ) {
        String descrip = new StringBuffer()
            .append( getXmlDescription() )
            .append( PlotUtil.concatLines( moreXml ) )
            .toString();
        setXmlDescription( descrip );
        return this;
    }

    /**
     * Uppercases the first letter of a string.
     *
     * @param  word  word
     * @return   word with first letter capitalised if possible
     */
    public static String capitalise( String word ) {
        if ( word == null ) {
            return null;
        }
        int leng = word.length();
        StringBuffer sbuf = new StringBuffer( leng );
        if ( leng > 0 ) {
            sbuf.append( Character.toUpperCase( word.charAt( 0 ) ) );
        }
        if ( leng > 1 ) {
            sbuf.append( word.substring( 1 ) );
        }
        return sbuf.toString();
    }
}
