package uk.ac.starlink.vo;

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
     * @param  lang  ADQL language variant (e.g. "ADQL-2.0")
     * @param  tcap  TAP capability object
     * @param  tables  table metadata set
     * @param  table  currently selected table
     */
    String getText( boolean lineBreaks, String lang, TapCapability tcap,
                    TableMeta[] tables, TableMeta table );

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
}
