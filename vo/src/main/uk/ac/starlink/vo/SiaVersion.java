package uk.ac.starlink.vo;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.ContentCoding;

/**
 * Version of the Simple Image Access protocol.
 *
 * @since   12 Mar 2020
 * @author  Mark Taylor
 * @see   <a href="http://www.ivoa.net/Documents/SIA/"
 *           >Simple Image Access Protocol</a>
 */
public enum SiaVersion {

    /** SIA version 1.0. */
    V10( "1.0", 1, new Ivoid( "ivo://ivoa.net/std/SIA" ),
         "http://www.ivoa.net/Documents/SIA/20091116/", true ) {
        public StarTable executeQuery( String serviceUrl,
                                       double ra, double dec, double size,
                                       SiaFormatOption format,
                                       StarTableFactory factory,
                                       ContentCoding coding )
                throws IOException {
            DalQuery query =
                new DalQuery( serviceUrl, "SIA", ra, dec, size, coding );
            if ( format != null ) {
                query.addArgument( "FORMAT", format.getSiav1Value() );
            }
            return query.execute( factory );
        }
    },

    /** SIA version 2.0. */
    V20( "2.0", 2, new Ivoid( "ivo://ivoa.net/std/SIA#query-2.0" ),
         "http://www.ivoa.net/Documents/SIA/20151223/", false ) {
        public StarTable executeQuery( String serviceUrl,
                                       double ra, double dec, double size,
                                       SiaFormatOption format,
                                       StarTableFactory factory,
                                       ContentCoding coding )
                throws IOException {
            CgiQuery query = new CgiQuery( serviceUrl );
            String posTxt = new StringBuffer()
                .append( "CIRCLE " )
                .append( CgiQuery.formatDouble( ra ) )
                .append( " " )
                .append( CgiQuery.formatDouble( dec ) )
                .append( " " )
                .append( size >= 0 ? CgiQuery.formatDouble( 0.5 * size ) : "0" )
                .toString();
            query.addArgument( "POS", posTxt );
            if ( format != null ) {
                for ( String fmt : format.getSiav2Values() ) {
                    query.addArgument( "FORMAT", fmt );
                }
            }
            return DalQuery.executeQuery( query.toURL(), factory, coding );
        }
    };

    private final String number_;
    private final int majorVersion_;
    private final Ivoid standardId_;
    private final String docUrl_;
    private final boolean usesUcd1_;

    /**
     * Constructor.
     *
     * @param  version number as string
     * @param  majorVersion  major version number
     * @param  standardId  StandardsRegExt identifier
     * @param  docUrl  standards document URL
     * @param  usesUcd1  true if query responses use UCD1;
     *                   false for UCD1+
     */
    SiaVersion( String number, int majorVersion, Ivoid standardId,
                String docUrl, boolean usesUcd1 ) {
        number_ = number;
        majorVersion_ = majorVersion;
        standardId_ = standardId;
        docUrl_ = docUrl;
        usesUcd1_ = usesUcd1;
    }

    /**
     * Returns the major version number.
     *
     * @return  major version number
     */
    public int getMajorVersion() {
        return majorVersion_;
    }

    /**
     * Returns the URL of the standards document defining this version.
     * 
     * @return   document URL
     */
    public String getDocumentUrl() {
        return docUrl_;
    }

    /**
     * Returns the StandardsRegExt identifier associated with this
     * version of the SIA standard.
     *
     * @return  standardId
     */
    public Ivoid getStandardId() {
        return standardId_;
    }

    /**
     * Returns true if query responses use the UCD1 standard,
     * false if they use the UCD1+ standard.
     *
     * @return  true for UCD1, false for UCD1+
     */
    public boolean usesUcd1() {
        return usesUcd1_;
    }

    /**
     * Performs an SIA positional query.
     * The exact geometry of the query is not defined here, but the
     * <code>size</code> parameter defines an angular extent such as
     * the width of a rectangular region or a diameter (not radius)
     * of a cone.
     *
     * @param  serviceUrl  base URL for SIA service query resource
     * @param  ra   central right ascension position in degrees
     * @param  dec  central declination position in degrees
     * @param  size  extent of query in degrees
     * @param  format   required format for query result records returned
     * @param  factory  table factory
     * @param  coding  content coding for communications
     * @return   table giving query result
     */
    public abstract StarTable
            executeQuery( String serviceUrl,
                          double ra, double dec, double size,
                          SiaFormatOption format,
                          StarTableFactory factory, ContentCoding coding )
            throws IOException;

    @Override
    public String toString() {
        return number_;
    }

    /**
     * Returns the version object appropriate for use with a given
     * capability interface.
     *
     * @param  intf  interface
     * @return   SIA version used by interface;
     *           not null, a best guess is used if necessary
     */
    public static SiaVersion forInterface( RegCapabilityInterface intf ) {
        Ivoid stdId = new Ivoid( intf.getStandardId() );
        for ( SiaVersion vers : values() ) {
            if ( vers.standardId_.equalsIvoid( stdId ) ) {
                return vers;
            }
        }
        String versTxt = intf.getVersion();
        for ( SiaVersion vers : values() ) {
            if ( vers.number_.equals( versTxt ) ) {
                return vers;
            }
        }
        return V10;
    }
}
