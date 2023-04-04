package uk.ac.starlink.vo;

import java.net.URL;

/**
 * Represents a type of example ADQL query.
 * The query text can be generated as a function of given service metadata.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2011
 */
public interface AdqlExample {

    /**
     * Produces ADQL text for a query of the type represented by this object,
     * for a given set of service details.
     *
     * @param  lineBreaks  whether output ADQL should include multiline
     *                     formatting
     * @param  lang  ADQL language variant
     * @param  tcap  TAP capability object
     * @param  tables  table metadata set
     * @param  table  currently selected table
     * @param  skypos  2-element array giving preferred (RA,Dec) sky position
     *                 in degrees, or null if none preferred
     * @return   example text, or null if no example can be constructed
     *           given the input values
     */
    String getAdqlText( boolean lineBreaks, VersionedLanguage lang,
                        TapCapability tcap, TableMeta[] tables,
                        TableMeta table, double[] skypos );

    /**
     * Returns this example's name.
     *
     * @return   name
     */
    String getName();

    /**
     * Returns this example's description.
     *
     * @return   short description
     */
    String getDescription();

    /**
     * Returns a documentation URL associated with this example if available.
     *
     * @return  documentation URL (suitable for browser display), or null
     */
    URL getInfoUrl();
}
