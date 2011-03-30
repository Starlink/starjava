package uk.ac.starlink.vo;

/**
 * Represents a type of example ADQL query.
 * The query text can be generated as a function of given service metadata.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2011
 */
abstract class AdqlExample {

    private final String name_;
    private final String description_;

    /**
     * Constructor.
     *
     * @param   name  example name
     * @param   descripton   example short description
     */
    protected AdqlExample( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Produces ADQL text for a query of the type represented by this object,
     * for a given set of service details.
     *
     * @param  lang  ADQL language variant (e.g. "ADQL-2.0")
     * @param  tcap  TAP capability object
     * @param  tables  table metadata set 
     * @param  table  currently selected table
     */
    public abstract String getText( String lang, TapCapability tcap,
                                    TableMeta[] tables, TableMeta table );

    /**
     * Returns this example's name.
     *
     * @return   name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns this example's description.
     *
     * @return   short description
     */
    public String getDescription() {
        return description_;
    }
}
